package se.lu.nateko.cp.cpauth.services

import akka.actor.ActorSystem
import se.lu.nateko.cp.cpauth.Envri.Envri
import se.lu.nateko.cp.cpauth.{RestHeartConfig}
import spray.json.{JsObject, JsString}
import CpGeoClient.geoIpInfoFormat
import spray.json._
import se.lu.nateko.cp.cpauth.PostgresConfig

trait PortalUsageLogger{
	def log(entry: JsObject, ip: String)(implicit envri: Envri): Unit
}

trait PortalDownloadsLogger{
	def log(entry: JsObject)(implicit envri: Envri): Unit
}

class PortalLoggerFactory(geoClient: CpGeoClient, confRestheart: RestHeartConfig, confPg: PostgresConfig)(implicit system: ActorSystem){
	import system.dispatcher
	private trait PortalLogger{
		protected val restHeartLogClient: RestHeartLogClient
		protected val pgLogClient: PostgresClient

		protected def logInternal(entry: JsObject, ip: String, coll: Option[String])(implicit envri: Envri): Unit = if (!confRestheart.ipsToIgnore.contains(ip)){
			geoClient.lookup(ip)
				.flatMap { ipinfo =>
					val js = ipinfo.toJson.asJsObject

					if (coll.isDefined && entry.fields.contains("_id")){
						val itemType = coll.get match{
							case "dobjdls" => DownloadItemType.Data
							case "docdls" => DownloadItemType.Doc
							case "colldls" => DownloadItemType.Coll
							case _ => deserializationError(s"Unsupported collection (${coll.get}) provided")
						}

						val oid = entry.fields("_id").asJsObject.fields("$oid").asInstanceOf[JsString].value
						val secondsSinceEpochHex = oid.substring(0, 8).toString()
						val secondsSinceEpoch = java.lang.Long.parseLong(secondsSinceEpochHex, 16)
						val ts = java.time.Instant.ofEpochSecond(secondsSinceEpoch)

						val hashId = entry.fields("dobj").asJsObject.fields("pid").asInstanceOf[JsString].value.split("/").last

						val pgEvent = DownloadEvent(itemType, ts, hashId, ip, ipinfo.city, ipinfo.country_code, Some(ipinfo.longitude), Some(ipinfo.latitude))
						pgLogClient.logDownload(pgEvent)
					}

					val logEntry = JsObject(entry.fields ++ js.fields)
					restHeartLogClient.log(logEntry)
				}
		}
	}

	def usage: PortalUsageLogger = new PortalLogger with PortalUsageLogger{
		override val restHeartLogClient = new RestHeartLogClient(confRestheart, confRestheart.usageCollection)
		def log(entry: JsObject, ip: String)(implicit envri: Envri): Unit = logInternal(entry, ip, None)
		override val pgLogClient = new PostgresClient(confPg)
	}

	def objectDownloads = downloadsLogger(confRestheart.downloadsCollection)
	def collDownloads = downloadsLogger(confRestheart.collDlsCollection)

	private def downloadsLogger(coll: String): PortalDownloadsLogger = new PortalLogger with PortalDownloadsLogger{
		override val restHeartLogClient = new RestHeartLogClient(confRestheart, coll)
		override val pgLogClient = new PostgresClient(confPg)
		
		def log(entry: JsObject)(implicit envri: Envri): Unit = entry.fields.get("ip") match{
			case Some(JsString(ip)) =>
				logInternal(entry, ip, Some(coll))
			case _ =>
				system.log.error("No 'ip' string property found in the js object, can not log data object download")
		}
	}
}
