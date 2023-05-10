package se.lu.nateko.cp.cpauth.accounts

import akka.Done
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.core.SprayJsonUtils.*
import spray.json.*

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import eu.icoscp.georestheart.RestHeartConfig
import eu.icoscp.utils.akkauri.uriJavaToAkka
import eu.icoscp.georestheart.RestHeartClientBase
import se.lu.nateko.cp.cpauth.core.CoreUtils
import eu.icoscp.geoipclient.CpGeoClient

class RestHeartClient(
	val config: RestHeartConfig, geoip: CpGeoClient, http: HttpExt
)(using Materializer) extends RestHeartClientBase(config, geoip, http):

	import http.system.dispatcher
	def log = http.system.log

	def init: Future[Done] = createDbsAndColls

	private val KeepIdsOnly = "keys" -> "{\"_id\": 1}"
	private def pageSizeQpar(size: Int) = "pagesize" -> size.toString

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
				uri = config.usersCollUri,
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
		val uri = config.usersCollUri.withQuery(Uri.Query(qParams + KeepIdsOnly + pageSizeQpar(1000)))
		for(
			resp <- singleRequest(HttpRequest(uri = uri));
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
				ids <- CoreUtils.tryseq(uidObjs.map(getStringField(_, "_id")))
			) yield ids.map(UserId.apply)
	}.flatten



end RestHeartClient
