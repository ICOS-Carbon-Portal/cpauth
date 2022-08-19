package se.lu.nateko.cp.cpauth

import se.lu.nateko.cp.cpauth.opensaml.IdpInfo
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.accounts.UserEntry
import spray.json.*

object CpauthJsonProtocol {
	import DefaultJsonProtocol.*

	given RootJsonFormat[IdpInfo] = jsonFormat2(IdpInfo.apply)
	
	given RootJsonFormat[UserId] = jsonFormat1(UserId.apply)
	
	given RootJsonFormat[UserEntry] = jsonFormat2(UserEntry.apply)

	def enumFormat[T <: Enumeration](e: T) = new RootJsonFormat[e.Value] {
		def write(v: e.Value) = JsString(v.toString)

		def read(value: JsValue): e.Value = value match{
			case JsString(s) =>
				try{
					e.withName(s)
				}catch{
					case _: NoSuchElementException => deserializationError(
						"Expected one of: " + e.values.map(_.toString).mkString("'", "', '", "'")
					)
				}
			case _ => deserializationError("Expected a string")
		}
	}

}
