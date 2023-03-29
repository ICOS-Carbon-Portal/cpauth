package se.lu.nateko.cp.cpauth.core

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import spray.json._
import com.typesafe.config.ConfigRenderOptions

object ConfigLoader {
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

	def subConfigAsJson(path: String): JsValue = {
		val renderOpts = ConfigRenderOptions.concise.setJson(true)
		appConfig.getValue(path).render(renderOpts).parseJson
	}

	//TODO Add notion of Envri on the cpauth-core level already, make Envri -> PublicAuthConfig map
	def icosPubAuthConfig = subConfigAsJson("cpauthAuthPub").convertTo[PublicAuthConfig]
	def sitesPubAuthConfig = subConfigAsJson("fieldsitesAuthPub").convertTo[PublicAuthConfig]

}
