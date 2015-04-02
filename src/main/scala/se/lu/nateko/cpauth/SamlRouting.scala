package se.lu.nateko.cpauth

import spray.routing.Directives
import spray.routing.Route
import java.net.URI
import scala.util.Try
import se.lu.nateko.cpauth.opensaml.IdpLibrary
import spray.http.Uri
import spray.http.StatusCodes
import se.lu.nateko.cpauth.opensaml.IdpInfo
import se.lu.nateko.cpauth.CpauthJsonProtocol._
import spray.http.HttpEntity
import spray.http.ContentType
import spray.http.MediaTypes
import spray.http.HttpCookie
import org.opensaml.saml2.core.Response
import se.lu.nateko.cpauth.opensaml.Parser
import se.lu.nateko.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cpauth.core.SamlConfig

trait SamlRouting extends Directives with CpauthDirectives{

	def samlConfig: SamlConfig
	def idpLib: IdpLibrary
	def cookieFactory: CookieFactory
	def targetLookup: TargetUrlLookup
	def metadataXmlStr: String
	def assExtractorTry: Try[AssertionExtractor]

	lazy val idpInfos: Seq[IdpInfo] = idpLib.getInfos.toSeq.sortBy(_.name)
	lazy val metadataXml: HttpEntity = {
		val xmlType = ContentType(MediaTypes.`application/xml`)
		HttpEntity(xmlType, metadataXmlStr)
	}

	val samlRoute: Route = pathPrefix("saml"){
		get{
			path("login") {
				parameter('idpUrl, 'targetUrl ?){ (idp, target) =>

					val idpPropTry = for(
						idpUri <- Try(new URI(idp));
						prop <- idpLib.getIdpProps(idpUri)
					) yield prop

					attempt(idpPropTry) {idpProp =>
						setCookie(cookieFactory.getLastIdpCookie(idp)) {
							val (reqXml, reqId) = Saml.authRequestXmlAndId(samlConfig.spConfig)
							val reqUri = Saml.getAuthUri(idpProp.ssoRedirect, reqXml)

							for(
								uriStr <- target if(uriStr != null && uriStr.trim.length > 0);
								uri <- Try(Uri(uriStr)).toOption
							) targetLookup.memorize(reqId, uri)

							redirect(reqUri, StatusCodes.Found)
						}
					}
				} ~ complete((StatusCodes.BadRequest, "Identity provider has not been specified!"))
			} ~
			path("cpauth"){ complete(metadataXml) } ~
			path("idps"){ complete(idpInfos) }
		} ~
		post{
			path("SAML2" / "POST"){
				formField('SAMLResponse){ resp =>

					val cookieAndReqIdTry: Try[(HttpCookie, String)] = for(
						extractor <- assExtractorTry;
						response <- Try(Parser.fromBase64[Response](resp));
						cookie <- cookieFactory.makeAuthenticationCookie(response, extractor, idpLib)
					) yield (cookie, response.getInResponseTo)

					attempt(cookieAndReqIdTry){ case (cookie, reqId) =>
						setCookie(cookie) {
							val target: Option[Uri] = targetLookup.getAndForget(reqId)
							redirect(target.getOrElse(Uri("/home/")), StatusCodes.Found)
						}
					}
				}
			}
		}
	}
}