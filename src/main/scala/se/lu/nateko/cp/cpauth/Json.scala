package se.lu.nateko.cp.cpauth

import se.lu.nateko.cp.cpauth.opensaml.IdpInfo
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.accounts.UserEntry
import spray.json.*

object CpauthJsonProtocol:
	import DefaultJsonProtocol.*

	given RootJsonFormat[IdpInfo] = jsonFormat2(IdpInfo.apply)
	
	given RootJsonFormat[UserId] = jsonFormat1(UserId.apply)
	
	given RootJsonFormat[UserEntry] = jsonFormat2(UserEntry.apply)
