package se.lu.nateko.cp.cpauth.routing

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import se.lu.nateko.cp.cpauth.accounts.RestHeartClient
import se.lu.nateko.cp.cpauth.core.AuthSource
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.oauth.FacebookAuthenticationService
import se.lu.nateko.cp.cpauth.oauth.OrcidAuthenticationService
import se.lu.nateko.cp.cpauth.services.CookieFactory
import se.lu.nateko.cp.cpauth.Envri.Envri
import se.lu.nateko.cp.cpauth.CpauthConfig
import akka.actor.ActorSystem
import se.lu.nateko.cp.cpauth.OAuthProvider

trait OAuthRouting extends CpauthDirectives{

	def oauthConfig: CpauthConfig.OAuthConfig
	def cookieFactory: CookieFactory
	implicit def dispatcher: ExecutionContext
	implicit def system: ActorSystem
	def restHeart: RestHeartClient

	val facebookRoute: Route = (pathPrefix("oauth" / "facebook") & extractEnvri){implicit envri =>
		oauthRoute(cpauthTokenFromFacebook, AuthSource.Facebook)
	}

	val orcidRoute: Route = (pathPrefix("oauth" / "orcidid") & extractEnvri){implicit envri =>
		oauthRoute(cpauthTokenFromOrcidId, AuthSource.Orcid)
	}

	private def cpauthTokenFromFacebook(code: String)(implicit envri: Envri): Future[UserId] = {
		facebookAuth.retrieveUserInfo(code).map{userInfo =>
			val uid = UserId(userInfo.email)

			//Silent side effect: creating user profile if it does not already exist
			restHeart.createUserIfNew(uid, userInfo.givenName, userInfo.surname)

			uid
		}
	}

	private def cpauthTokenFromOrcidId(code: String)(implicit envri: Envri): Future[UserId] = {
		orcidIdAuthenticationService.retrieveUserInfo(code)
			.flatMap(userInfo => userInfo.email match {
				case Some(email) =>
					val uid = UserId(email)

					//Silent side effect: creating user profile if it does not already exist
					restHeart.createUserIfNew(uid, userInfo.givenName.getOrElse(""), userInfo.surname.getOrElse(""))

					Future.successful(uid)
				case None =>
					restHeart.findUsers(Map("profile.orcid" -> userInfo.orcidId))
						.map(_.headOption.getOrElse(throw new Exception(
							"You need to either make your (verified!) email public in your OrcidID account, " +
							"or log in to CP by other means first, and specify your OrcidId in your CP user profile"
						)))
			})
	}

	private def oauthRoute(uidProvider: String => Future[UserId], source: AuthSource.AuthSource)(implicit envri: Envri): Route = {
		parameters(("code", "state".?)){(code, targetUrl) =>
			val tokenFut: Future[String] = uidProvider(code).flatMap{uid =>
				Future.fromTry(
					cookieFactory.makeTokenBase64(uid, source)
				)
			}
			onSuccess(tokenFut){token =>
				setCookie(cookieFactory.makeAuthCookie(token)){

					targetUrl match{
						case Some(target) =>
							//getting rid of Facebook's appended #_=_
							val uri = if(Uri(target).fragment.isDefined) target else target + "#"
							redirect(uri, StatusCodes.Found)

						case None => redirect("/#", StatusCodes.Found)
					}
				}
			}
		}
	}

	private def facebookAuth(implicit envri: Envri) = new FacebookAuthenticationService(
		oauthConfig(envri)(OAuthProvider.facebook)
	)

	private def orcidIdAuthenticationService(implicit envri: Envri) = new OrcidAuthenticationService(
		oauthConfig(envri)(OAuthProvider.orcidid)
	)

}
