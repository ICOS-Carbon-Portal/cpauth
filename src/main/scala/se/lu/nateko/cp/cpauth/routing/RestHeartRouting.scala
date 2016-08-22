package se.lu.nateko.cp.cpauth.routing

import scala.concurrent.Future

import akka.Done
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import se.lu.nateko.cp.cpauth.RestHeartConfig
import se.lu.nateko.cp.cpauth.core.UserId
import spray.json.JsObject
import spray.json.JsString

trait RestHeartRouting extends CpauthDirectives with ProxyDirectives{

	def restheartConfig: RestHeartConfig

	def restheartRoute: Route = {
		val config = restheartConfig
		val restheartBaseUri = Uri(config.baseUri)

		path(config.dbName / config.usersCollection / "importusers"){
			(post & admin){
				onSuccess(importUsersToRestheart()){_ =>
					complete(StatusCodes.OK)
				}
			}
		} ~
		path(config.dbName / config.usersCollection / Segment){ email =>
			user{uid =>
				(validateUser(email, uid) | ifUserIsAdmin(uid)){
					restheartProxy(restheartBaseUri)
				} ~
				forbid("Access to other users' documents is forbidden")
			} ~
			forbid("Must be logged in with Carbon Portal for this operation")
		}
	}

	private def validateUser(email: String, uid: UserId): Directive0 =
		validate(email == uid.email, "Only admins can write to other users' documents")

	private def importUsersToRestheart(): Future[Done] = {
		val config = restheartConfig

		def doForUser(user: (UserId, String, String)): Future[Done] = {

			val (uid, givenName, surname) = user
			val email = uid.email.toLowerCase()
			import config._
			val uri = Uri(s"$baseUri/$dbName/$usersCollection/$email")

			val payload = JsObject(
				"givenName" -> JsString(givenName),
				"surname" -> JsString(surname)
			)

			for(
				entity <- Marshal(payload).to[RequestEntity];
				req = HttpRequest(method = HttpMethods.PUT, uri = uri, entity = entity);
				resp <- http.singleRequest(req);
				done <- validateResponse(resp, email)
			) yield Done
		}

		userDb.listUsersOld.flatMap{ users =>
			val seed: Future[Done] = Future.successful(Done)
			users.foldLeft(seed)(
				(fut, user) => fut.flatMap(_ => doForUser(user))
			)
		}
	}

	private def validateResponse(resp: HttpResponse, email: String): Future[Done] = {
		if(resp.status.isSuccess()) {
			Future.successful(Done)
		} else Future.failed(
			new Exception(s"Failed to write for $email, got response ${resp.status.defaultMessage()}")
		)
	}
}
