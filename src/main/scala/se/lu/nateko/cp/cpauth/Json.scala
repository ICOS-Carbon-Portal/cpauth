package se.lu.nateko.cp.cpauth

import spray.json.DefaultJsonProtocol
import se.lu.nateko.cp.cpauth.opensaml.IdpInfo
import se.lu.nateko.cp.cpauth.core.UserInfo
import se.lu.nateko.cp.cpauth.accounts.UserEntry

object CpauthJsonProtocol extends DefaultJsonProtocol {

	implicit val idpInfoFormat = jsonFormat2(IdpInfo)
	
	implicit val userInfoFormat = jsonFormat3(UserInfo)
	
	implicit val userEntryFormat = jsonFormat2(UserEntry)
}
