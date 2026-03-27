package se.lu.nateko.cp.viewscore

import eu.icoscp.envri.Envri
import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat
import se.lu.nateko.cp.cpauth.core.JsonSupport.{given RootJsonFormat[Envri]}
import se.lu.nateko.cp.cpauth.core.ConfigLoader.{appConfig, parseAs}

case class DomainConfig(authHost: String, dataHost: String)

case class ViewsCoreConfig(
	environmentName: Option[String],
	showUnderConstruction: Boolean,
	showCarbonBadge: Boolean,
	domains: Map[Envri, DomainConfig],
)

given RootJsonFormat[DomainConfig] = jsonFormat2(DomainConfig.apply)
given RootJsonFormat[ViewsCoreConfig] = jsonFormat4(ViewsCoreConfig.apply)

private val viewsCoreConfigRoot = appConfig.getConfig("viewsCore")

lazy val viewsCoreConfig = viewsCoreConfigRoot.root().parseAs[ViewsCoreConfig]

def domainsConfig(using envri: Envri) = viewsCoreConfig.domains.getOrElse(
	envri,
	throw Exception(s"viewscore is not configured for ENVRI '${envri.shortName}'")
)
