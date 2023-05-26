package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.CpauthJsonProtocol.given
import se.lu.nateko.cp.cpauth.core.UserId
import eu.icoscp.utils.akkauri.uriJavaToAkka
import spray.json.DefaultJsonProtocol.StringJsonFormat
import spray.json.DefaultJsonProtocol.immSeqFormat
import spray.json.DefaultJsonProtocol.tuple2Format

trait RestHeartRouting extends RestHeartDirectives{

	def restHeartCreds: Map[Envri, BasicHttpCredentials]

	val restheartRoute: Route = extractEnvri{implicit envri =>

		val config = restHeart.config
		val usersCollUri: Uri = config.usersCollUri

		def injectUsersCollection(req: HttpRequest) = req.withUri{
			val oldPathPart = req.uri.path.tail.tail.tail.tail
			req.uri.withPath(usersCollUri.path ++ oldPathPart)
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
							restheartProxy(usersCollUri, restHeartCreds.get(envri))
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
