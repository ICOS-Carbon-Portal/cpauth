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
import spray.http.Uri
import spray.routing.AuthenticationFailedRejection
import spray.routing.Directives
import spray.routing.RequestContext
import spray.routing.Route
import se.lu.nateko.cpauth.accounts.Users
import scala.concurrent.ExecutionContext

class CpauthDirectives(config: Config, authenticator: Try[Authenticator])(implicit ex: ExecutionContext) extends Directives {

	val remakeCookies = mapHttpResponseHeaders(_.map(remakeCookie))

	def redirectWhenDone(target: Uri, dropParam: Option[String] = None) =
		respondWithHeader(HttpHeaders.Location(withoutParam(dropParam, target))) &
		respondWithStatus(StatusCodes.Found) &
		remakeCookies

	private def withoutParam(param: Option[String], uri: Uri): Uri = param match{
		case None => uri
		case Some(drop) =>
			val filteredQuery = uri.query.filter{
				case (`drop`, _) => false
				case _ => true
			}
			uri.withQuery(filteredQuery)
	}

	private def remakeCookie(header: HttpHeader): HttpHeader = header match{
		case HttpHeaders.`Set-Cookie`(cookie) =>
			val newCookie = cookie.copy(secure = true, httpOnly =true, expires = None)
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

	def admin(inner: => Route): Route = user(uinfo =>
		onComplete(Users.userIsAdmin(uinfo.mail)){
			case Failure(err) => failWith(err)
			case Success(false) => complete(StatusCodes.Unauthorized)
			case Success(true) => inner
		}
	)

	private def getCookieHeaders(ctxt: RequestContext): List[HttpHeader] = {
		val unfiltered: List[HttpHeader] = ctxt.request.headers
		unfiltered.filter(_.is(HttpHeaders.Cookie.lowercaseName))
	}
}