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
import spray.routing.AuthenticationFailedRejection
import spray.routing.Directives
import spray.routing.Directive1
import spray.routing.RequestContext
import spray.routing.Route

class CpauthDirectives(config: Config, authenticator: Try[Authenticator]) extends Directives {

//	def attempt[T](attempt: => T): Directive1[T]

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