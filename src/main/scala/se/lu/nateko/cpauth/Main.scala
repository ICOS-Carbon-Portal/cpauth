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
import spray.http.HttpCookie
import spray.http.HttpHeaders
import spray.http.HttpEntity
import spray.http.HttpResponse
import spray.http.MediaTypes
import spray.http.StatusCodes
import spray.http.Uri
import spray.routing.SimpleRoutingApp
import se.lu.nateko.cpauth.accounts.Users
import se.lu.nateko.cpauth.core.UserInfo


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
								uriStr <- target if(uriStr != null && uriStr.trim.length > 0);
								uri <- Try(Uri(uriStr)).toOption
							) targetLookup.memorize(reqId, uri)

							redirect(reqUri, StatusCodes.Found)
						}
					}
				} ~ complete(
					HttpResponse(StatusCodes.BadRequest, "Identity provider has not been specified!")
				)
			} ~
			path("logout"){
				deleteCookie(config.authCookieName, config.authDomain, "/"){complete(StatusCodes.OK)}
			} ~
			path("saml" / "cpauth"){ complete(metadataXml) } ~
			path("saml" / "idps"){ complete(idpInfos) } ~
			path("whoami"){
				user(uinfo => complete(uinfo)) ~ complete(StatusCodes.Unauthorized)
			} ~
			headerValue{
				case HttpHeaders.Host(host, 0) => config.drupalProxying.get(host)
				case _ => None
			}{ drupalProxy =>
				extract(_.request.uri)(originalUri => {
					val targetUri = originalUri.withScheme("https")
					user{ uinfo =>
						redirectWhenDone(target = targetUri, dropParam = Some("login")){
							proxyTo(
								Uri.IPv4Host(drupalProxy.ipv4Host),
								drupalProxy.port,
								("givenName", uinfo.givenName),
								("surname", uinfo.surname),
								("mail", uinfo.mail)
							)
						}
					} ~
					redirect(
						Uri(config.serviceUrl + config.loginPath).withQuery(("targetUrl", targetUri.toString)),
						StatusCodes.Found
					)
				})
			}
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
							redirect(target.getOrElse(Uri("/home/")), StatusCodes.Found)
						}
					}
				}
			} ~
			path("password" / "login"){
				formFields('mail, 'password)((mail, password) =>

					onComplete(Users.authenticateUser(mail, password)){ uinfoTry =>

						uinfoTry.flatMap(cookieFactory.makeAuthenticationCookie) match{

							case Success(cookie) => setCookie(cookie)(complete(StatusCodes.OK))
	
							case Failure(err) => err match {
								case AuthenticationFailedException => complete(StatusCodes.Forbidden)
								case _ => failWith(err)
							}
						}
					}
				)
			} ~
			path("password" / "account" / "create"){
				user{adminInfo =>
					onComplete(Users.userIsAdmin(adminInfo.mail)){
						case Failure(err) => failWith(err)
						case Success(false) => complete(StatusCodes.Unauthorized)
						case Success(true) => 
							formFields('givenName, 'surname, 'mail, 'password)((givenName, surname, mail, password) =>
								
								onComplete(Users.userExists(mail)) { 
									case Success(true) => complete(StatusCodes.Forbidden)
									case Failure(err) => failWith(err)
									case Success(false) =>
										val uinfo = UserInfo(givenName, surname, mail)
										onComplete(Users.addUser(uinfo, password, false)){
											case Success(()) => complete(StatusCodes.OK)
											case Failure(err) => failWith(err)
										}
								}
							)
					}
				}
			}
		}

	}

}


