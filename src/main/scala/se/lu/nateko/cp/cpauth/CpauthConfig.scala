package se.lu.nateko.cp.cpauth

import akka.http.scaladsl.model.Uri
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.core.ConfigLoader
import se.lu.nateko.cp.cpauth.core.EmailConfig
import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import spray.json.*

import java.net.URI
import scala.util.Try
import eu.icoscp.georestheart.RestHeartConfig
import eu.icoscp.geoipclient.CpGeoConfig

enum OAuthProvider:
	case facebook, orcidid, atmoAccess

case class HttpConfig(
	serviceInterface: String,
	serviceHosts: Map[Envri, String],
	servicePrivatePort: Int,
	loginPath: String,
	drupalProxying: Map[String, ProxyConfig]
){
	def serviceHost(implicit envri: Envri) = serviceHosts(envri)
	def serviceUrl(implicit envri: Envri): String = "https://" + serviceHost
}

case class SamlSpConfig(url: String, consumerServiceUrl: String, spMetaPath: String)
case class ProxyConfig(ipv4Host: String, path: Option[String], port: Int)
case class SamlAttrConfig(mail: Seq[String], givenName: Seq[String], surname: Seq[String])

case class SamlConfig(
	idpMetadataFilePath: String,
	idpCookieName: String,
	privateKeyPaths: Map[Envri, String],
	spConfigs: Map[Envri, SamlSpConfig],
	attributes: SamlAttrConfig
){
	def spConfig(implicit envri: Envri) = spConfigs(envri)
	def privateKeyPath(implicit envri: Envri) = privateKeyPaths(envri)
}

case class DatabaseConfig(
	driver: String,
	url: String,
	user: String,
	password: String
)

case class PrivateAuthConfig(authTokenValiditySeconds: Int, privateKeyPaths: Map[Envri, String]){
	def privateKeyPath(implicit envri: Envri) = privateKeyPaths(envri)
}
case class AuthConfig(
	priv: PrivateAuthConfig,
	pub: Map[Envri, PublicAuthConfig],
	secretUserSalt: String,
	masterAdminUser: String,
	masterAdminPass: String
)

case class CpauthConfig(
	http: HttpConfig,
	saml: SamlConfig,
	database: DatabaseConfig,
	auth: AuthConfig,
	restheart: RestHeartConfig,
	mailing: EmailConfig,
	oauth: CpauthConfig.OAuthConfig,
)

object CpauthConfig{
	type EnvriOAuthConfig = Map[OAuthProvider, OAuthProviderConfig]
	type OAuthConfig = Map[Envri, EnvriOAuthConfig]
	import ConfigReader.given

	def oauthJson(conf: EnvriOAuthConfig): String = {
		conf.map{
			case (provider, config) => provider -> config.public
		}.toJson.prettyPrint
	}
}

case class OAuthProviderConfig(clientId: String, clientSecret: String, redirectPath: String){
	def public = this.copy(clientSecret = "")
}

object ConfigReader extends DefaultJsonProtocol:

	import se.lu.nateko.cp.cpauth.core.JsonSupport.{enumFormat, given}
	given RootJsonFormat[OAuthProvider] = enumFormat(OAuthProvider.valueOf, OAuthProvider.values)

	def getDefault: Try[CpauthConfig] = Try{
		import ConfigLoader.{appConfig, parseAs}
		appConfig.getValue("cpauth").parseAs[CpauthConfig]
	}

	given RootJsonFormat[SamlSpConfig] = jsonFormat3(SamlSpConfig.apply)
	given RootJsonFormat[ProxyConfig] = jsonFormat3(ProxyConfig.apply)
	given RootJsonFormat[SamlAttrConfig] = jsonFormat3(SamlAttrConfig.apply)
	given RootJsonFormat[HttpConfig] = jsonFormat5(HttpConfig.apply)
	given RootJsonFormat[SamlConfig] = jsonFormat5(SamlConfig.apply)
	given RootJsonFormat[DatabaseConfig] = jsonFormat4(DatabaseConfig.apply)

	given RootJsonFormat[PrivateAuthConfig] = jsonFormat2(PrivateAuthConfig.apply)
	given RootJsonFormat[AuthConfig] = jsonFormat5(AuthConfig.apply)
	given RootJsonFormat[EmailConfig] = jsonFormat5(EmailConfig.apply)
	given RootJsonFormat[OAuthProviderConfig] = jsonFormat3(OAuthProviderConfig.apply)

	given RootJsonFormat[CpauthConfig] = jsonFormat7(CpauthConfig.apply)
