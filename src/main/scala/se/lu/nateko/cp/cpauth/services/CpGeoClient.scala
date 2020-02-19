package se.lu.nateko.cp.cpauth.services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer.matFromSystem

import scala.concurrent.Future
import se.lu.nateko.cp.cpauth.CpGeoConfig
import spray.json.{JsNumber, JsObject, JsString, JsValue}

import scala.util.Failure
import scala.util.control.NoStackTrace

class CpGeoClient(conf: CpGeoConfig, errorEmailer: ErrorEmailer)(implicit system: ActorSystem) {
	import CpGeoClient._

	import system.dispatcher

	private val baseUrl = Uri(conf.baseUri)

	def lookup(ip: String): Future[JsObject] = ipError(ip) match{
		case None =>
			lookup(ip, Some(conf.maxAgeDays)).recoverWith{
				case _: QuotaError => lookup(ip, None)
			}
		case Some(errMsg) =>
			Future.successful(JsObject(
				"ip" -> JsString(ip.trim),
				"ipError" -> JsString(errMsg)
			))
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
				throw new GeoError("Got HTTP error from geoip service: " + resp.status.value)
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
		}.andThen{
			case Failure(err) => errorEmailer.enqueue(err)
		}
	}

}

object CpGeoClient{

	class GeoError(msg: String) extends Exception(msg) with NoStackTrace
	class QuotaError extends GeoError("Geo IP provider usage quota has been exceeded")

	def ipError(ip: String): Option[String] = {
		val trimmed = ip.trim

		if(trimmed.isEmpty) Some("IP address is an empty string") else try{

			val addr = java.net.InetAddress.getByName(trimmed)

			if(addr.isMulticastAddress)
				Some("IP address is a multicast address")
			else if(addr.isSiteLocalAddress)
				Some("IP address is a site-local address")
			else
				None
		} catch{
			case err: Throwable =>
				Some("Bad IP address: " + err.getMessage)
		}
	}

	//private implicit val system = ActorSystem("cp_geo_test")
	//def stop = system.terminate()
	//val test = new CpGeoClient(CpGeoConfig("http://127.0.0.1:5000/ip", 10))
}
