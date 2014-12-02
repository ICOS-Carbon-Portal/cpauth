package eu.carbonportal.cpauth

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import akka.actor.ActorSystem
import akka.util.Timeout
import spray.routing.SimpleRoutingApp
import spray.http._

object Main extends App with SimpleRoutingApp with ProxyDirectives {
	implicit val system = ActorSystem("cpauth")
	implicit val timeout: Timeout = Timeout(60.seconds)
	import system.dispatcher

	val cookie = HttpCookie(
		name = "testcookie",
		content = "success",
//		secure = true,
		domain = Some(".localhost.local"),
		path = Some("/"),
//		maxAge = Some(10.minutes.toSeconds)
		httpOnly = true
	)
	
	startServer(interface = "::0", port = 8080) {
//		setCookie(cookie){
//			proxyTo(Uri.NamedHost("oleg.mirzov.com"), 80)
//		}
		path("login"){
			redirect(Saml.getAuthUrl, StatusCodes.Found)
		} ~
		post{
			path("saml/SSO/POST"){
				complete("Welcome!")
			}
		}
	}
	
}


