package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.headers.Host
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteResult.Complete
import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.utils.addSegment
import se.lu.nateko.cp.cpauth.utils.drop

import scala.concurrent.Future

trait RestHeartDirectives extends CpauthDirectives with ProxyDirectives:

	def restheartProxy(user: UserId)(using envri: Envri): Route = ctxt => {
		val req = ctxt.request
		val headers = req.headers.filter {
			case _: Host => false
			case _ => true
		}
		val userUri: Uri = restHeart.config.usersCollUri.addSegment(user.email)
		val oldPathPart = req.uri.path.drop(6) //drop '/db/users/<email>' from the beginning
		val newUri = req.uri
			.withScheme(userUri.scheme)
			.withAuthority(userUri.authority)
			.withPath(userUri.path ++ oldPathPart)

		val creds = restHeart.httpCreds.get(envri)
		def proxy = proxyToUri(req.withHeaders(headers), newUri, creds)
		proxy.flatMap{
			case Complete(resp) if resp.status.intValue == 404 =>
				restHeart.createUserIfNew(user, "", "").flatMap(_ => proxy)
			case rr => Future.successful(rr)
		}
	}
