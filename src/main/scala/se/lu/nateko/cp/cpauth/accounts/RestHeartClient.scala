package se.lu.nateko.cp.cpauth.accounts

import akka.Done
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.RestHeartConfig
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.utils.SprayJsonUtils._
import se.lu.nateko.cp.cpauth.utils.Utils
import spray.json._

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

class RestHeartClient(val config: RestHeartConfig, http: HttpExt)(using Materializer) {
	import http.system.dispatcher
	def log = http.system.log

	def init: Future[Done] = if config.skipInit then Future.successful(Done) else Future.sequence(
		config.dbNames.keys.map{implicit envri =>
			createUsersCollIfNotExists.zip(createPortalUsageCollIfNotExists)
		}
	).map(_ => Done)

	def dbUri(using Envri): Uri = {
		import config._
		Uri(s"$baseUri/$dbName")
	}

	def usersCollUri(using Envri): Uri = {
		val db = dbUri
		db.withPath(db.path / config.usersCollection)
	}

	def portalUsageCollUri(using Envri): Uri = {
		val db = dbUri
		db.withPath(db.path / config.usageCollection)
	}
	private val KeepIdsOnly = "keys" -> "{\"_id\": 1}"
	private def pageSizeQpar(size: Int) = "pagesize" -> size.toString

	def getUserUri(uid: UserId)(using Envri): Uri = usersCollUri.withPath(usersCollUri.path / uid.email)

	def createUserIfNew(uid: UserId, givenName: String, surname: String)(using Envri): Future[Done] =
		userExists(uid).flatMap{
			exists => if(exists) ok else createNewUser(uid, givenName, surname)
		}.andThen{
			case Failure(exc) => log.error(exc, s"Problem creating user ${uid.email}")
		}

	private def createNewUser(uid: UserId, givenName: String, surname: String)(using Envri): Future[Done] =
		val payload = JsObject(
			"_id" -> JsString(uid.email),
			"profile" -> JsObject(
				"givenName" -> JsString(givenName),
				"surname" -> JsString(surname)
			),
			"cart" -> JsObject(
				"_items" -> JsArray.empty
			)
		)
		val createStatus = for(
			entity <- Marshal(payload).to[RequestEntity];
			req = HttpRequest(
				method = HttpMethods.POST,
				uri = usersCollUri,
				entity = entity,
				headers = Seq(headers.`Content-Type`(ContentTypes.`application/json`))
			);
			status <- requestDiscardResp(req)
		) yield status

		createStatus.flatMap{status =>
			if(status.isSuccess()) ok
			else fail(
				s"Could not create user ${uid.email}, got response ${status.defaultMessage()}"
			)
		}

	def userExists(uid: UserId)(using Envri): Future[Boolean] = {
		val query = Uri.Query(KeepIdsOnly)
		val req = HttpRequest(uri = getUserUri(uid).withQuery(query))

		requestDiscardResp(req).flatMap{status =>
			if(status.isSuccess()) Future.successful(true)
			else if(status == StatusCodes.NotFound) Future.successful(false)
			else fail(
				s"Failed checking user ${uid.email}, got response " + status.defaultMessage()
			)
		}
	}

	def getUserProps(uid: UserId, projections: Seq[String] = Nil)(using Envri): Future[JsObject] = {
		val query = if(projections.isEmpty)
			Uri.Query.Empty
		else {
			val keys = projections.map(proj => "\"" + proj + "\": 1").mkString("{", ", ", "}")
			Uri.Query("keys" -> keys)
		}
		val uri = getUserUri(uid).withQuery(query)
		for(
			resp <- http.singleRequest(HttpRequest(uri = uri));
			userVal <- Unmarshal(resp.entity.withContentType(ContentTypes.`application/json`)).to[JsValue];
			userObj <- Future.fromTry(ensure[JsObject](userVal))
		) yield userObj
	}

	def getGivenAndSurName(uid: UserId)(using Envri): Future[(String, String)] =
		getUserProps(uid, Seq("profile.givenName", "profile.surname")).flatMap{userObj =>

			val profileVal = getFieldOpt[JsValue](userObj, "profile").getOrElse(JsObject.empty);

			Future.fromTry(
				for(
					profile <- ensure[JsObject](profileVal);
					givenName <- getStringField(profile, "givenName");
					surname <- getStringField(profile, "surname")
				) yield (givenName, surname)
			)
		}

	def findUsers(filter: Map[String, String])(using Envri): Future[Seq[UserId]] = {

		val filterParam: String = filter.map{
			case (path, value) => s""""$path": "$value""""
		}.mkString("{", ", ", "}")

		val qParams: Map[String, String] = if(filter.nonEmpty) Map("filter" -> filterParam) else Map.empty
		val uri = usersCollUri.withQuery(Uri.Query(qParams + KeepIdsOnly + pageSizeQpar(1000)))
		for(
			resp <- http.singleRequest(HttpRequest(uri = uri));
			usersListResp <- Unmarshal(resp.entity.withContentType(ContentTypes.`application/json`)).to[JsValue];
			users <- Future.fromTry(parseFilteredUsersList(usersListResp))
		) yield users
	}

	private def parseFilteredUsersList(v: JsValue): Try[Seq[UserId]] = {
		for(
			returned <- ensure[JsArray](v);
			uidObjs <- getElements[JsObject](returned)
		) yield
			if(uidObjs.size == 0) Success(Nil)
			else for(
				ids <- Utils.tryseq(uidObjs.map(getStringField(_, "_id")))
			) yield ids.map(UserId.apply)
	}.flatten

	private def createDbIfNotExists(using Envri): Future[Done] =
		createIfNotExists("Mongo db", dbUri, dbUri)

	private def createCollIfNotExists(collUri: Uri, thing: String): Future[Done] = {
		val checkUri = collUri.withPath(collUri.path / "_indexes")
		createIfNotExists(thing, checkUri, collUri)
	}

	private def createUsersCollIfNotExists(using Envri): Future[Done] =
		createDbIfNotExists.flatMap{_ =>
			createCollIfNotExists(usersCollUri, "users collection")
		}

	private def createPortalUsageCollIfNotExists(using Envri): Future[Done] =
		createDbIfNotExists.flatMap{_ =>
			createCollIfNotExists(portalUsageCollUri, "portal usage collection")
		}

	private def createIfNotExists(thing: String, checkUri: Uri, putUri: Uri): Future[Done] =
		requestDiscardResp(HttpRequest(uri = checkUri)).flatMap{
			case StatusCodes.OK => ok

			case StatusCodes.NotFound =>
				requestDiscardResp(HttpRequest(HttpMethods.PUT, uri = putUri)).flatMap{
					case StatusCodes.Created => ok
					case status => fail(
						s"Could not create $thing at $putUri, got response $status"
					)
				}
			case status => fail(
				s"Got Unexpected status $status when trying to check if $thing exists at $checkUri"
			)
		}

	private def requestDiscardResp(req: HttpRequest): Future[StatusCode] =
		http.singleRequest(req).map{resp =>
			resp.discardEntityBytes()
			resp.status
		}

	private val ok = Future.successful(Done)

	private def fail[T](msg: String) = Future.failed[T](new Exception(msg))
}
