package se.lu.nateko.cpauth

import spray.routing.Directives
import spray.http.HttpHeaders
import spray.http.Uri
import spray.http.StatusCodes
import akka.actor.ActorSystem
import akka.util.Timeout
import spray.http.HttpHeader
import scala.concurrent.ExecutionContext
import se.lu.nateko.cpauth.core.UrlsConfig

trait DrupalRouting extends Directives with CpauthDirectives with ProxyDirectives{

	def urlsConfig: UrlsConfig
	implicit val system: ActorSystem
	implicit val timeout: Timeout

	val drupalRoute = get{
		headerValue{
			case HttpHeaders.Host(host, 0) => urlsConfig.drupalProxying.get(host)
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
					Uri(urlsConfig.serviceUrl + urlsConfig.loginPath).withQuery(("targetUrl", targetUri.toString)),
					StatusCodes.Found
				)
			})
		}
	}

	val remakeCookies = mapHttpResponseHeaders(_.map(remakeCookie))

	def redirectWhenDone(target: Uri, dropParam: Option[String] = None) =
		respondWithHeader(HttpHeaders.Location(withoutParam(dropParam, target))) &
		respondWithStatus(StatusCodes.Found) &
		remakeCookies

	private def withoutParam(param: Option[String], uri: Uri): Uri = param match{
		case None => uri
		case Some(drop) =>
			val filteredQuery = uri.query.filter{
				case (`drop`, _) => false
				case _ => true
			}
			uri.withQuery(filteredQuery)
	}

	private def remakeCookie(header: HttpHeader): HttpHeader = header match{
		case HttpHeaders.`Set-Cookie`(cookie) =>
			val newCookie = cookie.copy(secure = true, httpOnly = true, expires = None)
			HttpHeaders.`Set-Cookie`(newCookie)
		case x => x
	}
}