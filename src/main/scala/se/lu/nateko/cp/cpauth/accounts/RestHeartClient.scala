package se.lu.nateko.cp.cpauth.accounts

import scala.concurrent.Future
import scala.util.Success
import scala.util.Try

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
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import se.lu.nateko.cp.cpauth.RestHeartConfig
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.utils.SprayJsonUtils._
import se.lu.nateko.cp.cpauth.utils.Utils
import spray.json._

class RestHeartClient(val config: RestHeartConfig, http: HttpExt)(implicit m: Materializer) {
	import http.system.dispatcher

	val usersCollUri: Uri = {
		import config._
		Uri(s"$baseUri/$dbName/$usersCollection")
	}

	private val KeepIdsOnly = "keys" -> "{\"_id\": 1}"

	def getUserUri(uid: UserId): Uri = usersCollUri.withPath(usersCollUri.path / uid.email)

	def createUserIfNew(uid: UserId, givenName: String, surname: String): Future[Done] =
		userExists(uid).flatMap{exists =>
			if(exists) Future.successful(Done)
			else {
				val payload = JsObject("profile" -> JsObject(
					"givenName" -> JsString(givenName),
					"surname" -> JsString(surname)
				))
				createUser(uid, payload)
			}
		}

	private def createUser(uid: UserId, payload: JsObject): Future[Done] =
		writeUser(uid, payload, HttpMethods.PUT).flatMap{resp =>
			if(resp.status.isSuccess()) {
				Future.successful(Done)
			} else Future.failed(
				new Exception(s"Could not create user ${uid.email}, got response ${resp.status.defaultMessage()}")
			)
		}

	private def writeUser(uid: UserId, payload: JsObject, verb: HttpMethod): Future[HttpResponse] = {
		val uri = getUserUri(uid)
		for(
			entity <- Marshal(payload).to[RequestEntity];
			req = HttpRequest(method = verb, uri = uri, entity = entity);
			resp <- http.singleRequest(req)
		) yield resp
	}

	def userExists(uid: UserId): Future[Boolean] = {
		val query = Uri.Query(KeepIdsOnly)
		val req = HttpRequest(uri = getUserUri(uid).withQuery(query))

		http.singleRequest(req).flatMap{resp =>
			if(resp.status.isSuccess()) Future.successful(true)
			else if(resp.status == StatusCodes.NotFound) Future.successful(false)
			else Future.failed(
				new Exception(s"Failed checking user ${uid.email}, got response " + resp.status.defaultMessage())
			)
		}
	}

	def getUserProps(uid: UserId, projections: Seq[String] = Nil): Future[JsObject] = {
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

	def getGivenAndSurName(uid: UserId): Future[(String, String)] =
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

	def findUsers(filter: Map[String, String]): Future[Seq[UserId]] = {

		val filterParam: String = filter.map{
			case (path, value) => s""""$path": "$value""""
		}.mkString("{", ", ", "}")

		val uri = usersCollUri.withQuery(Uri.Query("filter" -> filterParam, KeepIdsOnly))
		for(
			resp <- http.singleRequest(HttpRequest(uri = uri));
			usersListResp <- Unmarshal(resp.entity.withContentType(ContentTypes.`application/json`)).to[JsValue];
			users <- Future.fromTry(parseFilteredUsersList(usersListResp))
		) yield users
	}

	private def parseFilteredUsersList(v: JsValue): Try[Seq[UserId]] = {
		for(
			obj <- ensure[JsObject](v);
			returned <- getField[JsNumber](obj, "_returned")
		) yield
			if(returned.value == 0) Success(Nil)
			else for(
				arr <- getField[JsArray](obj, "_embedded");
				uidObjs <- getElements[JsObject](arr);
				ids <- Utils.tryseq(uidObjs.map(getStringField(_, "_id")))
			) yield ids.map(UserId)
	}.flatten

}
