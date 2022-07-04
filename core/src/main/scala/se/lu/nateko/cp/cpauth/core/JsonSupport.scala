package se.lu.nateko.cp.cpauth.core

import spray.json.DefaultJsonProtocol

object JsonSupport extends DefaultJsonProtocol{

	implicit val pubAuthConfigFormat = jsonFormat4(PublicAuthConfig)

}
