package se.lu.nateko.cp.cpauth.services

import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import se.lu.nateko.cp.cpauth.Envri.Envri
import se.lu.nateko.cp.cpauth.{CpGeoConfig, RestHeartConfig}
import spray.json.{JsObject, JsString}

import scala.concurrent.duration.DurationInt


class PortalUsageLogger(confRestheart: RestHeartConfig, confGeoIp: CpGeoConfig, emailSender: EmailSender)(implicit system: ActorSystem, mat: Materializer) {

	import system.dispatcher

	private val geoClient = new CpGeoClient(confGeoIp)
	private val logClient = new RestHeartLogClient(confRestheart)

	private val errorLog = Source.queue[Throwable](10, OverflowStrategy.dropTail)
		.map((java.time.Instant.now, _))
		.groupedWithin(1000, 1.hour)
		.filter(_.nonEmpty)
		.toMat(Sink.foreach{errList =>
		  try {

			  val body = errList.map {
				  case (time, err) => s"$time ${err.getMessage}"
			  }.mkString("\n" + "-" * 10 + "\n")

			  emailSender.sendText(Seq("carbon.admin@nateko.lu.se"), "Resolving IP to location failed", body)

		  } catch {
			  case e: Throwable => system.log.error(e, "Error sending error report email")
		  }
		})(Keep.left).run()

	def log(entry: JsObject, ip: String)(implicit envri: Envri): Unit = if (!confRestheart.ipsToIgnore.contains(ip)){
		val geoFut = geoClient.lookup(ip)
		geoFut.failed.foreach(errorLog.offer)

		geoFut.recover{case _: Throwable => JsObject("ip" -> JsString(ip))}
		  .flatMap { js =>
			  val logEntry = JsObject(entry.fields ++ js.fields)
			  logClient.log(logEntry)
		  }

	}
}

