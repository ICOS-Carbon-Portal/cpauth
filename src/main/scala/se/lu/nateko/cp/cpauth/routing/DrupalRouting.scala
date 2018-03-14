package se.lu.nateko.cp.cpauth.routing

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.server.Directives._
import se.lu.nateko.cp.cpauth.HttpConfig
import se.lu.nateko.cp.cpauth.Envri

trait DrupalRouting extends CpauthDirectives with ProxyDirectives{

	def httpConfig: HttpConfig
	implicit val system: ActorSystem
	implicit val timeout = Timeout(60.seconds)
	//We only need Drupal proxying for ICOS
	private implicit val drupalEnvri = Envri.ICOS

	val drupalRoute = get{
		headerValue{
			case Host(host, 0) => httpConfig.drupalProxying.get(host.toString)
			case _ => None
		}{ drupalProxy =>
			extract(_.request.uri){originalUri =>
				val targetUri = originalUri.withScheme("https")
				user{ uid =>
					onSuccess(restHeart.getGivenAndSurName(uid)){ case (givenName, surname) =>
						redirectWhenDone(target = targetUri, dropParam = Some("login")){
							proxyTo(
								Uri.IPv4Host(drupalProxy.ipv4Host),
								drupalProxy.port,
								drupalProxy.path.map(Uri.Path(_)).getOrElse(originalUri.path),
								("givenName", givenName),
								("surname", surname),
								("mail", uid.email)
							)
						}
					}
				} ~
				redirect(
					Uri(httpConfig.serviceUrl + httpConfig.loginPath).withQuery(Query("targetUrl" -> targetUri.toString)),
					StatusCodes.Found
				)
			}
		}
	}

	private val dropResponseBody = mapResponseEntity(_ => HttpEntity.Empty)
	private val dropLocation = mapResponseHeaders(_.filterNot(_.is(Location.lowercaseName)))
	private val remakeCookies = mapResponseHeaders(_.map(remakeCookie))

	def redirectWhenDone(target: Uri, dropParam: Option[String] = None) =
		dropResponseBody &
		respondWithHeader(Location(withoutParam(dropParam, target))) &
		dropLocation & //happens before the previous line
		mapResponse(_.copy(status = StatusCodes.Found)) &
		remakeCookies

	private def withoutParam(param: Option[String], uri: Uri): Uri = param match{
		case None => uri
		case Some(drop) =>
			val filteredQuery = uri.query().filter{
				case (param, _) if param.equalsIgnoreCase(drop) => false
				case _ => true
			}
			uri.withQuery(filteredQuery)
	}

	private def remakeCookie(header: HttpHeader): HttpHeader = header match{
		case `Set-Cookie`(cookie) =>
			val newCookie = cookie.copy(secure = true, httpOnly = true, expires = None)
			`Set-Cookie`(newCookie)
		case x => x
	}
}