package se.lu.nateko.cpauth

import java.net.URI
import java.net.URL
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success
import org.opensaml.saml2.core.Response
import akka.actor.ActorSystem
import akka.util.Timeout
import core.CoreUtils
import se.lu.nateko.cpauth.CpauthJsonProtocol._
import se.lu.nateko.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cpauth.opensaml.Parser
import se.lu.nateko.cpauth.opensaml.AssertionExtractor
import spray.http.ContentType
import spray.http.FormData
import spray.http.HttpEntity
import spray.http.HttpResponse
import spray.http.MediaTypes
import spray.http.StatusCodes
import spray.http.Uri.apply
import spray.routing.SimpleRoutingApp
import se.lu.nateko.cpauth.core.Config


object Main extends App with SimpleRoutingApp with ProxyDirectives {

	implicit val system = ActorSystem("cpauth")
	implicit val timeout: Timeout = Timeout(60.seconds)
	import system.dispatcher

	val config: Config = se.lu.nateko.cpauth.core.Constants
	val assExtractorTry = AssertionExtractor(config)
	val idpLib: IdpLibrary = IdpLibrary.fromConfig(config)
	val cookieSetter = new CookieSetter(config)

	startServer(interface = "::0", port = 8080) {
		get{
			path("saml" / "login") {
				parameter('idpUrl, 'targetUrl ?){ (idp, target) =>

					setCookie(cookieSetter.getLastIdpCookie(idp)) {
						idpLib.getIdpProps(new URI(idp)) match{
							case Success(idpProp) =>
								redirect(getAuthenticatingRequestUrl(idpProp.ssoRedirect, target), StatusCodes.Found)
							case Failure(err) => completeWithError(err.getMessage)
						}
					}

				} ~
				completeWithError("Identity provider has not been specified!")
			} ~
			path("saml" / "cpauth"){
				val metadataXmlStr: String = CoreUtils.getResourceAsString(config.samlSpXmlPath)
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
							val summary = Playground.getResponseSummary(response, assExtractorTry, idpLib)
							val reply = summary.recover{case err => err.getMessage}
							complete(reply)
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

	def getAuthenticatingRequestUrl(idpUrl: URL, targetUrl: Option[String]): String = {
		targetUrl match{
			case None => Saml.getAuthUrl(idpUrl, config.consumerServiceUrl, config.spUrl)
			case Some(url) => Saml.getAuthUrl(idpUrl, config.consumerServiceUrl, config.spUrl, url)
		}

	}

}


