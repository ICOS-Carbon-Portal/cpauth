package se.lu.nateko.cpauth

import scala.util.control.NoStackTrace
import scala.concurrent.Future

sealed class CpauthException(message: String) extends Exception(message)

case object AuthenticationFailedException extends CpauthException("Incorrect user name or password") with NoStackTrace

object Exceptions{

	def failedFuture[T](msg: String): Future[T] = Future.failed(new CpauthException(msg) with NoStackTrace)

}