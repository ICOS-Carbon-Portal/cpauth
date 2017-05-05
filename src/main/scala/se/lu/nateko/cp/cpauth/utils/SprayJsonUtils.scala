package se.lu.nateko.cp.cpauth.utils

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsArray

object SprayJsonUtils {

	def ensure[T <: JsValue : Manifest](v: JsValue): Try[T] = v match{
		case t: T => Success(t)
		case _ =>
			val expected = implicitly[Manifest[T]].toString
			val got = v.getClass.getSimpleName
			Failure(new Exception(s"Expected $expected, got $got"))
	}

	def ensureOpt[T <: JsValue : Manifest](v: JsValue): Option[T] = v match{
		case t: T => Some(t)
		case _ => None
	}

	def getField[T <: JsValue : Manifest](o: JsObject, field: String): Try[T] = (o.fields.get(field) match {
		case Some(v) => Success(v)
		case None => Failure(new NoSuchElementException(s"Field $field was not found"))
	}).flatMap(ensure[T])

	def getFieldOpt[T <: JsValue : Manifest](o: JsObject, field: String): Option[T] = o.fields.get(field).flatMap(ensureOpt[T])

	def getStringField(o: JsObject, field: String): Try[String] = getField[JsString](o, field).map(_.value)
	def getStringFieldOpt(o: JsObject, field: String): Option[String] = getFieldOpt[JsString](o, field).map(_.value)

	def getElements[T <: JsValue : Manifest](arr: JsArray): Try[Seq[T]] = {
		Utils.tryseq(arr.elements.map(ensure[T]))
	}

}
