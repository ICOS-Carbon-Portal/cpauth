package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import akka.event.LoggingAdapter
import se.lu.nateko.cp.cpauth.oauth.facebook.FacebookAuthenticationService
import se.lu.nateko.cp.cpauth.services.CookieFactory
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.core.AuthSource
import scala.util.Success
import scala.util.Failure

trait OAuthRouting {

	//def log: LoggingAdapter
	def facebookAuth: FacebookAuthenticationService
	def cookieFactory: CookieFactory

	def oauthRoute: Route = pathPrefix("oauth" / "facebook"){

		parameters('code, 'state ?){(code, targetUrl) =>

			//TODO Change this code to async
			val userInfo = facebookAuth.retrieveUserInfo(code)
			val uid = UserId(userInfo.email)

			//TODO Join this operation with retrieveUserInfo, return Future
			cookieFactory.makeTokenBase64(uid, AuthSource.Facebook) match{

				case Success(token) =>
					val cookie = cookieFactory.makeAuthCookie(token)

					setCookie(cookie){
						targetUrl match{
							case Some(target) => redirect(target, StatusCodes.Found)
							case None => redirect("/#", StatusCodes.Found)
						}
					}

				case Failure(err) => failWith(err)
			}

		}

	}

}