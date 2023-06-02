package se.lu.nateko.cp.viewscore

import eu.icoscp.envri.Envri
import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat
import se.lu.nateko.cp.cpauth.core.JsonSupport.{given RootJsonFormat[Envri]}
import se.lu.nateko.cp.cpauth.core.ConfigLoader.{appConfig, parseAs}

case class ViewsCoreConfig(authHost: String, dataHost: String)

given RootJsonFormat[ViewsCoreConfig] = jsonFormat2(ViewsCoreConfig.apply)

lazy val envri2Config = appConfig.getValue("viewsCore").parseAs[Map[Envri, ViewsCoreConfig]]

def viewsConfig(using envri: Envri) = envri2Config.getOrElse(
	envri,
	throw Exception(s"viewscore is not configured for ENVRI '${envri.shortName}'")
)
