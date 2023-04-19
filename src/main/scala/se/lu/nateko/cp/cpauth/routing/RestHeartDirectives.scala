package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route

trait RestHeartDirectives extends CpauthDirectives with ProxyDirectives{

	def restheartProxy(baseUri: Uri): Route = ctxt => {
		val req = ctxt.request
		val newUri = req.uri.withScheme(baseUri.scheme).withAuthority(baseUri.authority)
		proxyToUri(req, newUri)
	}
}
