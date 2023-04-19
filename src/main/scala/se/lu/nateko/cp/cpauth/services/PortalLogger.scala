package se.lu.nateko.cp.cpauth.services

import akka.actor.ActorSystem
import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.PostgresConfig
import se.lu.nateko.cp.cpauth.RestHeartConfig
import se.lu.nateko.cp.cpauth.core.*
import spray.json.JsObject
import spray.json.JsString
import spray.json.*

import scala.util.Failure
import scala.util.Success

import CpGeoClient.given

class PortalLogger(
	geoClient: CpGeoClient, confRestheart: RestHeartConfig, confPg: PostgresConfig
)(using system: ActorSystem){

	import system.dispatcher
	private val restHeartLogClient = new RestHeartLogClient(confRestheart)
	private val pgLogClient = new PostgresClient(confPg)

	def logUsage(entry: JsObject, ip: String)(using Envri): Unit =
		logInternally(ip)(logUsageToRestheart(entry, _))

	def logDl(entry: DownloadEventInfo)(using Envri): Unit = logInternally(entry.ip){ipinfo =>

		entry match
			case pgEvent: DlEventForPostgres =>
				pgLogClient.logDownload(pgEvent, ipinfo).failed.foreach{err =>
					system.log.error(err, "Could not log download to Postgres")
				}
			case csv: CsvDownloadInfo =>
				logUsageToRestheart(JsObject("csvDownload" -> csv.toJson), ipinfo)

			case cpb: CpbDownloadInfo =>
				logUsageToRestheart(JsObject("cpbDownload" -> cpb.toJson), ipinfo)
			
			case zip: ZipExtractionInfo =>
				logUsageToRestheart(JsObject("zipExtraction" -> zip.toJson), ipinfo)
	}

	private def logInternally(ip: String)(logAction: Either[String, GeoIpInfo] => Unit): Unit = if (!confRestheart.ipsToIgnore.contains(ip)){
		geoClient.lookup(ip).onComplete{
			case Success(ipinfo) =>
				logAction(Right(ipinfo))
			case Failure(err) =>
				logAction(Left(ip))
				system.log.error(err, s"Could not fetch GeoIP information for ip $ip")
		}
	}

	private def logUsageToRestheart(entry: JsObject, ipinfo: Either[String, GeoIpInfo])(using Envri): Unit = {
		val geoJs = ipinfo.fold(
			ip => JsObject("ip" -> JsString(ip)),
			geo => geo.toJson.asJsObject
		)
		val logEntry = JsObject(entry.fields ++ geoJs.fields)

		restHeartLogClient.logPortalUsage(logEntry).failed.foreach{err =>
			system.log.error(err, s"Could not log portal usage info ${logEntry.compactPrint} to RestHeart")
		}
	}

}
