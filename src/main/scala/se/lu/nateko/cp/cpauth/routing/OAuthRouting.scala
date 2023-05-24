package se.lu.nateko.cp.cpauth.routing

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import se.lu.nateko.cp.cpauth.CpauthConfig
import se.lu.nateko.cp.cpauth.Envri.Envri
import se.lu.nateko.cp.cpauth.OAuthProvider
import se.lu.nateko.cp.cpauth.accounts.RestHeartClient
import se.lu.nateko.cp.cpauth.core.AuthSource
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.oauth.*
import se.lu.nateko.cp.cpauth.services.CookieFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

trait OAuthRouting extends CpauthDirectives:

	def oauthConfig: CpauthConfig.OAuthConfig
	def cookieFactory: CookieFactory
	given system: ActorSystem
	def restHeart: RestHeartClient

	val oauthRoute: Route = (pathPrefix("oauth") & extractEnvri) { implicit envri =>
		pathPrefix("facebook"){
			oauthRoute(facebookAuth.retrieveUserInfo(_).map(uinfoToToken), AuthSource.Facebook)
		} ~
		pathPrefix("orcidid"){
			oauthRoute(cpauthTokenFromOrcidId, AuthSource.Orcid)
		} ~
		pathPrefix("atmoAccess"){
			atmoAccessAuth match
				case None => complete(StatusCodes.InternalServerError -> s"ATMO ACCESS authentication is not configured for $envri")
				case Some(atmoService) =>
					oauthRoute(atmoService.retrieveUserInfo(_).map(uinfoToToken), AuthSource.AtmoAccess)
		} ~
		complete(StatusCodes.NotFound)
	}

	private def uinfoToToken(userInfo: UserInfo)(using Envri): UserId =
		val uid = UserId(userInfo.email)
		//Silent side effect: creating user profile if it does not already exist
		restHeart.createUserIfNew(uid, userInfo.givenName, userInfo.surname)
		uid

	private def cpauthTokenFromOrcidId(code: String)(using Envri): Future[UserId] = orcidAuth
		.retrieveUserInfo(code)
		.flatMap(userInfo => userInfo.toPlainUserInfo match
			case Some(uinfo) =>
				Future.successful(uinfoToToken(uinfo))
			case None =>
				restHeart.findUsers(Map("profile.orcid" -> userInfo.orcidId))
					.map(_.headOption.getOrElse(throw new Exception(
						"You need to either make your (verified!) email public in your OrcidID account, " +
						"or log in to CP by other means first, and specify your OrcidId in your CP user profile"
					)))
		)

	private def oauthRoute(uidProvider: String => Future[UserId], source: AuthSource)(using Envri): Route = {
		parameters("code", "state".?){(code, targetUrl) =>
			val tokenFut: Future[String] = uidProvider(code).flatMap{uid =>
				Future.fromTry(
					cookieFactory.makeTokenBase64(uid, source)
				)
			}
			onSuccess(tokenFut){token =>
				setCookie(cookieFactory.makeAuthCookie(token)){

					targetUrl match
						case Some(target) =>
							//getting rid of Facebook's appended #_=_
							val uri = if(Uri(target).fragment.isDefined) target else target + "#"
							redirect(uri, StatusCodes.Found)

						case None => redirect("/#", StatusCodes.Found)
				}
			}
		}
	}

	private def facebookAuth(using envri: Envri) = new FacebookAuthenticationService(
		oauthConfig(envri)(OAuthProvider.facebook)
	)

	private def orcidAuth(using envri: Envri) = new OrcidAuthenticationService(
		oauthConfig(envri)(OAuthProvider.orcidid)
	)

	private def atmoAccessAuth(using envri: Envri): Option[AtmoAccessAuthenticationService] =
		for
			envriConf <- oauthConfig.get(envri)
			conf <- envriConf.get(OAuthProvider.atmoAccess)
		yield new AtmoAccessAuthenticationService(conf)

end OAuthRouting
