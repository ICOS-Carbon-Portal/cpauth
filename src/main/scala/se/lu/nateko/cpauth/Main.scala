package se.lu.nateko.cpauth

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import se.lu.nateko.cpauth.CpauthJsonProtocol._
import se.lu.nateko.cpauth.accounts.Users
import se.lu.nateko.cpauth.core.Authenticator
import se.lu.nateko.cpauth.core.Config
import se.lu.nateko.cpauth.core.CoreUtils
import se.lu.nateko.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cpauth.opensaml.IdpLibrary
import spray.http.StatusCodes
import spray.routing.SimpleRoutingApp


object Main extends App with SimpleRoutingApp with SamlRouting with PasswordRouting with DrupalRouting {

	implicit val system = ActorSystem("cpauth")
	implicit val timeout = Timeout(60.seconds)
	implicit val dispatcher = system.dispatcher

	val config: Config = Constants
	val (urlsConfig, publicAuthConfig, samlConfig) = (config, config, config)

	val assExtractorTry = AssertionExtractor(config)
	val idpLib: IdpLibrary = IdpLibrary.fromConfig(config)
	val cookieFactory = new CookieFactory(config)
	val targetLookup: TargetUrlLookup = new MapBasedUrlLookup
	val authenticator = Authenticator(config)
	val metadataXmlStr: String = CoreUtils.getResourceAsString(config.samlSpXmlPath)
	val userDb = Users

	startServer(interface = "::0", port = config.servicePrivatePort) {
		samlRoute ~
		passwordRoute ~
		get{
			path("logout"){
				deleteCookie(config.authCookieName, config.authDomain, "/"){complete(StatusCodes.OK)}
			} ~
			path("whoami"){
				user(uinfo => complete(uinfo)) ~ complete(StatusCodes.Unauthorized)
			}
		} ~
		drupalRoute
	}

}
