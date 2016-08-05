package se.lu.nateko.cp.cpauth

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import se.lu.nateko.cp.cpauth.core.Authenticator
import se.lu.nateko.cp.cpauth.core.CookieToToken
import se.lu.nateko.cp.cpauth.core.UserInfo
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
import akka.stream.ActorMaterializer

trait CpauthDirectives {

	def publicAuthConfig: PublicAuthConfig
	def httpConfig: HttpConfig
	def authenticator: Try[Authenticator]

	implicit def dispatcher: ExecutionContext
	implicit def materializer: ActorMaterializer
	implicit def scheduler: Scheduler

	def attempt[T](thunk: => T)(f: T => Route): Route = attempt(Try(thunk))(f)

	def attempt[T](attempt: Try[T])(f: T => Route): Route = attempt match {
		case Success(t) => f(t)
		case Failure(err) => complete((StatusCodes.BadRequest, err.getMessage))
	}

	def user(inner: UserInfo => Route): Route = cookie(publicAuthConfig.authCookieName)(cookie => {
		val userTry = for(
			auth <- authenticator;
			token <- CookieToToken.recoverToken(cookie.value);
			uinfo <- auth.unwrapUserInfo(token)
		) yield uinfo

		val userFuture = Utils.slowFailureDown(Future.fromTry(userTry), 500 millis)

		onComplete(userFuture){
			case Success(uinfo) => inner(uinfo)
			case Failure(err) => reject(
				new AuthenticationFailedRejection(
					AuthenticationFailedRejection.CredentialsRejected,
					HttpChallenge("https", "")
				)
			)
		}
	}) ~ reject(
		new AuthenticationFailedRejection(
			AuthenticationFailedRejection.CredentialsMissing,
			HttpChallenge("https", "")
		)
	)

	def cpauthCookie: Route = cookie(publicAuthConfig.authCookieName)(cookie => {
		complete(publicAuthConfig.authCookieName + "=" + cookie.value)
	})

	lazy val logout: Route = deleteCookie(publicAuthConfig.authCookieName, httpConfig.authDomain, "/"){
		complete(StatusCodes.OK)
	}

	protected def primitiveToJson[T](v: T): HttpResponse = {
		HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, v.toString))
	}

}
