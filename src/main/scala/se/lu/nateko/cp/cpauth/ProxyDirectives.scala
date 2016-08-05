package se.lu.nateko.cp.cpauth

import scala.util.{Success, Failure}
import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.HttpProtocols
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMessage

trait ProxyDirectives { this: CpauthDirectives =>

	import ProxyDirectives._

	def proxyTo(host: Uri.Host, port: Int, path: Uri.Path, query: (String, String)*)
		(implicit actorSys: ActorSystem): Route = {

		extract(c => c.request)(req => {

			val finalPath = if(path.isEmpty) Uri.Path./ else path
			val newQuery = mergeQueries(query, req.uri.query())
			val newUri = Uri.Empty.withPath(finalPath).withQuery(newQuery)
			val newReq = req.copy(uri = newUri, protocol = HttpProtocols.`HTTP/1.1`)

			onSuccess(Http().singleRequest(newReq)) {
				response => complete(response.withoutRedundantHeaders)
			}
		})
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
