package se.lu.nateko.cpauth

import se.lu.nateko.cpauth.core.Config
import spray.routing.Directives
import spray.routing.Directive1
import scala.util.Try
import se.lu.nateko.cpauth.core.Authenticator
import se.lu.nateko.cpauth.core.UserInfo
import se.lu.nateko.cpauth.core.CookieToToken
import spray.routing.Route
import scala.util.Success
import scala.util.Failure
import spray.routing.AuthenticationFailedRejection
import spray.http.HttpHeaders
import spray.routing.RequestContext
import spray.http.HttpHeader

class CpauthDirectives(config: Config, authenticator: Try[Authenticator]) extends Directives {

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