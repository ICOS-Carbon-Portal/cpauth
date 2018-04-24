package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.model.{ HttpMethods, StatusCodes }
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import se.lu.nateko.cp.cpauth.Envri.Envri
import se.lu.nateko.cp.cpauth.core.UserId

trait RestHeartRouting extends RestHeartDirectives{

	val restheartRoute: Route = extractEnvri{implicit envri =>

		val config = restHeart.config

		def injectUsersCollection(req: HttpRequest) = req.copy(
			uri = {
				val oldPathPart = req.uri.path.tail.tail.tail.tail
				val newPath = Path./(config.dbName) / config.usersCollection ++ oldPathPart
				req.uri.withPath(newPath)
			}
		)

		path("db" / "users" / Segment){ email =>
			options{
				addAccessControlHeaders(envri){
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
					addAccessControlHeaders(envri){
						mapRequest(injectUsersCollection){
							restheartProxy
						}
					}
				} ~
				forbid("Access to other users' documents is forbidden")
			} ~
			forbid("Must be logged in with Carbon Portal for this operation")
		}
	}

	private def validateUser(email: String, uid: UserId): Directive0 =
		validate(email == uid.email, "Only admins can write to other users' documents")

	override def addAccessControlHeaders(implicit envri: Envri): Directive0 = headerValueByType[Origin](()).flatMap{origin =>
		if (origin.value.endsWith(authConfig.pub(envri).authCookieDomain)) {
			respondWithHeaders(
				`Access-Control-Allow-Origin`(origin.value)
			)
		} else {
			pass
		}
	}.recover(_ => pass)
}
