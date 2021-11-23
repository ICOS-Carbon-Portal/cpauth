package se.lu.nateko.cp.cpauth.services

import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt


class ErrorEmailer(to: String, subject: String, emailSender: EmailSender)(implicit system: ActorSystem, mat: Materializer) {

	private val errorLog = Source.queue[Throwable](10, OverflowStrategy.dropTail)
	  .map((java.time.Instant.now, _))
	  .groupedWithin(2000, 2.hours)
	  .filter(_.nonEmpty)
	  .toMat(Sink.foreach{errList =>
		  try {

			  val body = errList.map {
				  case (time, err) => s"$time ${err.getMessage}"
			  }.mkString("\n" + "-" * 10 + "\n")

			  emailSender.sendText(Seq(to), subject, body)

		  } catch {
			  case e: Throwable => system.log.error(e, "Error sending error report email")
		  }
	  })(Keep.left).run()

	def enqueue(error: Throwable): Future[QueueOfferResult] = errorLog.offer(error)

}
