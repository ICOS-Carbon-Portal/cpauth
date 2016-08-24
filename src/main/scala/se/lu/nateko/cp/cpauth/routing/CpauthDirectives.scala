package se.lu.nateko.cp.cpauth.routing

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import se.lu.nateko.cp.cpauth.core.Authenticator
import se.lu.nateko.cp.cpauth.core.CookieToToken
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import akka.actor.Scheduler
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.Directive0
import akka.stream.ActorMaterializer
import se.lu.nateko.cp.cpauth.HttpConfig
import se.lu.nateko.cp.cpauth.Utils
import se.lu.nateko.cp.cpauth.accounts.UsersIo
import akka.http.scaladsl.server.Directive
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.StandardRoute
import se.lu.nateko.cp.cpauth.accounts.RestHeartClient

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

	val user: Directive1[UserId] = Directive{inner =>
		cookie(publicAuthConfig.authCookieName)(cookie => {
			val userTry = for(
				auth <- authenticator;
				token <- CookieToToken.recoverToken(cookie.value);
				uid <- auth.unwrapUserId(token)
			) yield uid

			val userFuture = Utils.slowFailureDown(Future.fromTry(userTry), 500 millis)

			onComplete(userFuture){
				case Success(uid) => inner(Tuple1(uid))
				case Failure(err) => reject(
					new AuthenticationFailedRejection(
						AuthenticationFailedRejection.CredentialsRejected,
						HttpChallenge("https", "")
					)
				)
			}
		})
	}

	def cpauthCookie: Route = cookie(publicAuthConfig.authCookieName)(cookie => {
		complete(publicAuthConfig.authCookieName + "=" + cookie.value)
	})

	lazy val logout: Route = deleteCookie(publicAuthConfig.authCookieName, httpConfig.authDomain, "/"){
		complete(StatusCodes.OK)
	}

	val admin: Directive0 = user.tflatMap(uit => ifUserIsAdmin(uit._1)) |
		complete((StatusCodes.Forbidden, "Need to be logged in as CPauth admin"))

	def ifUserIsAdmin(uid: UserId): Directive0 = Directive{ inner =>
		onComplete(userDb.userIsAdmin(uid)){
			case Failure(err) => failWith(err)
			case Success(false) => reject(AuthorizationFailedRejection)
			case Success(true) => inner(())
		}
	}
}
