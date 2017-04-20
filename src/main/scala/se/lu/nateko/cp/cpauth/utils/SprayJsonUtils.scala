package se.lu.nateko.cp.cpauth.utils

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue

object SprayJsonUtils {

	def getStringField(o: JsObject, field: String): Try[String] = o.fields.get(field) match {
		case Some(JsString(s)) => Success(s)
		case None => Failure(new NoSuchElementException(s"Field $field was not found"))
		case _ => Failure(new NoSuchElementException(s"Field $field was not a string field"))
	}

	def ensureObject(jsv: JsValue) : Try[JsObject] = jsv match {
		case o: JsObject => Success(o)
		case _ => Failure(new Exception("Expected a JSON object, got a JSON value"))
	}

	//TODO Implement
	def getStringArray(o: JsObject, field: String): Try[Seq[String]] = ???

}
