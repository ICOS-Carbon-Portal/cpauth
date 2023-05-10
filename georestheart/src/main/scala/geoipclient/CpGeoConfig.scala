package eu.icoscp.geoipclient

import spray.json.{DefaultJsonProtocol, RootJsonFormat, enrichString}
import se.lu.nateko.cp.cpauth.core.ConfigLoader

case class CpGeoConfig(baseUri: String, maxAgeDays: Int, emailErrorsTo: String)

object CpGeoConfig extends DefaultJsonProtocol:
	import ConfigLoader.{appConfig, parseAs}
	given RootJsonFormat[CpGeoConfig] = jsonFormat3(CpGeoConfig.apply)

	def load: CpGeoConfig = appConfig.getValue("geoipclient").parseAs[CpGeoConfig]
