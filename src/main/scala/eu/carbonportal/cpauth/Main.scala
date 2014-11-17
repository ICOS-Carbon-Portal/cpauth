package eu.carbonportal.cpauth

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import akka.actor.ActorSystem
import akka.util.Timeout
import spray.http.Uri
import spray.routing.SimpleRoutingApp
import spray.http.HttpCookie

object Main extends App with SimpleRoutingApp with ProxyDirectives {
	implicit val system = ActorSystem("cpauth")
	implicit val timeout: Timeout = Timeout(60.seconds)
	import scala.concurrent.ExecutionContext.Implicits.global

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
		setCookie(cookie){
			proxyTo(Uri.NamedHost("oleg.mirzov.com"), 80)
		}
	}
	
	
}


