package eu.icoscp.geoipclient

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.Materializer
import akka.stream.OverflowStrategy
import akka.stream.QueueOfferResult
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import se.lu.nateko.cp.cpauth.core.EmailSender

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt


class ErrorEmailer(to: String, subject: String, emailSender: EmailSender, log: LoggingAdapter)(using Materializer):

	private val errorLog = Source.queue[Throwable](10, OverflowStrategy.dropTail)
		.map((java.time.Instant.now, _))
		.groupedWithin(2000, 2.hours)
		.filter(_.nonEmpty)
		.toMat(Sink.foreach{errList =>
			try
				val body = errList.map {
					case (time, err) => s"$time ${err.getMessage}"
				}.mkString("\n" + "-" * 10 + "\n")

				emailSender.sendText(Seq(to), subject, body)
			catch case e: Throwable => log.error(e, "Error sending error report email")
		})(Keep.left).run()

	def enqueue(error: Throwable): Future[QueueOfferResult] = errorLog.offer(error)
