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

trait ProxyDirectives extends Directives{

	import ProxyDirectives._

	def proxyTo(host: Uri.Host, port: Int, query: (String, String)*)
		(implicit actorSys: ActorSystem, timeout: Timeout, ectxt: ExecutionContext): Route =

		extract(c => c.request)(req => {

			val newQuery = req.uri.query ++ query
			val newUri = req.uri.withHost(host).withPort(port).withQuery(newQuery :_*)
			val newReq = req.copy(uri = newUri).withHost(host, port)

			onSuccess(IO(Http).ask(newReq).mapTo[HttpResponse]) {
				response => complete(response.withoutRedundantHeaders)
			}
		})
}

object ProxyDirectives{

	private val blackList: Set[String] = {
		import HttpHeaders._
		Set(`Content-Type`, `Content-Length`, Server, Date, `Transfer-Encoding`)
			.map(_.lowercaseName)
	}

	implicit class HeaderManipHttpMessage[T <: HttpMessage](val msg: T) extends AnyVal{

		def withHost(host: Uri.Host, port: Int): msg.Self = {
			val hport = if(port == 80) 0 else port
			val hostHeader = HttpHeaders.Host(host.toString, hport)
			msg.withHeaders(hostHeader)
		}
		
		def withoutRedundantHeaders: msg.Self = {
			val headers = msg.headers.filter(header => !blackList.contains(header.lowercaseName))
			msg.withHeaders(headers)
		}

	}

}
