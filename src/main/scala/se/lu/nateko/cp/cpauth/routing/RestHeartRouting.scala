package se.lu.nateko.cp.cpauth.routing

import scala.concurrent.Future

import akka.Done
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import se.lu.nateko.cp.cpauth.core.UserId

trait RestHeartRouting extends RestHeartDirectives{

	def restheartRoute: Route = {
		val config = restHeart.config

		path(config.dbName / config.usersCollection / "importusers"){
			(post & admin){
				onSuccess(importUsersToRestheart()){_ =>
					complete(StatusCodes.OK)
				}
			}
		} ~
		path(config.dbName / config.usersCollection / Segment){ email =>
			token{token =>
				(validateUser(email, token.userId) | ifUserIsAdmin(token)){
					restheartProxy
				} ~
				forbid("Access to other users' documents is forbidden")
			} ~
			forbid("Must be logged in with Carbon Portal for this operation")
		}
	}

	private def validateUser(email: String, uid: UserId): Directive0 =
		validate(email == uid.email, "Only admins can write to other users' documents")

	private def importUsersToRestheart(): Future[Done] = {
		userDb.listUsersOld.flatMap(restHeart.importOldUsers)
	}
}
