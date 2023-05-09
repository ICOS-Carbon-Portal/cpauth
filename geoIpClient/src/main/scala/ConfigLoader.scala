package se.lu.nateko.cp.cpauth.core

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import eu.icoscp.envri.Envri
import spray.json.enrichString
import se.lu.nateko.cp.cpauth.core.{PublicAuthConfig, CoreConfig}
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol
import se.lu.nateko.cp.geoipclient.CpGeoConfig


object JsonSupport extends DefaultJsonProtocol:

	given RootJsonFormat[CpGeoConfig] = jsonFormat3(CpGeoConfig.apply)

end JsonSupport

object ConfigLoader:
	import JsonSupport.given

	lazy val appConfig: Config = {
		val confFile = new java.io.File("application.conf").getAbsoluteFile
		if(!confFile.exists) ConfigFactory.load
		else
			ConfigFactory.parseFile(confFile)
				.withFallback(ConfigFactory.defaultApplication)
				.withFallback(ConfigFactory.defaultReferenceUnresolved)
				.resolve
	}

	private val renderOpts = ConfigRenderOptions.concise.setJson(true)

	def geoipConfig: CpGeoConfig = appConfig
		.getValue("geoipClient")
		.render(renderOpts)
		.parseJson
		.convertTo[CpGeoConfig]

end ConfigLoader

