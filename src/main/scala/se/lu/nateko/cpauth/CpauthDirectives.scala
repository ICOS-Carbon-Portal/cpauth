package se.lu.nateko.cpauth

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import se.lu.nateko.cpauth.core.Authenticator
import se.lu.nateko.cpauth.core.Config
import se.lu.nateko.cpauth.core.CookieToToken
import se.lu.nateko.cpauth.core.UserInfo
import spray.http.HttpHeader
import spray.http.HttpHeaders
import spray.http.HttpResponse
import spray.http.StatusCodes
import spray.routing.AuthenticationFailedRejection
import spray.routing.Directives
import spray.routing.RequestContext
import spray.routing.Route

class CpauthDirectives(config: Config, authenticator: Try[Authenticator]) extends Directives {

	def attempt[T](thunk: => T)(f: T => Route): Route = attempt(Try(thunk))(f)

	def attempt[T](attempt: Try[T])(f: T => Route): Route = attempt match {
		case Success(t) => f(t)
		case Failure(err) => ctxt => ctxt.complete{
			HttpResponse(status = StatusCodes.BadRequest, entity = err.getMessage)
		}
	}

	def user(inner: UserInfo => Route): Route = cookie(config.authCookieName)(cookie => {
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

	private def getCookieHeaders(ctxt: RequestContext): List[HttpHeader] = {
		val unfiltered: List[HttpHeader] = ctxt.request.headers
		unfiltered.filter(_.is(HttpHeaders.Cookie.lowercaseName))
	}
}