package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.model.{ HttpMethods, StatusCodes }
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import se.lu.nateko.cp.cpauth.CpauthJsonProtocol.given
import se.lu.nateko.cp.cpauth.Envri.Envri
import se.lu.nateko.cp.cpauth.core.UserId
import spray.json.DefaultJsonProtocol.{immSeqFormat, tuple2Format, StringJsonFormat}

trait RestHeartRouting extends RestHeartDirectives{

	val restheartRoute: Route = extractEnvri{implicit envri =>

		val config = restHeart.config

		def injectUsersCollection(req: HttpRequest) = req.withUri{
			val oldPathPart = req.uri.path.tail.tail.tail.tail
			val newPath = Path./(config.dbName) / config.usersCollection ++ oldPathPart
			req.uri.withPath(newPath)
		}

		path("db" / "users" / Segment){ email =>
			options{
				addAccessControlAllowOrigin(envri){
					respondWithHeaders(
						`Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.PATCH),
						`Access-Control-Allow-Credentials`(true),
						`Access-Control-Allow-Headers`("Content-Type")
					){
						complete(StatusCodes.OK)
					}
				}
			} ~
			token { token =>
				(validateUser(email, token.userId) | ifUserIsAdmin(token)) {
					addAccessControlAllowOrigin(envri){
						mapRequest(injectUsersCollection){
							restheartProxy
						}
					}
				} ~
				forbid("Access to other users' documents is forbidden")
			} ~
			forbid("Must be logged in with Carbon Portal for this operation")
		} ~
		path("anonidlookup"){
			(admin & onSuccess(restHeart.findUsers(Map.empty))){users =>
				val table = users.map{uid =>
					anonymizeCpUser(uid) -> uid.email
				}.sortBy(_._1)
				complete(table)
			}
		}

	}

	private def validateUser(email: String, uid: UserId): Directive0 =
		validate(email == uid.email, "Only admins can write to other users' documents")

	def addAccessControlAllowOrigin(implicit envri: Envri): Directive0 = headerValueByType(Origin).flatMap{origin =>
		if (origin.value.endsWith(authConfig.pub(envri).authCookieDomain)) {
			respondWithHeaders(
				`Access-Control-Allow-Origin`(origin.origins.head)
			)
		} else {
			pass
		}
	}.recover(_ => pass)
}
