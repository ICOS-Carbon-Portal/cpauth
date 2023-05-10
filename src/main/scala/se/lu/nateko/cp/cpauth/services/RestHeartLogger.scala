package se.lu.nateko.cp.cpauth.services

import akka.actor.ActorSystem
import se.lu.nateko.cp.cpauth.RestHeartConfig
import spray.json.JsObject
import eu.icoscp.envri.Envri
import spray.json.JsString
import se.lu.nateko.cp.geoipclient.GeoIpInfo
import se.lu.nateko.cp.geoipclient.CpGeoClient
import spray.json.enrichAny
import se.lu.nateko.cp.geoipclient.CpGeoClient.given_RootJsonFormat_GeoIpInfo
import scala.util.Failure
import scala.util.Success

class RestHeartLogger(geoClient: CpGeoClient, confRestheart: RestHeartConfig)(using system: ActorSystem):

	import system.dispatcher
	private val restHeartLogClient = new RestHeartLogClient(confRestheart)

	def logUsage(entry: JsObject, ip: String)(using Envri): Unit =
		logInternally(ip)(logUsageToRestheart(entry, _))

	private def logInternally(ip: String)(logAction: Either[String, GeoIpInfo] => Unit): Unit = if (!confRestheart.ipsToIgnore.contains(ip)){
		geoClient.lookup(ip).onComplete{
			case Success(ipinfo) =>
				logAction(Right(ipinfo))
			case Failure(err) =>
				logAction(Left(ip))
				system.log.error(err, s"Could not fetch GeoIP information for ip $ip")
		}
	}

	private def logUsageToRestheart(entry: JsObject, ipinfo: Either[String, GeoIpInfo])(using Envri): Unit =
		val geoJs = ipinfo.fold(
			ip => JsObject("ip" -> JsString(ip)),
			geo => geo.toJson.asJsObject
		)
		val logEntry = JsObject(entry.fields ++ geoJs.fields)

		restHeartLogClient.logPortalUsage(logEntry).failed.foreach{err =>
			system.log.error(err, s"Could not log portal usage info ${logEntry.compactPrint} to RestHeart")
		}

end RestHeartLogger
