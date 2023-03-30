package se.lu.nateko.cp.cpauth.core

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import eu.icoscp.envri.Envri
import spray.json.enrichString

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

	def authPubConfig: Map[Envri,PublicAuthConfig] = appConfig
		.getValue("authPub")
		.render(renderOpts)
		.parseJson
		.convertTo[Map[Envri,PublicAuthConfig]]
