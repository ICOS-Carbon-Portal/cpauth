package se.lu.nateko.cp.cpauth.core

import spray.json.*
import eu.icoscp.envri.Envri

object JsonSupport extends DefaultJsonProtocol:

	given RootJsonFormat[PublicAuthConfig] = jsonFormat4(PublicAuthConfig.apply)

	given JsonFormat[Envri] with
		override def read(json: JsValue): Envri = json match
			case JsString(str) =>
				try Envri.valueOf(str)
				catch case _ => deserializationError(s"Unknown ENVRI $str")
			case _ => deserializationError(s"Expected JSON string, got $json")

		override def write(e: Envri): JsValue = JsString(e.toString)
