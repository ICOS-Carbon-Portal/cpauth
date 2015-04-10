package se.lu.nateko.cp.cpauth

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import se.lu.nateko.cp.cpauth.core.Authenticator
import se.lu.nateko.cp.cpauth.core.CookieToToken
import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import se.lu.nateko.cp.cpauth.core.UrlsConfig
import se.lu.nateko.cp.cpauth.core.UserInfo
import spray.http.HttpHeader
import spray.http.HttpHeaders
import spray.http.HttpResponse
import spray.http.StatusCodes
import spray.routing.AuthenticationFailedRejection
import spray.routing.Directives
import spray.routing.RequestContext
import spray.routing.Route

trait CpauthDirectives extends Directives {

	def publicAuthConfig: PublicAuthConfig with UrlsConfig
	def authenticator: Try[Authenticator]
	implicit val dispatcher: ExecutionContext

	def attempt[T](thunk: => T)(f: T => Route): Route = attempt(Try(thunk))(f)

	def attempt[T](attempt: Try[T])(f: T => Route): Route = attempt match {
		case Success(t) => f(t)
		case Failure(err) => complete((StatusCodes.BadRequest, err.getMessage))
	}

	def user(inner: UserInfo => Route): Route = cookie(publicAuthConfig.authCookieName)(cookie => {
		val userTry = for(
			auth <- authenticator;
			token <- CookieToToken.recoverToken(cookie);
			uinfo <- auth.unwrapUserInfo(token)
		) yield uinfo

		userTry match{
			case Success(uinfo) => inner(uinfo)
			case Failure(err) => extract(getCookieHeaders){ headers =>
				reject(new AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, headers))
			}
		}
	}) ~ reject(new AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, Nil))

	lazy val logout: Route = deleteCookie(publicAuthConfig.authCookieName, publicAuthConfig.authDomain, "/"){
		complete(StatusCodes.OK)
	}

	protected def primitiveToJson[T](v: T): HttpResponse = {
		import spray.http.HttpEntity
		import spray.http.ContentTypes
		HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, v.toString))
	}

	private def getCookieHeaders(ctxt: RequestContext): List[HttpHeader] = {
		val unfiltered: List[HttpHeader] = ctxt.request.headers
		unfiltered.filter(_.is(HttpHeaders.Cookie.lowercaseName))
	}
}