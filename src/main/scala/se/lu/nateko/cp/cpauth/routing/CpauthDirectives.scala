package se.lu.nateko.cp.cpauth.routing

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import akka.actor.Scheduler
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directive
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.StandardRoute
import akka.stream.ActorMaterializer
import se.lu.nateko.cp.cpauth.HttpConfig
import se.lu.nateko.cp.cpauth.utils.Utils
import se.lu.nateko.cp.cpauth.accounts.RestHeartClient
import se.lu.nateko.cp.cpauth.accounts.UsersIo
import se.lu.nateko.cp.cpauth.core.Authenticator
import se.lu.nateko.cp.cpauth.core.CookieToToken
import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.core.AuthToken
import se.lu.nateko.cp.cpauth.core.AuthSource

trait CpauthDirectives {

	def publicAuthConfig: PublicAuthConfig
	def httpConfig: HttpConfig
	def authenticator: Try[Authenticator]
	def userDb: UsersIo
	def restHeart: RestHeartClient

	implicit def dispatcher: ExecutionContext
	implicit def materializer: ActorMaterializer
	implicit def scheduler: Scheduler

	def attempt[T](thunk: => T)(f: T => Route): Route = attempt(Try(thunk))(f)

	def attempt[T](attempt: Try[T])(f: T => Route): Route = attempt match {
		case Success(t) => f(t)
		case Failure(err) => complete((StatusCodes.BadRequest, err.getMessage))
	}

	def forbid(message: String): StandardRoute = complete((StatusCodes.Forbidden, message))

	val token: Directive1[AuthToken] = Directive{inner =>
		cookie(publicAuthConfig.authCookieName)(cookie => {
			val tokenTry = for(
				auth <- authenticator;
				signedToken <- CookieToToken.recoverToken(cookie.value);
				token <- auth.unwrapToken(signedToken)
			) yield token

			val tokenFuture = Utils.slowFailureDown(Future.fromTry(tokenTry), 500 millis)

			onComplete(tokenFuture){
				case Success(token) => inner(Tuple1(token))
				case Failure(err) => reject(
					new AuthenticationFailedRejection(
						AuthenticationFailedRejection.CredentialsRejected,
						HttpChallenge("https", "")
					)
				)
			}
		})
	}

	val user: Directive1[UserId] = token.map(_.userId)

	def cpauthCookie: Route = cookie(publicAuthConfig.authCookieName)(cookie => {
		import spray.json._
		attempt(CookieToToken.recoverToken(cookie.value)){token =>
			complete(JsObject(
				"value" -> JsString(publicAuthConfig.authCookieName + "=" + cookie.value),
				"expiry" -> JsNumber(token.token.expiresOn),
				"source" -> JsString(token.token.source.toString)
			))
		}
	})

	lazy val logout: Route = deleteCookie(publicAuthConfig.authCookieName, httpConfig.authDomain, "/"){
		complete(StatusCodes.OK)
	}

	val admin: Directive0 = token.tflatMap(uit => ifUserIsAdmin(uit._1)) |
		complete((StatusCodes.Forbidden, "Need to be logged in as CPauth admin"))

	def ifUserIsAdmin(token: AuthToken): Directive0 = Directive{ inner =>
		onComplete(userDb.userIsAdmin(token.userId).map(_ && token.source == AuthSource.Password)){
			case Failure(err) => failWith(err)
			case Success(false) => reject(AuthorizationFailedRejection)
			case Success(true) => inner(())
		}
	}
}
