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

class RestHeartClient(val config: RestHeartConfig, http: HttpExt)(implicit m: Materializer) {

	import http.system.dispatcher

	def getUserUri(uid: UserId): Uri = {
		val email = uid.email.toLowerCase()
		import config._
		Uri(s"$baseUri/$dbName/$usersCollection/$email")
	}

	def createNewUser(uid: UserId, payload: JsObject): Future[Done] = {
		val uri = getUserUri(uid)
		for(
			entity <- Marshal(payload).to[RequestEntity];
			req = HttpRequest(method = HttpMethods.PUT, uri = uri, entity = entity);
			resp <- http.singleRequest(req);
			done <- validateResponse(resp, "Failed to write for " + uid.email)
		) yield Done
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
			userObj <- Unmarshal(resp.entity).to[JsValue]
		) yield userObj.asJsObject("Expected a JSON object, got a JSON value")
	}

	def getGivenAndSurName(uid: UserId): Future[(String, String)] =
		getUserProps(uid, Seq("givenName", "surname")).flatMap{userObj =>

			def getField(field: String): Try[String] = {
				userObj.fields.get(field) match {
					case Some(JsString(v)) => Success(v)
					case _ => Failure(new Exception(s"Expected a string property '$field'"))
				}
			}
			Future.fromTry(for(
				givenName <- getField("givenName");
				surname <- getField("surame")
			) yield (givenName, surname))
		}

	private def validateResponse(resp: HttpResponse, msg: String): Future[Done] = {
		if(resp.status.isSuccess()) {
			Future.successful(Done)
		} else Future.failed(
			new Exception(s"$msg, got response ${resp.status.defaultMessage()}")
		)
	}

	def importOldUsers(users: Seq[(UserId, String, String)]): Future[Done] = {

		val seed: Future[Done] = Future.successful(Done)
		users.foldLeft(seed)(
			(fut, user) => fut.flatMap{_ =>
				val (uid, givenName, surname) = user
	
				val payload = JsObject(
					"givenName" -> JsString(givenName),
					"surname" -> JsString(surname)
				)
				createNewUser(uid, payload)
			}
		)
	}
}
