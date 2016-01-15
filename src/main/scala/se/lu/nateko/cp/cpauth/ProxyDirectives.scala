package se.lu.nateko.cp.cpauth

import spray.routing.Directives
import spray.http._
import spray.routing.RequestContext
import spray.can.Http
import akka.io.IO
import akka.pattern.ask
import scala.util.{Success, Failure}
import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import spray.routing.Route
import spray.client.pipelining._
import scala.concurrent.Future

trait ProxyDirectives extends Directives{

	import ProxyDirectives._

	def proxyTo(host: Uri.Host, port: Int, path: Uri.Path, query: (String, String)*)
		(implicit actorSys: ActorSystem, timeout: Timeout, ectxt: ExecutionContext): Route = {

		val pipeline: Future[SendReceive] = for (
			Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup(host.address, port)
		) yield sendReceive(connector)

		extract(c => c.request)(req => {

			val finalPath = if(path.isEmpty) Uri.Path./ else path
			val newQuery = mergeQueries(query, req.uri.query)
			val newUri = Uri.Empty.withPath(finalPath).withQuery(newQuery)
			val newReq = req.copy(uri = newUri, protocol = HttpProtocols.`HTTP/1.1`)

			onSuccess(pipeline.flatMap(_(newReq)).mapTo[HttpResponse]) {
				response => complete(response.withoutRedundantHeaders)
			}
		})
	}
}

object ProxyDirectives{

	private val blackList: Set[String] = {
		import HttpHeaders._
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
