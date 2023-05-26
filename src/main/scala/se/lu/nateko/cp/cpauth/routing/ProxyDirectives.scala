package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.HttpMessage
import akka.http.scaladsl.model.HttpProtocols
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.model.HttpRequest
import scala.concurrent.Future
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.headers.Host

trait ProxyDirectives { this: CpauthDirectives =>

	val http: HttpExt

	import ProxyDirectives._

	def proxyTo(host: Uri.Host, port: Int, path: Uri.Path, query: (String, String)*): Route = ctxt => {

		val req = ctxt.request
		val finalPath = if(path.isEmpty) Uri.Path./ else path
		val newQuery = mergeQueries(query, req.uri.query())
		val newUri = Uri./.withScheme("http").withHost(host).withPort(port).withPath(finalPath).withQuery(newQuery)
		proxyToUri(req, newUri, None)
	}

	protected def proxyToUri(req: HttpRequest, newUri: Uri, creds: Option[BasicHttpCredentials]): Future[RouteResult] = {
		val newReq = req.withUri(newUri).withProtocol(HttpProtocols.`HTTP/1.1`).withoutRedundantHeaders
		val credReq = creds.fold(newReq)(newReq.addCredentials(_))
		http.singleRequest(credReq).map(response => Complete(response.withoutRedundantHeaders))
	}
}

object ProxyDirectives{

	import akka.http.scaladsl.model.headers._

	private val headersBlackList: Set[String] = {
		Set(`Content-Type`, `Content-Length`, Server, Date, `Transfer-Encoding`, `Timeout-Access`, `Access-Control-Allow-Origin`)
			.map(_.lowercaseName)
	}

	def mergeQueries(highPrio: Seq[(String, String)], old: Uri.Query) = Uri.Query{
		old.foldLeft(highPrio.toMap){
			case (map, pair @ (key, _)) => if(map.contains(key)) map else map + pair
		}
	}

	implicit class HeaderManipHttpMessage[T <: HttpMessage](val msg: T) extends AnyVal{

		def withoutRedundantHeaders: msg.Self = {
			val headers = msg.headers.filter(header => !headersBlackList.contains(header.lowercaseName))
			msg.withHeaders(headers)
		}

	}

}
