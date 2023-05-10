package se.lu.nateko.cp.cpauth.routing

import akka.actor.Scheduler
import akka.http.javadsl.server.CustomRejection
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.*
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.Directive
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.MissingCookieRejection
import akka.http.scaladsl.server.RejectionHandler
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.StandardRoute
import play.twirl.api.Html
import se.lu.nateko.cp.cpauth.AuthConfig
import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.accounts.RestHeartClient
import se.lu.nateko.cp.cpauth.accounts.UsersIo
import se.lu.nateko.cp.cpauth.core.AuthSource
import se.lu.nateko.cp.cpauth.core.AuthToken
import se.lu.nateko.cp.cpauth.core.Authenticator
import se.lu.nateko.cp.cpauth.core.CookieToToken
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.utils.TemplatePageMarshalling
import se.lu.nateko.cp.cpauth.utils.Utils
import spray.json.RootJsonFormat
import spray.json.RootJsonWriter

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import SprayJsonSupport.sprayJsValueMarshaller
import se.lu.nateko.cp.cpauth.core.AnonId


trait CpauthDirectives {

	def authConfig: AuthConfig
	def userDb: UsersIo
	def restHeart: RestHeartClient
	def hostToEnvri(host: String): Option[Envri]

	given dispatcher: ExecutionContext
	given scheduler: Scheduler
	given ToResponseMarshaller[Html] = TemplatePageMarshalling.marshaller[Html]
	given [T: RootJsonWriter]: ToEntityMarshaller[T] = SprayJsonSupport.sprayJsonMarshaller

	def publicAuthConfig(using envri: Envri) = authConfig.pub(envri)
	def authenticator(using Envri): Try[Authenticator] = Authenticator(publicAuthConfig)
	def anonymizeCpUser(uid: UserId): AnonId = AnonId(uid, authConfig.secretUserSalt)

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
	val userOpt: Directive1[Option[UserId]] = user.map(Some.apply) | provide(None)

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

	def handleAuthRejections(context: String): Directive0 = {
		val err = s"Authentication/authorization error while $context:"
		val authRejectionHandler = RejectionHandler.newBuilder()
			.handle{
				case CpauthCookieMissingRejection => complete((StatusCodes.Unauthorized, s"$err authentication cookie missing"))
				case WrongCpauthCookieSourceRejection => complete((StatusCodes.Forbidden, s"$err wrong kind of authentication method"))
				case bad: BadCpauthCookieRejection => complete((StatusCodes.Unauthorized, s"$err ${bad.message}"))
			}
			.result()
		handleRejections(authRejectionHandler)
	}

	lazy val whoami: Route =  extractEnvri { implicit envri =>
		addAccessControlHeaders(envri) {
			(get & handleAuthRejections("Login control")) {
				user { userId =>
					import se.lu.nateko.cp.cpauth.CpauthJsonProtocol.{given RootJsonFormat[UserId]}
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
	def addAccessControlHeaders(implicit envri: Envri): Directive0 = headerValueByType(Origin).flatMap{origin =>
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

sealed trait CpauthCustomRejection extends CustomRejection
case object CpauthCookieMissingRejection extends CpauthCustomRejection
case object WrongCpauthCookieSourceRejection extends CpauthCustomRejection

class BadCpauthCookieRejection(err: Throwable) extends CpauthCustomRejection{
	val message = err.getMessage + err.getStackTrace.map(_.toString).mkString("\n", "\n", "")
}
