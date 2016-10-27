package se.lu.nateko.cp.cpauth.accounts

import scala.concurrent.Future

import akka.Done
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import se.lu.nateko.cp.cpauth.RestHeartConfig
import se.lu.nateko.cp.cpauth.core.UserId
import spray.json.{JsObject, JsValue, JsString}
import scala.concurrent.duration.DurationInt
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.StatusCodes

class RestHeartClient(val config: RestHeartConfig, http: HttpExt)(implicit m: Materializer) {

	import http.system.dispatcher

	def getUserUri(uid: UserId): Uri = {
		val email = uid.email
		import config._
		Uri(s"$baseUri/$dbName/$usersCollection/$email")
	}

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
		val query = Uri.Query("keys" -> "{\"_id\": 1}")
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
			userObj <- Unmarshal(resp.entity.withContentType(ContentTypes.`application/json`)).to[JsValue]
		) yield userObj.asJsObject("Expected a JSON object, got a JSON value")
	}

	def getGivenAndSurName(uid: UserId): Future[(String, String)] =
		getUserProps(uid, Seq("profile.givenName", "profile.surname")).flatMap{userObj =>

			def getField(obj: JsObject, field: String): Try[String] = {
				obj.fields.get(field) match {
					case Some(JsString(v)) => Success(v)
					case None => Success("")
					case _ => Failure(new Exception(s"Expected a string property '$field'"))
				}
			}
			Future.fromTry(for(
				profile <- Try{
					val profile = userObj.fields.get("profile").getOrElse(
						throw new Exception("User profile not found for " + uid.email)
					)
					profile.asJsObject("Expected 'profile' to be a JSON object")
				};
				givenName <- getField(profile, "givenName");
				surname <- getField(profile, "surname")
			) yield (givenName, surname))
		}

}
