package se.lu.nateko.cp.cpauth

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.model.HttpProtocols
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMessage
import akka.http.scaladsl.HttpExt

trait ProxyDirectives { this: CpauthDirectives =>

	val http: HttpExt

	import ProxyDirectives._

	def proxyTo(host: Uri.Host, port: Int, path: Uri.Path, query: (String, String)*): Route = ctxt => {

		val req = ctxt.request
		val finalPath = if(path.isEmpty) Uri.Path./ else path
		val newQuery = mergeQueries(query, req.uri.query())
		val newUri = req.uri.withPath(finalPath).withQuery(newQuery)
		val newReq = req.copy(uri = newUri, protocol = HttpProtocols.`HTTP/1.1`)

		http.singleRequest(newReq).map(response => Complete(response.withoutRedundantHeaders))
	}
}

object ProxyDirectives{

	private val blackList: Set[String] = {
		import headers._
		Set(`Content-Type`, `Content-Length`, Server, Date, `Transfer-Encoding`)
			.map(_.lowercaseName)
	}

	def mergeQueries(highPrio: Seq[(String, String)], old: Uri.Query) = Uri.Query{
		old.foldLeft(highPrio.toMap){
			case (map, pair @ (key, _)) => if(map.contains(key)) map else map + pair
		}
	}

	implicit class HeaderManipHttpMessage[T <: HttpMessage](val msg: T) extends AnyVal{

		def withoutRedundantHeaders: msg.Self = {
			val headers = msg.headers.filter(header => !blackList.contains(header.lowercaseName))
			msg.withHeaders(headers)
		}

	}

}
