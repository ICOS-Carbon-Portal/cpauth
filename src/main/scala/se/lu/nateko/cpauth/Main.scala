package se.lu.nateko.cpauth

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import akka.actor.ActorSystem
import akka.util.Timeout
import spray.routing.SimpleRoutingApp
import spray.http._
import spray.json.DefaultJsonProtocol
import spray.httpx.SprayJsonSupport
import spray.routing.HttpService._
import core.CoreUtils
import core.Constants
import se.lu.nateko.cpauth.opensaml.ResponseAnalyzer
import org.opensaml.saml2.core.Response
import se.lu.nateko.cpauth.opensaml.Parser
import se.lu.nateko.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cpauth.opensaml.IdpInfo


object CpauthJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport{
	implicit val idpInfoFormat = jsonFormat2(IdpInfo)
}

import CpauthJsonProtocol._

object Main extends App with SimpleRoutingApp with ProxyDirectives {

	implicit val system = ActorSystem("cpauth")
	implicit val timeout: Timeout = Timeout(60.seconds)
	import system.dispatcher

	val analyzer = ResponseAnalyzer(Constants)
	val idpLib: IdpLibrary = IdpLibrary.fromConfig(Constants)
	
	startServer(interface = "::0", port = 8080) {
//		setCookie(cookie){
//			proxyTo(Uri.NamedHost("icos-cp.eu"), 80)
//		}
		get{
			path("saml" / "login") {
				parameter('idpUrl, 'targetUrl ?){ (idp, target) =>
					redirect(getAuthenticatingRequestUrl(idp, target), StatusCodes.Found)
				} ~
				completeWithError("Identity provider has not been specified!")
			} ~
			path("saml" / "cpauth"){
				val metadataXmlStr: String = CoreUtils.getResourceLines(Constants.samlSpXmlPath).mkString("")
				val xmlType = ContentType(MediaTypes.`application/xml`)
				val metadataXmlEntity = HttpEntity(xmlType, metadataXmlStr)

				complete(metadataXmlEntity)
			} ~
			path("saml" / "idps"){
				val infos = idpLib.getInfos.toSeq.sortBy(_.name)
				complete(infos)
			}
		} ~
		post{
			path("saml" / "SAML2" / "POST"){
				entity(as[FormData]){ fd =>
					getSamlResponse(fd) match{
						case None => completeWithError("No SAMLResponse received")
						case Some(resp) =>
							val response = Parser.fromBase64[Response](resp)
							complete(Playground.getResponseSummary(response, analyzer.get))
							//complete(CoreUtils.decode64(resp))
					}
				}
			}
		}

	}

	def completeWithError(msg: String) = complete{
		HttpResponse(status = StatusCodes.BadRequest, entity = msg)
	}

	def getSamlResponse(formData: FormData): Option[String] = formData.fields
		.collect{case ("SAMLResponse", resp) => resp}.headOption

	def getAuthenticatingRequestUrl(idpUrl: String, targetUrl: Option[String]): String = {
		import Constants._
		targetUrl match{
			case None => Saml.getAuthUrl(idpUrl, consumerServiceUrl, spUrl)
			case Some(url) => Saml.getAuthUrl(idpUrl, consumerServiceUrl, spUrl, url)
		}

	}

}


