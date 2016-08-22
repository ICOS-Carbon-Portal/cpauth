package se.lu.nateko.cp.cpauth

import spray.json.DefaultJsonProtocol
import se.lu.nateko.cp.cpauth.opensaml.IdpInfo
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.accounts.UserEntry

object CpauthJsonProtocol extends DefaultJsonProtocol {

	implicit val idpInfoFormat = jsonFormat2(IdpInfo)
	
	implicit val userIdFormat = jsonFormat1(UserId)
	
	implicit val userEntryFormat = jsonFormat2(UserEntry)
}
