package se.lu.nateko.cp.cpauth.services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import se.lu.nateko.cp.cpauth.CpGeoConfig
import spray.json.{ JsNumber, JsObject, JsString, JsValue}

import scala.util.control.NoStackTrace

class CpGeoClient(conf: CpGeoConfig)(implicit system: ActorSystem) {
	import CpGeoClient.{GeoError, QuotaError}

	implicit val materializer = ActorMaterializer()
	import system.dispatcher

	private val baseUrl = Uri(conf.baseUri)

	def lookup(ip: String): Future[JsObject] =
		lookup(ip, Some(conf.maxAgeDays)).recoverWith{
			case _: QuotaError => lookup(ip, None)
		}

	private def lookup(ip: String, maxAge: Option[Int]): Future[JsObject] = {
		val ipPath = baseUrl.path / "ip" / ip

		val path = maxAge.fold(ipPath)(maxDays => ipPath / maxDays.toString)
		Http().singleRequest(
			HttpRequest(uri = baseUrl.withPath(path))
		).flatMap {resp =>
			if(resp.status == StatusCodes.OK)
				Unmarshal(resp).to[JsValue].map(_.asJsObject)
			else
				throw new GeoError(resp.status.defaultMessage)
		}.map{js =>
			js.fields.get("error") match{
				case Some(JsString(msg)) =>
					throw new GeoError(msg)
				case Some(JsObject(fields)) if fields.get("code").contains(JsNumber(104)) =>
					throw new QuotaError
				case Some(errJson) =>
					throw new GeoError(errJson.prettyPrint)
				case None =>
					js
			}

		}
	}

}

object CpGeoClient{

	class GeoError(msg: String) extends Exception(msg) with NoStackTrace
	class QuotaError extends GeoError("Geo IP provider usage quota has been exceeded")

	//private implicit val system = ActorSystem("cp_geo_test")
	//def stop = system.terminate()
	//val test = new CpGeoClient(CpGeoConfig("http://127.0.0.1:5000/ip", 10))
}
