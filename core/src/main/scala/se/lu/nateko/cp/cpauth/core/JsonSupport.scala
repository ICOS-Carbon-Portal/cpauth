package se.lu.nateko.cp.cpauth.core

import spray.json.*
import eu.icoscp.envri.Envri

object JsonSupport extends DefaultJsonProtocol:

	given RootJsonFormat[PublicAuthConfig] = jsonFormat4(PublicAuthConfig.apply)
	given RootJsonFormat[Envri] = enumFormat(Envri.valueOf, Envri.values)

	def enumFormat[T <: reflect.Enum](valueOf: String => T, values: Array[T]) = new RootJsonFormat[T]:
		def write(v: T) = JsString(v.toString)

		def read(value: JsValue): T = value match
			case JsString(s) =>
				try valueOf(s)
				catch case _: IllegalArgumentException =>
					deserializationError("Expected one of: " + values.mkString("'", "', '", "'"))
			case _ => deserializationError("Expected a JSON string")
