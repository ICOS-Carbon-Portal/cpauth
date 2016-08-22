package se.lu.nateko.cp.cpauth.routing

import se.lu.nateko.cp.cpauth.RestHeartConfig
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.Uri
import se.lu.nateko.cp.cpauth.core.UserInfo

trait RestHeartRouting extends CpauthDirectives with ProxyDirectives{

	def restheartConfig: RestHeartConfig

	def restheartRoute: Route = {
		val config = restheartConfig
		val restheartBaseUri = Uri(config.baseUri)

		path(config.dbName / config.usersCollection / Segment){ uid =>
			user{uinfo =>
				(validateUser(uid, uinfo) | ifUserIsAdmin(uinfo)){
					restheartProxy(restheartBaseUri)
				} ~
				forbid("Access to other users' documents is forbidden")
			} ~
			forbid("Must be logged in with Carbon Portal for this operation")
		}
	}

	private def validateUser(uid: String, uinfo: UserInfo): Directive0 =
		validate(uid == uinfo.mail, "Only admins can write to other users' documents")

}
