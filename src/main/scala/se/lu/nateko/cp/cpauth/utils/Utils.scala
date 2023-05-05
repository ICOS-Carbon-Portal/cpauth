package se.lu.nateko.cp.cpauth.utils

import java.util.zip.Deflater
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import org.apache.commons.codec.binary.Base64
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import akka.pattern.Patterns.after
import akka.actor.Scheduler
import scala.concurrent.duration.FiniteDuration
import scala.util.Try
import scala.collection.mutable.Buffer
import scala.util.Failure
import scala.util.Success
import java.net.URI
import akka.http.scaladsl.model.Uri

object Utils {

	import org.slf4j.LoggerFactory
	import ch.qos.logback.classic.{Level, Logger}
	
	def setRootLoggingLevel(level: Level): Unit =
		LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
			.asInstanceOf[Logger]
			.setLevel(level)

	def setRootLoggingLevelToInfo(): Unit = setRootLoggingLevel(Level.INFO)

	def disableLogging(): () => Unit = {
		val originalLevel = getRootLoggingLevel
		setRootLoggingLevel(Level.OFF)
		() => setRootLoggingLevel(originalLevel)
	}

	def getRootLoggingLevel: Level =
		LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
			.asInstanceOf[Logger]
			.getLevel

	implicit class SafeJavaCollectionWrapper[T](val list: java.util.Collection[T]) extends AnyVal {
		import scala.jdk.CollectionConverters._

		def toSafeIterable: Iterable[T] =
			if(list == null)
				Iterable.empty[T]
			else list.asScala
	}

	def compressAndBase64ForSaml(s: String): String = {
		val deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
		val byteStream = new ByteArrayOutputStream()

		val utf8 = java.nio.charset.StandardCharsets.UTF_8
		val defstr = new DeflaterOutputStream(byteStream, deflater)
		defstr.write(s.getBytes(utf8))
		defstr.close()
		byteStream.close()

		new String(new Base64().encode(byteStream.toByteArray), utf8)
	}

	def slowFailureDown[T](future: Future[T], time: FiniteDuration)(implicit ex: ExecutionContext, scheduler: Scheduler): Future[T] =
		future.recoverWith{
			case err => after(time, scheduler, ex, () => Future.failed(err))
		}

	def tryseq[T](tries: Iterable[Try[T]]): Try[Seq[T]] = {
		val successes = Buffer.empty[T]
		var error: Option[Failure[Seq[T]]] = None
		val iter = tries.iterator

		while(error.isEmpty && iter.hasNext){
			iter.next() match{
				case Success(t) => successes += t
				case Failure(err) => error = Some(Failure(err))
			}
		}
		error.getOrElse(Success(successes.toVector))
	}

	implicit class CrasheableTry[T](val inner: Try[T]) extends AnyVal{
		import scala.concurrent.Await
		import scala.concurrent.duration.Duration
		def getOrCrash(message: String)(implicit system: akka.actor.ActorSystem): T = inner.recover{
			case err =>
				system.log.error(err, message)
				Await.ready(system.terminate(), Duration.Inf)
				sys.exit(1)
		}.get
	}
}

given uriJavaToAkka: Conversion[URI, Uri] = uri => Uri(uri.toString)

extension (uri: Uri)
	def appendPathSegment(segment: String): Uri = uri.withPath(uri.path / segment)
