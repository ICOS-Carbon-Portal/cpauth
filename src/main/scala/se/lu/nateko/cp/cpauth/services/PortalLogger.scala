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

class PortalLogger(
	geoClient: CpGeoClient, confRestheart: RestHeartConfig, confPg: PostgresConfig
)(implicit system: ActorSystem){

	import system.dispatcher
	private val restHeartLogClient = new RestHeartLogClient(confRestheart)
	private val pgLogClient = new PostgresClient(confPg)

	def logUsage(entry: JsObject, ip: String)(implicit envri: Envri): Unit =
		logInternally(ip)(logToRestheart(entry, _, confRestheart.usageCollection))

	def logDl(entry: DownloadEventInfo)(implicit envri: Envri): Unit = logInternally(entry.ip){ipinfo =>

		val (itemType, coll) = entry match{
			case _: CollectionDownloadInfo => DownloadItemType.Coll -> confRestheart.collDlsCollection
			case _: DocumentDownloadInfo => DownloadItemType.Doc -> confRestheart.downloadsCollection
			case _: DataObjDownloadInfo => DownloadItemType.Data -> confRestheart.downloadsCollection
		}

		logToRestheart(entry.toJson.asJsObject, ipinfo, coll)

		val pgEvent = DownloadEvent(
			itemType,
			entry.time,
			entry.hashId,
			entry.ip,
			ipinfo.city,
			ipinfo.country_code,
			Some(ipinfo.longitude),
			Some(ipinfo.latitude)
		)
		pgLogClient.logDownload(pgEvent).failed.foreach{err =>
			system.log.error(err, "Could not log download to Postgres")
		}
	}

	private def logInternally(ip: String)(logAction: GeoIpInfo => Unit): Unit = if (!confRestheart.ipsToIgnore.contains(ip)){
		geoClient.lookup(ip).onComplete{
			case Success(ipinfo) => logAction(ipinfo)
			case Failure(err) => system.log.error(err, s"Could not fetch GeoIP information for ip $ip")
		}
	}

	private def logToRestheart(entry: JsObject, ipinfo: GeoIpInfo, coll: String)(implicit envri: Envri): Unit = {
		val js = ipinfo.toJson.asJsObject
		val logEntry = JsObject(entry.fields ++ js.fields)

		restHeartLogClient.log(logEntry, coll).failed.foreach{err =>
			system.log.error(err, s"Could not log download info ${logEntry.compactPrint} to RestHeart collection $coll")
		}
	}

}
