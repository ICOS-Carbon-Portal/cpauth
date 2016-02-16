package se.lu.nateko.cp.cpauth

import akka.actor.ActorSystem
import akka.pattern.ask
import se.lu.nateko.cp.cpauth.CpauthJsonProtocol._
import se.lu.nateko.cp.cpauth.accounts.Users
import se.lu.nateko.cp.cpauth.core.Authenticator
import se.lu.nateko.cp.cpauth.core.CoreUtils
import se.lu.nateko.cp.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cp.cpauth.opensaml.IdpLibrary
import spray.http.StatusCodes
import spray.routing.ExceptionHandler
import spray.routing.SimpleRoutingApp
import se.lu.nateko.cp.cpauth.core.AuthenticationFailedException
import spray.can.Http
import scala.concurrent.Await
import scala.concurrent.duration.Duration


object Main extends App with SimpleRoutingApp with SamlRouting with PasswordRouting with DrupalRouting {

	implicit val system = ActorSystem("cpauth")
	implicit val dispatcher = system.dispatcher
	implicit val scheduler = system.scheduler

	val config: CpauthConfig = ConfigReader.getDefault
	val (httpConfig, publicAuthConfig, samlConfig) = (config.http, config.auth.pub, config.saml)

	val assExtractorTry = AssertionExtractor(config.saml)
	val idpLib: IdpLibrary = IdpLibrary.fromConfig(config.saml)
	val cookieFactory = new CookieFactory(config)
	val targetLookup: TargetUrlLookup = new MapBasedUrlLookup
	val authenticator = Authenticator(publicAuthConfig)

	lazy val userDb = Users

	val cpauthExceptionHandler = ExceptionHandler{
		case AuthenticationFailedException => complete((StatusCodes.Forbidden, AuthenticationFailedException.getMessage))
		case ex =>
			val stack = ex.getStackTrace.map(_.toString).mkString("\n")
			complete((StatusCodes.InternalServerError, ex.getMessage + "\n" + stack))
	}

	startServer(interface = "127.0.0.1", port = config.http.servicePrivatePort) {
		handleExceptions(cpauthExceptionHandler){
			samlRoute ~
			passwordRoute ~
			drupalRoute ~
			get{
				path("logout")(logout) ~
				path("whoami"){
					user(uinfo => complete(uinfo)) ~ complete(StatusCodes.Unauthorized)
				} ~
				path("cpauthcookie"){
					cpauthCookie
				} ~
				pathEndOrSingleSlash{
					user(_ => redirect("/home/", StatusCodes.Found)) ~
					redirect("/login/", StatusCodes.Found)
				}
			}
		}
	}.onSuccess{ case _ =>
		sys.addShutdownHook{
//			val serverStop = akka.io.IO(Http).ask(Http.Unbind)
//			println("Sent Http.Unbind")
			akka.io.IO(Http) ! akka.actor.PoisonPill
			//println("Sent PoisonPill, waiting 1 sec")
			Thread.sleep(1000)
			//println("Shutting down the actor system")
			system.shutdown()
			//println("closing Users DB")
			Users.closeDb()
			//println("Users DB closed, done with the shutdown hook!")
		}
	}

}
