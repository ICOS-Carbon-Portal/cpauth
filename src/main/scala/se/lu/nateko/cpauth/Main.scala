package se.lu.nateko.cpauth

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import akka.actor.ActorSystem
import akka.util.Timeout
import spray.routing.SimpleRoutingApp
import spray.http._
import spray.routing.HttpService._
import core.Crypto

object Main extends App with SimpleRoutingApp with ProxyDirectives {

	//val idpUrl = "https://idp.lu.se/idp/shibboleth"
	//val idpUrl = "https://idp.testshib.org/idp/shibboleth"

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
	
	def completeWithError(msg: String) = complete{
		HttpResponse(status = StatusCodes.InternalServerError, entity = msg)
	}
	
	def getSamlResponse(formData: FormData): Option[String] = formData.fields
		.collect{case ("SAMLResponse", resp) => resp}.headOption
	
	startServer(interface = "::0", port = 8080) {
//		setCookie(cookie){
//			proxyTo(Uri.NamedHost("oleg.mirzov.com"), 80)
//		}
		path("login"){
			_.redirect(Saml.getAuthUrl, StatusCodes.Found)
		} ~
		post{
			path("saml" / "SAML2" / "POST"){
				entity(as[FormData]){ fd =>
					getSamlResponse(fd) match{
						case None => completeWithError("No SAMLResponse received")
						case Some(resp) =>
							val response = Crypto.decode64(resp)
							complete(Playground.getResponseSummary(response))
					}
				}
			}
		}
	}
	
}


