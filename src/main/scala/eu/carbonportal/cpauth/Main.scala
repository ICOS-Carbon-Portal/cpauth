package eu.carbonportal.cpauth

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import akka.actor.ActorSystem
import akka.util.Timeout
import spray.http.Uri
import spray.routing.SimpleRoutingApp

object Main extends App with SimpleRoutingApp with ProxyDirectives {
	implicit val system = ActorSystem("cpauth")
	implicit val timeout: Timeout = Timeout(60.seconds)
	import scala.concurrent.ExecutionContext.Implicits.global

	startServer(interface = "::0", port = 8080) {
		proxyTo(Uri.NamedHost("spray.io"), 80)
	}
	
	
}


