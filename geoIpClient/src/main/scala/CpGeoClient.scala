package se.lu.nateko.cp.geoipclient

// import spray.json.*
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.model.*
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer.matFromSystem
import akka.stream.QueueOfferResult
import spray.json.DefaultJsonProtocol
import spray.json.JsValue
import spray.json.RootJsonFormat
import spray.json.RootJsonReader
import spray.json.enrichAny

import scala.concurrent.Future
import scala.util.Failure
import scala.util.control.NoStackTrace

sealed trait GeoIpResponse
case class GeoIpInfo(
	ip: String,
	latitude: Double,
	longitude: Double,
	country_code: Option[String],
	city: Option[String]
) extends GeoIpResponse

case class GeoIpError(error: String) extends GeoIpResponse
case class GeoIpInnerError(msg: String, code: Int) extends GeoIpResponse

class CpGeoClient(conf: CpGeoConfig, errorEmailer: ErrorEmailer)(using system: ActorSystem) extends DefaultJsonProtocol:
	import CpGeoClient._

	given RootJsonFormat[CpGeoConfig] = jsonFormat3(CpGeoConfig.apply)
	given RootJsonFormat[GeoIpError] = jsonFormat1(GeoIpError.apply)
	given RootJsonFormat[GeoIpInnerError] = jsonFormat2(GeoIpInnerError.apply)

	import system.dispatcher

	private val baseUrl = Uri(conf.baseUri)
	private given RootJsonReader[GeoIpResponse] = CpGeoClient.geoIpResponseFormat

	def lookup(ip: String): Future[GeoIpInfo] = ipError(ip) match
		case None =>
			lookup(ip, Some(conf.maxAgeDays)).recoverWith{
				case _: QuotaError => lookup(ip, None)
			}.flatMap{
				case GeoIpInnerError(msg, code) => Future.failed(new GeoError(s"$code : $msg"))
				case GeoIpError(error) => Future.failed(new GeoError(error))
				case info : GeoIpInfo => Future.successful(info)
			}
		case Some(errMsg) =>
			Future.failed(new GeoError(errMsg))


	private def lookup(ip: String, maxAge: Option[Int]): Future[GeoIpResponse] =
		val ipPath = baseUrl.path / "ip" / ip

		val path = maxAge.fold(ipPath)(maxDays => ipPath / maxDays.toString)
		Http().singleRequest(
			request = HttpRequest(uri = baseUrl.withPath(path)),
			settings = ConnectionPoolSettings(system).withMaxConnections(8).withMaxOpenRequests(512)
		).flatMap {resp =>
			if(resp.status == StatusCodes.OK)
				Unmarshal(resp.entity).to[GeoIpResponse]
			else
				Unmarshal(resp.entity).to[String].flatMap{entStr =>
					Future.failed(new GeoError(s"Got HTTP error from geoip service: ${resp.status.value} $entStr"))
				}
		}.flatMap{
			case GeoIpInnerError(_, 104) => Future.failed(new QuotaError)
			case x => Future.successful(x)
		}.andThen{
			case Failure(err) => errorEmailer.enqueue(err)
		}

end CpGeoClient

object CpGeoClient extends DefaultJsonProtocol:

	class GeoError(msg: String) extends Exception(msg) with NoStackTrace
	class QuotaError extends GeoError("Geo IP provider usage quota has been exceeded")

	given RootJsonFormat[GeoIpInfo] = jsonFormat5(GeoIpInfo.apply)
	given RootJsonFormat[GeoIpError] = jsonFormat1(GeoIpError.apply)
	given RootJsonFormat[GeoIpInnerError] = jsonFormat2(GeoIpInnerError.apply)

	given geoIpResponseFormat: RootJsonFormat[GeoIpResponse] with
		override def read(json: JsValue): GeoIpResponse =
			val obj = json.asJsObject("Expected GeoIpResponse to be a JSON object")
			if(obj.fields.contains("ip")) obj.convertTo[GeoIpInfo]
			else if(obj.fields.contains("error")) obj.convertTo[GeoIpError]
			else if(obj.fields.contains("msg")) obj.convertTo[GeoIpInnerError]
			else spray.json.deserializationError(s"unexpected GeoIpResponse: ${json.prettyPrint}")

		override def write(obj: GeoIpResponse): JsValue = obj match
			case e: GeoIpError => e.toJson
			case e: GeoIpInnerError => e.toJson
			case i: GeoIpInfo => i.toJson



	def ipError(ip: String): Option[String] =
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

	//private implicit val system = ActorSystem("cp_geo_test")
	//def stop = system.terminate()
	//val test = new CpGeoClient(CpGeoConfig("http://127.0.0.1:5000/ip", 10))
end CpGeoClient
