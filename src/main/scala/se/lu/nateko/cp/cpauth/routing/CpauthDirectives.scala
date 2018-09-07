package se.lu.nateko.cp.cpauth.routing

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import akka.actor.Scheduler
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpMethods, StatusCodes}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directive
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.StandardRoute
import akka.stream.ActorMaterializer
import se.lu.nateko.cp.cpauth.AuthConfig
import se.lu.nateko.cp.cpauth.Envri.Envri
import se.lu.nateko.cp.cpauth.utils.Utils
import se.lu.nateko.cp.cpauth.accounts.RestHeartClient
import se.lu.nateko.cp.cpauth.accounts.UsersIo
import se.lu.nateko.cp.cpauth.core.Authenticator
import se.lu.nateko.cp.cpauth.core.CookieToToken
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.core.AuthToken
import se.lu.nateko.cp.cpauth.core.AuthSource
import akka.http.javadsl.server.CustomRejection
import akka.http.scaladsl.server.MissingCookieRejection
import akka.http.scaladsl.server.RejectionHandler

trait CpauthDirectives {

	def authConfig: AuthConfig
	def userDb: UsersIo
	def restHeart: RestHeartClient
	def hostToEnvri(host: String): Option[Envri]

	implicit def dispatcher: ExecutionContext
	implicit def materializer: ActorMaterializer
	implicit def scheduler: Scheduler

	def publicAuthConfig(implicit envri: Envri) = authConfig.pub(envri)
	def authenticator(implicit envri: Envri): Try[Authenticator] = Authenticator(publicAuthConfig)

	val extractEnvri: Directive1[Envri] = extractHost.flatMap{h =>
		hostToEnvri(h) match{
			case None => complete(StatusCodes.BadRequest -> s"Unexpected host $h, cannot find corresponding ENVRI")
			case Some(envri) => provide(envri)
		}
	}

	def attempt[T](thunk: => T)(f: T => Route): Route = attempt(Try(thunk))(f)

	def attempt[T](attempt: Try[T])(f: T => Route): Route = attempt match {
		case Success(t) => f(t)
		case Failure(err) => complete(StatusCodes.BadRequest -> err.getMessage)
	}

	def forbid(message: String): StandardRoute = complete((StatusCodes.Forbidden, message))

	val token: Directive1[AuthToken] = Directive{inner =>
		(cancelRejections(classOf[MissingCookieRejection]) & extractEnvri){implicit envri =>
			cookie(publicAuthConfig.authCookieName)(cookie => {
				val tokenTry = for(
					auth <- authenticator;
					signedToken <- CookieToToken.recoverToken(cookie.value);
					token <- auth.unwrapToken(signedToken)
				) yield token

				val tokenFuture = Utils.slowFailureDown(Future.fromTry(tokenTry), 500.millis)

				onComplete(tokenFuture){
					case Success(token) => inner(Tuple1(token))
					case Failure(err) => reject(new BadCpauthCookieRejection(err))
				}
			}) ~
			reject(CpauthCookieMissingRejection)
		}
	}

	val user: Directive1[UserId] = token.map(_.userId)

	def cpauthCookie: Route = extractEnvri{implicit envri =>
		cookie(publicAuthConfig.authCookieName)(cookie => {
			import spray.json._
			attempt(CookieToToken.recoverToken(cookie.value)){token =>
				complete(JsObject(
					"value" -> JsString(publicAuthConfig.authCookieName + "=" + cookie.value),
					"expiry" -> JsNumber(token.token.expiresOn),
					"source" -> JsString(token.token.source.toString)
				))
			}
		})
	}

	val authRejectionHandler = RejectionHandler.newBuilder()
		.handle{
			case CpauthCookieMissingRejection => complete((StatusCodes.Unauthorized, "Authentication cookie missing"))
			case bad: BadCpauthCookieRejection => complete((StatusCodes.Unauthorized, bad.message))
		}
		.result()

	lazy val whoami: Route =  extractEnvri { implicit envri =>
		addAccessControlHeaders(envri) {
			(get & handleRejections(authRejectionHandler)) {
				user { userId =>
					import se.lu.nateko.cp.cpauth.CpauthJsonProtocol.userIdFormat
					complete(userId)
				}
			} ~
			options {
				respondWithHeaders(
					`Access-Control-Allow-Methods`(HttpMethods.GET)
				) {
					complete(StatusCodes.OK)
				}
			} ~
			complete(StatusCodes.Unauthorized)
		}
	}

	lazy val logout: Route = extractEnvri{implicit envri =>
		addAccessControlHeaders(envri) {
			deleteCookie(publicAuthConfig.authCookieName, publicAuthConfig.authCookieDomain, "/") {
				complete(StatusCodes.OK)
			}
		}
	}

	val admin: Directive0 = token.tflatMap(uit => ifUserIsAdmin(uit._1)) |
		complete((StatusCodes.Forbidden, "Need to be logged in as admin, using username/password account"))

	def ifUserIsAdmin(token: AuthToken): Directive0 = Directive{ inner =>

		val isAdminFut = if(token.userId.email == authConfig.masterAdminUser)
			Future.successful(true)
		else userDb.userIsAdmin(token.userId)

		onComplete(isAdminFut.map(_ && token.source == AuthSource.Password)){
			case Failure(err) => failWith(err)
			case Success(false) => reject(AuthorizationFailedRejection)
			case Success(true) => inner(())
		}
	}

	//TODO Maybe this is not even needed, as Cpauth's whoami and logout are not used by any other services
	def addAccessControlHeaders(implicit envri: Envri): Directive0 = headerValueByType[Origin](()).flatMap{origin =>
		if (origin.value.endsWith(authConfig.pub(envri).authCookieDomain)) {
			respondWithHeaders(
				`Access-Control-Allow-Origin`(origin.value),
				`Access-Control-Allow-Credentials`(true)
			)
		} else {
			pass
		}
	}.recover(_ => pass)
}

case object CpauthCookieMissingRejection extends CustomRejection

class BadCpauthCookieRejection(err: Throwable) extends CustomRejection{
	val message = err.getMessage + err.getStackTrace.map(_.toString).mkString("\n", "\n", "")
}
