package se.lu.nateko.cp.cpauth.routing

import java.net.URI
import scala.util.Try
import se.lu.nateko.cp.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cp.cpauth.opensaml.IdpInfo
import se.lu.nateko.cp.cpauth.CpauthJsonProtocol._
import org.opensaml.saml2.core.Response
import se.lu.nateko.cp.cpauth.opensaml.Parser
import se.lu.nateko.cp.cpauth.opensaml.AssertionExtractor
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.Uri
import se.lu.nateko.cp.cpauth.CookieFactory
import se.lu.nateko.cp.cpauth.Saml
import se.lu.nateko.cp.cpauth.SamlConfig
import se.lu.nateko.cp.cpauth.TargetUrlLookup

trait SamlRouting extends CpauthDirectives{

	def samlConfig: SamlConfig
	def idpLib: IdpLibrary
	def cookieFactory: CookieFactory
	def targetLookup: TargetUrlLookup
	def assExtractorTry: Try[AssertionExtractor]

	implicit val system: ActorSystem

	lazy val idpInfos: Seq[IdpInfo] = idpLib.getInfos.toSeq.sortBy(_.name)

	def samlRoute: Route = pathPrefix("saml"){
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
			path("cpauth"){ getFromResource("icos-cp_sp_meta.xml") } ~
			path("privacyStatement"){ getFromResource("privacyStatement.html")} ~
			path("idps"){ complete(idpInfos) }
		} ~
		post{
			path("SAML2" / "POST"){
				formField('SAMLResponse){ resp =>

					val cookieAndReqIdTry: Try[(HttpCookie, String)] = for(
						extractor <- assExtractorTry;
						response <- Try(Parser.fromBase64[Response](resp));
						(cookie, uid, statements) <- cookieFactory.makeAuthenticationCookie(response, extractor, idpLib)
					) yield {
						for( //side effect: creating new entry in RESTHeart if not present
							givenName <- statements.getSingleValue(samlConfig.attributes.givenName);
							surname <- statements.getSingleValue(samlConfig.attributes.surname)
						) yield restHeart.createUserIfNew(uid, givenName, surname)

						(cookie, response.getInResponseTo)
					}

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