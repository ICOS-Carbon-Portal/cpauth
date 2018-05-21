package se.lu.nateko.cp.cpauth.services

import akka.actor.ActorSystem
import se.lu.nateko.cp.cpauth.Envri.Envri
import se.lu.nateko.cp.cpauth.{RestHeartConfig}
import spray.json.{JsObject, JsString}


abstract class PortalLogger(geoClient: CpGeoClient, confRestheart: RestHeartConfig)(implicit system: ActorSystem) {

	import system.dispatcher

	protected val logClient: RestHeartLogClient

	protected def logInternal(entry: JsObject, ip: String)(implicit envri: Envri): Unit = if (!confRestheart.ipsToIgnore.contains(ip)){
		geoClient.lookup(ip).recover{case _: Throwable => JsObject("ip" -> JsString(ip))}
		  .flatMap { js =>
			  val logEntry = JsObject(entry.fields ++ js.fields)
			  logClient.log(logEntry)
		  }

	}
}

class PortalUsageLogger(geoClient: CpGeoClient, confRestheart: RestHeartConfig)(implicit system: ActorSystem) extends PortalLogger(geoClient, confRestheart){
	override val logClient = new PortalUseLogClient(confRestheart)
	def log(entry: JsObject, ip: String)(implicit envri: Envri): Unit = logInternal(entry, ip)
}

class ObjectDownloadsLogger(geoClient: CpGeoClient, confRestheart: RestHeartConfig)(implicit system: ActorSystem) extends PortalLogger(geoClient, confRestheart){

	override val logClient = new ObjectDownloadsLogClient(confRestheart)

	def log(entry: JsObject)(implicit envri: Envri): Unit = entry.fields.get("ip") match{
		case Some(JsString(ip)) =>
			logInternal(entry, ip)
		case _ =>
			system.log.error("No 'ip' string property found in the js object, can not log data object download")
	}
}
