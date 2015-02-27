package se.lu.nateko.cpauth

import spray.json.DefaultJsonProtocol
import spray.httpx.SprayJsonSupport
import se.lu.nateko.cpauth.opensaml.IdpInfo
import se.lu.nateko.cpauth.core.UserInfo

object CpauthJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport{
	implicit val idpInfoFormat = jsonFormat2(IdpInfo)
	
	implicit val userInfoFormat = jsonFormat3(UserInfo)
}
