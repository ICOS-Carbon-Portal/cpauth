package se.lu.nateko.cp.cpauth.core

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import eu.icoscp.envri.Envri
import spray.json.enrichString
import spray.json.RootJsonReader
import com.typesafe.config.ConfigValue

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

	extension(conf: ConfigValue)
		def parseAs[T: RootJsonReader]: T =
			conf.render(renderOpts).parseJson.convertTo[T]

	def authPubConfig = appConfig.getValue("authPub").parseAs[Map[Envri,PublicAuthConfig]]
