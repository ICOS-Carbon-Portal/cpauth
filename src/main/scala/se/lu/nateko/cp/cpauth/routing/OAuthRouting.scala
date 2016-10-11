package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import akka.event.LoggingAdapter
import se.lu.nateko.cp.cpauth.oauth.facebook.FacebookAuthenticationService

trait OAuthRouting {

  def log: LoggingAdapter
  def facebookAuth: FacebookAuthenticationService
  
  def oauthRoute: Route = pathPrefix("oauth" / "facebook" / "auth"){
    parameter("code"){code =>
      val userInfo = facebookAuth.retrieveUserInfo(code)
      complete(userInfo.email + userInfo.givenName + userInfo.surname)
    }

    
    
	}

}