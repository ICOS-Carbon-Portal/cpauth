package se.lu.nateko.cp.cpauth.utils

import scala.collection.convert.WrapAsScala
import java.util.zip.Deflater
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import org.apache.commons.codec.binary.Base64
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import akka.pattern.Patterns.after
import akka.actor.Scheduler
import scala.concurrent.duration.FiniteDuration

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

		def toSafeIterable: Iterable[T] =
			if(list == null)
				Iterable.empty[T]
			else WrapAsScala.iterableAsScalaIterable(list)
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
			case err => after(time, scheduler, ex, Future.failed(err))
		}

}