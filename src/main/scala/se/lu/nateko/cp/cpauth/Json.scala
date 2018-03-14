package se.lu.nateko.cp.cpauth

import se.lu.nateko.cp.cpauth.opensaml.IdpInfo
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.accounts.UserEntry
import spray.json._

object CpauthJsonProtocol extends DefaultJsonProtocol {

	implicit val idpInfoFormat = jsonFormat2(IdpInfo)
	
	implicit val userIdFormat = jsonFormat1(UserId)
	
	implicit val userEntryFormat = jsonFormat2(UserEntry)

	def enumFormat[T <: Enumeration](enum: T) = new RootJsonFormat[enum.Value] {
		def write(v: enum.Value) = JsString(v.toString)

		def read(value: JsValue): enum.Value = value match{
			case JsString(s) =>
				try{
					enum.withName(s)
				}catch{
					case _: NoSuchElementException => deserializationError(
						"Expected one of: " + enum.values.map(_.toString).mkString("'", "', '", "'")
					)
				}
			case _ => deserializationError("Expected a string")
		}
	}

}
