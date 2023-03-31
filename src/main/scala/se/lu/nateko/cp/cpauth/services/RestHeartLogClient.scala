package se.lu.nateko.cp.cpauth.services

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model._
import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.RestHeartConfig
import spray.json.JsObject
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Future

class RestHeartLogClient(conf: RestHeartConfig)(implicit system: ActorSystem) {

	import system.dispatcher

	val baseUrl = Uri(conf.baseUri)
	private val http = Http()

	def log(logEntry: JsObject, logCollection: String)(implicit envri: Envri): Future[Done] = {

		val restheartLogUri = baseUrl.withPath(baseUrl.path / conf.dbName / logCollection)

		val requestFut = Marshal(logEntry).to[RequestEntity].map{ent =>
			HttpRequest(
				uri = restheartLogUri,
				method = HttpMethods.POST,
				headers = Accept(MediaTypes.`application/json`) :: Nil,
				entity = ent
			)

		}

		requestFut.flatMap(http.singleRequest(_)).flatMap{resp =>
			if(resp.status.isSuccess)
				Future.successful(Done)
			else
				Future.failed(new Exception(resp.status.defaultMessage))
		}
	}
}
