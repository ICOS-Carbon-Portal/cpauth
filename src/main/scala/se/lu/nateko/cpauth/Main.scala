package se.lu.nateko.cpauth

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import akka.actor.ActorSystem
import akka.util.Timeout
import spray.routing.SimpleRoutingApp
import spray.http._
import spray.routing.HttpService._

import core.CoreUtils
import core.Constants

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

	def completeWithError(msg: String) = complete{
		HttpResponse(status = StatusCodes.InternalServerError, entity = msg)
	}

	def getSamlResponse(formData: FormData): Option[String] = formData.fields
		.collect{case ("SAMLResponse", resp) => resp}.headOption

	def getAuthenticatingRequestUrl: String = {
		Saml.getAuthUrl(Constants.idpUrl, Constants.consumerServiceUrl, Constants.spUrl)
	}

	startServer(interface = "::0", port = 8080) {
//		setCookie(cookie){
//			proxyTo(Uri.NamedHost("icos-cp.eu"), 80)
//		}
		get{
			path("saml" / "login"){
				_.redirect(getAuthenticatingRequestUrl, StatusCodes.Found)
			} ~
			path("saml" / "cpauth"){
				val metadataXmlStr: String = CoreUtils.getResourceLines(Constants.samlSpXmlPath).mkString("")
				val xmlType = ContentType(MediaTypes.`application/xml`)
				val metadataXmlEntity = HttpEntity(xmlType, metadataXmlStr)

				complete(metadataXmlEntity)
			}
		} ~
		post{
			path("saml" / "SAML2" / "POST"){
				entity(as[FormData]){ fd =>
					getSamlResponse(fd) match{
						case None => completeWithError("No SAMLResponse received")
						case Some(resp) =>
							val response = CoreUtils.decode64(resp)
							complete(Playground.getResponseSummary(response))
					}
				}
			}
		}

	}
	
}


