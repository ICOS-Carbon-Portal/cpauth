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
import se.lu.nateko.cp.cpauth.oauth.{FacebookAuthenticationService, OrcidAuthenticationService}
import se.lu.nateko.cp.cpauth.services.CookieFactory

trait OAuthRouting {

	def facebookAuth: FacebookAuthenticationService
	def cookieFactory: CookieFactory
	def blockingExeContext: ExecutionContext
	def dispatcher: ExecutionContext
	def restHeart: RestHeartClient
	def orcidIdAuthenticationService: OrcidAuthenticationService

	def oauthRoute: Route = pathPrefix("oauth" / "facebook"){

		parameters('code, 'state ?){(code, targetUrl) =>

			onSuccess(cpauthTokenFromFacebook(code: String)){token =>

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

	private[this] def cpauthTokenFromFacebook(code: String): Future[String] = {
		implicit val exeCtxt = blockingExeContext

		Future{
			val userInfo = facebookAuth.retrieveUserInfo(code)
			val uid = UserId(userInfo.email)

			//Silent side effect: creating user profile if it does not already exist
			restHeart.createUserIfNew(uid, userInfo.givenName, userInfo.surname)

			uid
		}.flatMap{uid =>
			Future.fromTry(
				cookieFactory.makeTokenBase64(uid, AuthSource.Facebook)
			)
		}
	}

	//TODO Remove code duplication with oauthRoute
	def orcididRoute: Route = pathPrefix("oauth" / "orcidid"){

		parameters('code, 'state ?){(code, targetUrl) =>
			onSuccess(cpauthTokenFromOrcidId(code: String)){token =>

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

	private[this] def cpauthTokenFromOrcidId(code: String): Future[String] = {
		implicit val exeCtxt = dispatcher

		orcidIdAuthenticationService.retrieveUserInfo(code)
			.map(userInfo => {
				UserId(userInfo.email)
			})
			.flatMap{uid =>
				Future.fromTry(
					cookieFactory.makeTokenBase64(uid, AuthSource.Orcid)
				)
			}
	}

}
