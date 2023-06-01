package se.lu.nateko.cp.viewscore

import se.lu.nateko.cp.cpauth.core.ConfigLoader
import se.lu.nateko.cp.cpauth.core.ConfigLoader.parseAs
import eu.icoscp.envri.Envri
import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat
import se.lu.nateko.cp.cpauth.core.JsonSupport.{ given RootJsonFormat[Envri] }

case class ViewsCoreConfig(
	authHost: String,
	dataHost: String
)

given RootJsonFormat[ViewsCoreConfig] = jsonFormat2(ViewsCoreConfig.apply)

lazy val viewsConfig = ConfigLoader.appConfig.getValue("viewsCore").parseAs[Map[Envri, ViewsCoreConfig]]

def getConfig(using envri: Envri) = viewsConfig.getOrElse(
	envri, 
	throw new Exception(s"viewscore config not configured for ENVRI $envri")
)
