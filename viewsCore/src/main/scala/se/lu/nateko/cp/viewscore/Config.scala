package se.lu.nateko.cp.viewscore

import eu.icoscp.envri.Envri
import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat
import se.lu.nateko.cp.cpauth.core.JsonSupport.{given RootJsonFormat[Envri]}
import se.lu.nateko.cp.cpauth.core.ConfigLoader.{appConfig, parseAs}

case class HostConfig(authHost: String, dataHost: String)

case class ViewsCoreConfig(
	environmentName: Option[String],
	showUnderConstruction: Boolean,
	showCarbonBadge: Boolean,
	hosts: Map[Envri, HostConfig],
)

given RootJsonFormat[HostConfig] = jsonFormat2(HostConfig.apply)
given RootJsonFormat[ViewsCoreConfig] = jsonFormat4(ViewsCoreConfig.apply)

private val viewsCoreConfigRoot = appConfig.getConfig("viewsCore")

given viewsCoreConfig: ViewsCoreConfig = viewsCoreConfigRoot.root().parseAs[ViewsCoreConfig]

def hostsConfig(using envri: Envri) = viewsCoreConfig.hosts.getOrElse(
	envri,
	throw Exception(s"viewscore is not configured for ENVRI '${envri.shortName}'")
)
