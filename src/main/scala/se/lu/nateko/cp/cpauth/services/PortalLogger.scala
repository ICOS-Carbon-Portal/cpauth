package se.lu.nateko.cp.cpauth.services

import akka.actor.ActorSystem
import se.lu.nateko.cp.cpauth.Envri.Envri
import se.lu.nateko.cp.cpauth.{RestHeartConfig}
import spray.json.{JsObject, JsString}
import CpGeoClient.geoIpInfoFormat
import spray.json._

trait PortalUsageLogger{
	def log(entry: JsObject, ip: String)(implicit envri: Envri): Unit
}

trait PortalDownloadsLogger{
	def log(entry: JsObject)(implicit envri: Envri): Unit
}

class PortalLoggerFactory(geoClient: CpGeoClient, confRestheart: RestHeartConfig)(implicit system: ActorSystem){
	import system.dispatcher
	private trait PortalLogger{
		protected val logClient: RestHeartLogClient

		protected def logInternal(entry: JsObject, ip: String)(implicit envri: Envri): Unit = if (!confRestheart.ipsToIgnore.contains(ip)){
			geoClient.lookup(ip)
				.map(ipinfo => ipinfo.toJson.asJsObject)
				.recover{case _: Throwable => JsObject("ip" -> JsString(ip))}
				.flatMap { js =>
					val logEntry = JsObject(entry.fields ++ js.fields)
					logClient.log(logEntry)
				}
		}
	}

	def usage: PortalUsageLogger = new PortalLogger with PortalUsageLogger{
		override val logClient = new RestHeartLogClient(confRestheart, confRestheart.usageCollection)
		def log(entry: JsObject, ip: String)(implicit envri: Envri): Unit = logInternal(entry, ip)
	}

	def objectDownloads = downloadsLogger(confRestheart.downloadsCollection)
	def collDownloads = downloadsLogger(confRestheart.collDlsCollection)

	private def downloadsLogger(coll: String): PortalDownloadsLogger = new PortalLogger with PortalDownloadsLogger{
		override val logClient = new RestHeartLogClient(confRestheart, coll)

		def log(entry: JsObject)(implicit envri: Envri): Unit = entry.fields.get("ip") match{
			case Some(JsString(ip)) =>
				logInternal(entry, ip)
			case _ =>
				system.log.error("No 'ip' string property found in the js object, can not log data object download")
		}
	}
}
