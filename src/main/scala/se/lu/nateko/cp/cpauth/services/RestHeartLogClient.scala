package se.lu.nateko.cp.cpauth.services

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.RestHeartConfig
import spray.json.JsObject

import scala.concurrent.Future

class RestHeartLogClient(conf: RestHeartConfig)(using system: ActorSystem):

	import system.dispatcher
	private val http = Http()

	def logPortalUsage(logEntry: JsObject)(using Envri) =
		log(logEntry, conf.portalUsageCollUri)

	private def log(logEntry: JsObject, restheartLogUri: Uri)(using Envri): Future[Done] =

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
