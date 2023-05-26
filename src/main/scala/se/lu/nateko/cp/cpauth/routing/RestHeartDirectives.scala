package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.headers.Host

trait RestHeartDirectives extends CpauthDirectives with ProxyDirectives{

	def restheartProxy(baseUri: Uri, creds: Option[BasicHttpCredentials]): Route = ctxt => {
		val req = ctxt.request
		val headers = req.headers.filter {
			case _: Host => false
			case _ => true
		}
		val newUri = req.uri.withScheme(baseUri.scheme).withAuthority(baseUri.authority)
		proxyToUri(req.withHeaders(headers), newUri, creds)
	}
}
