package se.lu.nateko.cp.cpauth.core

import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat

object JsonSupport extends DefaultJsonProtocol:

	given RootJsonFormat[PublicAuthConfig] = jsonFormat4(PublicAuthConfig.apply)
