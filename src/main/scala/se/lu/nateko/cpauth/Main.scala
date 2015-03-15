package se.lu.nateko.cpauth

import java.net.URI
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.opensaml.saml2.core.Response
import akka.actor.ActorSystem
import akka.util.Timeout
import se.lu.nateko.cpauth.CpauthJsonProtocol._
import se.lu.nateko.cpauth.core.Authenticator
import se.lu.nateko.cpauth.core.Config
import se.lu.nateko.cpauth.core.CoreUtils
import se.lu.nateko.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cpauth.opensaml.IdpInfo
import se.lu.nateko.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cpauth.opensaml.Parser
import spray.http.ContentType
import spray.http.HttpEntity
import spray.http.HttpResponse
import spray.http.MediaTypes
import spray.http.StatusCodes
import spray.http.Uri
import spray.routing.SimpleRoutingApp
import spray.http.HttpCookie


object Main extends App with SimpleRoutingApp with ProxyDirectives {

	implicit val system = ActorSystem("cpauth")
	implicit val timeout: Timeout = Timeout(60.seconds)
	import system.dispatcher

	val config: Config = Constants
	val assExtractorTry = AssertionExtractor(config)
	val idpLib: IdpLibrary = IdpLibrary.fromConfig(config)
	val idpInfos: Seq[IdpInfo] = idpLib.getInfos.toSeq.sortBy(_.name)
	val cookieFactory = new CookieFactory(config)
	val targetLookup: TargetUrlLookup = new MapBasedUrlLookup
	val authenticator = Authenticator(config)
	val cpauthDirs = new CpauthDirectives(config, authenticator)
	import cpauthDirs._

	val metadataXml: HttpEntity = {
		val metadataXmlStr: String = CoreUtils.getResourceAsString(config.samlSpXmlPath)
		val xmlType = ContentType(MediaTypes.`application/xml`)
		HttpEntity(xmlType, metadataXmlStr)
	}

	startServer(interface = "::0", port = config.servicePrivatePort) {
		get{
			path("saml" / "login") {
				parameter('idpUrl, 'targetUrl ?){ (idp, target) =>

					val idpPropTry = for(
						idpUri <- Try(new URI(idp));
						prop <- idpLib.getIdpProps(idpUri)
					) yield prop

					attempt(idpPropTry) {idpProp =>
						setCookie(cookieFactory.getLastIdpCookie(idp)) {
							val (reqXml, reqId) = Saml.authRequestXmlAndId(config.spConfig)
							val reqUri = Saml.getAuthUri(idpProp.ssoRedirect, reqXml)
	
							for(
								uriStr <- target;
								uri <- Try(Uri(uriStr)).toOption
							) targetLookup.memorize(reqId, uri)
	
							redirect(reqUri, StatusCodes.Found)
						}
					}

				} ~ complete(HttpResponse(
						status = StatusCodes.BadRequest,
						entity = "Identity provider has not been specified!")
					)
			} ~
			path("saml" / "cpauth"){ complete(metadataXml) } ~
			path("saml" / "idps"){ complete(idpInfos) } ~
			path("whoami"){
				user(uinfo => complete(uinfo)) ~
				complete(HttpResponse(status = StatusCodes.Unauthorized))
			} ~ 
			parameter('drupallogin){ druplogin =>
				user{ uinfo =>
					proxyTo(Uri.IPv4Host(config.drupalPrivateHost), config.drupalPrivatePort,
						("login", "1"),
						("givenName", uinfo.givenName),
						("surname", uinfo.surname),
						("mail", uinfo.mail))
				} ~ attempt(Uri(druplogin))(target => {
					val nextTarget = target.withQuery(("drupallogin", druplogin) +: target.query).toString
					val redirectUri = Uri(config.serviceUrl + config.loginPath).withQuery(("targetUrl", nextTarget))
					redirect(redirectUri, StatusCodes.Found)
				})
			} ~
			complete(StatusCodes.NotFound)
		} ~
		post{
			path("saml" / "SAML2" / "POST"){
				formField('SAMLResponse){ resp =>

					val cookieAndReqIdTry: Try[(HttpCookie, String)] = for(
						extractor <- assExtractorTry;
						response <- Try(Parser.fromBase64[Response](resp));
						cookie <- cookieFactory.makeAuthenticationCookie(response, extractor, idpLib)
					) yield (cookie, response.getInResponseTo)

					attempt(cookieAndReqIdTry){ case (cookie, reqId) =>
						setCookie(cookie) {
							val target: Option[Uri] = targetLookup.getAndForget(reqId)
							redirect(target.getOrElse(Uri("/whoami")), StatusCodes.Found)
						}
					}
				}
			}
		}

	}

	def completeWithError(msg: String) = complete{
		HttpResponse(status = StatusCodes.BadRequest, entity = msg)
	}

}


