package se.lu.nateko.cp.cpauth.services

import akka.actor.ActorSystem
import se.lu.nateko.cp.cpauth.Envri.Envri
import se.lu.nateko.cp.cpauth.{RestHeartConfig}
import spray.json.{JsObject, JsString}
import CpGeoClient.geoIpInfoFormat
import spray.json._
import se.lu.nateko.cp.cpauth.PostgresConfig
import se.lu.nateko.cp.cpauth.core.DownloadEventInfo
import scala.util.Success
import scala.util.Failure
import se.lu.nateko.cp.cpauth.core.CollectionDownloadInfo
import se.lu.nateko.cp.cpauth.core.DocumentDownloadInfo
import se.lu.nateko.cp.cpauth.core.DataObjDownloadInfo
import se.lu.nateko.cp.cpauth.core.CsvDownloadInfo

class PortalLogger(
	geoClient: CpGeoClient, confRestheart: RestHeartConfig, confPg: PostgresConfig
)(implicit system: ActorSystem){

	import system.dispatcher
	private val restHeartLogClient = new RestHeartLogClient(confRestheart)
	private val pgLogClient = new PostgresClient(confPg)

	def logUsage(entry: JsObject, ip: String)(implicit envri: Envri): Unit =
		logInternally(ip)(logToRestheart(entry, _, confRestheart.usageCollection))

	def logDl(entry: DownloadEventInfo)(implicit envri: Envri): Unit = logInternally(entry.ip){ipinfo =>

		entry match{
			case _: CollectionDownloadInfo | _: DocumentDownloadInfo | _: DataObjDownloadInfo =>
				pgLogClient.logDownload(entry, ipinfo).failed.foreach{err =>
					system.log.error(err, "Could not log download to Postgres")
				}
			case csv: CsvDownloadInfo =>
				logUsage(JsObject("csvDownload" -> csv.toJson), csv.ip)
		}
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

	private def logToRestheart(entry: JsObject, ipinfo: Either[String, GeoIpInfo], coll: String)(implicit envri: Envri): Unit = {
		val geoJs = ipinfo.fold(
			ip => JsObject("ip" -> JsString(ip)),
			geo => geo.toJson.asJsObject
		)
		val logEntry = JsObject(entry.fields ++ geoJs.fields)

		restHeartLogClient.log(logEntry, coll).failed.foreach{err =>
			system.log.error(err, s"Could not log download info ${logEntry.compactPrint} to RestHeart collection $coll")
		}
	}

}
