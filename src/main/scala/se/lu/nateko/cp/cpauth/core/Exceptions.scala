package se.lu.nateko.cp.cpauth.core

import scala.util.control.NoStackTrace
import scala.concurrent.Future
import scala.util.Try
import scala.util.Failure

class CpauthException(message: String) extends Exception(message)

case object AuthenticationFailedException extends CpauthException("Incorrect user name or password") with NoStackTrace

object Exceptions{

	def failedFuture[T](msg: String): Future[T] = Future.failed(new CpauthException(msg) with NoStackTrace)

	def failure[T](msg: String): Try[T] = Failure(new CpauthException(msg) with NoStackTrace)

}