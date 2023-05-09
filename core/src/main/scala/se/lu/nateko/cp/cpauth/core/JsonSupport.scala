package se.lu.nateko.cp.cpauth.core

import eu.icoscp.envri.Envri
import spray.json.*

import java.net.URI
import se.lu.nateko.cp.cpauth.core.{PublicAuthConfig, EmailConfig, CoreConfig}

object JsonSupport extends DefaultJsonProtocol:

	given RootJsonFormat[PublicAuthConfig] = jsonFormat4(PublicAuthConfig.apply)
	given RootJsonFormat[Envri] = enumFormat(Envri.valueOf, Envri.values)
	given RootJsonFormat[EmailConfig] = jsonFormat5(EmailConfig.apply)
	given RootJsonFormat[CoreConfig] = jsonFormat1(CoreConfig.apply)

	def enumFormat[T <: reflect.Enum](valueOf: String => T, values: Array[T]) = new RootJsonFormat[T]:
		def write(v: T) = JsString(v.toString)

		def read(value: JsValue): T = value match
			case JsString(s) =>
				try valueOf(s)
				catch case _: IllegalArgumentException =>
					deserializationError("Expected one of: " + values.mkString("'", "', '", "'"))
			case _ => deserializationError("Expected a JSON string")

	given RootJsonFormat[URI] with
		def write(uri: URI): JsValue = JsString(uri.toString)

		def read(value: JsValue): URI = value match
			case JsString(uri) =>
				try new URI(uri)
				catch case err: Throwable =>
					deserializationError(s"Could not parse URI from $uri", err)

			case _ => deserializationError("URI string expected")
