package se.lu.nateko.cp.cpauth.routing

import se.lu.nateko.cp.cpauth.accounts.RestHeartClient
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route

trait RestHeartDirectives extends CpauthDirectives with ProxyDirectives{

	def restHeart: RestHeartClient

	def restheartProxy: Route = ctxt => {
		val baseUri = Uri(restHeart.config.baseUri)
		val req = ctxt.request
		val newUri = req.uri.withScheme(baseUri.scheme).withAuthority(baseUri.authority)
		proxyToUri(req, newUri)
	}
}
