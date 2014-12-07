package se.lu.nateko.cpauth

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

trait ProxyDirectives extends Directives{

	def proxyTo(host: Uri.Host, port: Int)(implicit actorSys: ActorSystem, timeout: Timeout, ectxt: ExecutionContext): RequestContext => Unit = ctxt => {
		import ProxyDirectives._
		
		val req = ctxt.request
		val newUri = req.uri.withHost(host).withPort(port)
		val newReq = req.copy(uri = newUri).withHost(host, port)

		val relay = onComplete(IO(Http).ask(newReq).mapTo[HttpResponse]) {
			case Success(resp) => ctxt => ctxt.responder ! resp.withoutRedundantHeaders
			case Failure(ex) =>
				val msg = s"An error occurred: ${ex.getMessage}"
				val code = StatusCodes.InternalServerError
				complete{(code, msg)}
		}
		relay(ctxt)
	}
}

object ProxyDirectives{

	private val blackList: Set[String] = {
		import HttpHeaders._
		Set(`Content-Type`, `Content-Length`, Server, Date, `Transfer-Encoding`)
			.map(_.lowercaseName)
	}
	
	implicit class HeaderManipHttpMessage(val msg: HttpMessage) extends AnyVal{

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
