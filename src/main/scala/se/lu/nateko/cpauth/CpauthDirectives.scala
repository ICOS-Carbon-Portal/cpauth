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
import spray.routing.Directive0
import spray.routing.Directives
import spray.routing.RequestContext
import spray.routing.Route
import spray.http.Uri
import spray.http.HttpHeaders

class CpauthDirectives(config: Config, authenticator: Try[Authenticator]) extends Directives {

	def redirectWhenDone(target: Uri) = respondWithHeader(HttpHeaders.Location(target)) &
											respondWithStatus(StatusCodes.Found) &
											setCookieHost(target.authority.host)

	def setCookieHost(host: Uri.Host): Directive0 = mapHttpResponseHeaders(_.map(setCookieHost(host, _)))
	
	private def setCookieHost(host: Uri.Host, header: HttpHeader): HttpHeader = header match{
		case HttpHeaders.`Set-Cookie`(cookie) =>
			val newDomain = {
				val segments = host.address.split('.')
				if(segments.length <= 2) host.address
				else segments.tail.map("." + _).mkString
			}
			val newCookie = cookie.copy(domain = Some(newDomain), secure = true)
			HttpHeaders.`Set-Cookie`(newCookie)
		case x => x
	}

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