package eu.icoscp.georestheart

import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import java.net.URI
import eu.icoscp.envri.Envri
import akka.http.scaladsl.model.Uri

case class RestHeartDBConfig(
	uri: URI,
	username: Option[String],
	password: Option[String]
)

case class RestHeartConfig(
	db: Map[Envri, RestHeartDBConfig],
	portalUsageCollection: String,
	usersCollection: String,
	ipsToIgnore: Seq[String],
	skipInit: Boolean
):
	import eu.icoscp.utils.akkauri.{*, given}
	import scala.language.implicitConversions

	def portalUsageCollUri(using Envri): Uri = collUri(portalUsageCollection)
	def usersCollUri(using Envri): Uri = collUri(usersCollection)

	def collUri(lastSegment: String)(using Envri): Uri = dbConf.uri.appendPathSegment(lastSegment)

	def dbConf(using envri: Envri): RestHeartDBConfig = db.getOrElse(
		envri, throw new Exception(s"RestHeart db config for ENVRI '$envri' not found")
	)
end RestHeartConfig

object RestHeartConfig extends DefaultJsonProtocol:
	import se.lu.nateko.cp.cpauth.core.JsonSupport.given
	given RootJsonFormat[RestHeartDBConfig] = jsonFormat3(RestHeartDBConfig.apply)
	given RootJsonFormat[RestHeartConfig] = jsonFormat5(RestHeartConfig.apply)
