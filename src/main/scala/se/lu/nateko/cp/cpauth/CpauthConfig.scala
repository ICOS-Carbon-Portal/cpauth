package se.lu.nateko.cp.cpauth

import java.net.URI
import scala.util.Try
import com.typesafe.config.Config
import spray.json._
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigFactory
import se.lu.nateko.cp.cpauth.core.ConfigLoader
import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import eu.icoscp.envri.Envri
import akka.http.scaladsl.model.Uri

enum OAuthProvider:
	case facebook, orcidid

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

case class RestHeartDBConfig(
	uri: URI,
	username: String,
	password: String
)

case class RestHeartConfig(
	db: Map[Envri, RestHeartDBConfig],
	portalUsageCollection: String,
	usersCollection: String,
	ipsToIgnore: Seq[String],
	skipInit: Boolean
):
	import se.lu.nateko.cp.cpauth.utils.uriJavaToAkka
	import scala.language.implicitConversions

	def dbUri(using envri: Envri): Uri = db(envri).uri
	def username(using envri: Envri): String = db(envri).username
	def password(using envri: Envri): String = db(envri).password
	def portalUsageCollUri(using envri: Envri): Uri = dbUri.withPath(dbUri.path / portalUsageCollection)
	def usersCollUri(using envri: Envri): Uri = dbUri.withPath(dbUri.path / usersCollection)


case class CredentialsConfig(username: String, password: String)

case class PostgresConfig(
	hostname: String,
	port: Int,
	dbName: String,
	writer: CredentialsConfig,
	dbAccessPoolSize: Int
)

case class CpGeoConfig(baseUri: String, maxAgeDays: Int, emailErrorsTo: String)

case class EmailConfig(
	smtpServer: String,
	username: String,
	password: String,
	fromAddress: String,
	logBccAddress: Option[String]
)

type PostgresConfigs = Map[Envri, PostgresConfig]

case class CpauthConfig(
	http: HttpConfig,
	saml: SamlConfig,
	database: DatabaseConfig,
	auth: AuthConfig,
	restheart: RestHeartConfig,
	postgres: PostgresConfigs,
	mailing: EmailConfig,
	oauth: CpauthConfig.OAuthConfig,
	geoip: CpGeoConfig
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

object ConfigReader extends DefaultJsonProtocol{

	import se.lu.nateko.cp.cpauth.core.JsonSupport.{enumFormat, given}
	given RootJsonFormat[OAuthProvider] = enumFormat(OAuthProvider.valueOf, OAuthProvider.values)

	def getDefault: Try[CpauthConfig] = Try(fromAppConfig(ConfigLoader.appConfig))

	def getAppConfig: Config = {
		val confFile = new java.io.File("application.conf").getAbsoluteFile
		if(!confFile.exists) ConfigFactory.load
		else
			ConfigFactory.parseFile(confFile)
				.withFallback(ConfigFactory.defaultApplication)
				.withFallback(ConfigFactory.defaultReferenceUnresolved)
				.resolve
	}

	given RootJsonFormat[SamlSpConfig] = jsonFormat3(SamlSpConfig.apply)
	given RootJsonFormat[ProxyConfig] = jsonFormat3(ProxyConfig.apply)
	given RootJsonFormat[SamlAttrConfig] = jsonFormat3(SamlAttrConfig.apply)
	given RootJsonFormat[HttpConfig] = jsonFormat5(HttpConfig.apply)
	given RootJsonFormat[SamlConfig] = jsonFormat5(SamlConfig.apply)
	given RootJsonFormat[DatabaseConfig] = jsonFormat4(DatabaseConfig.apply)
	given RootJsonFormat[RestHeartDBConfig] = jsonFormat3(RestHeartDBConfig.apply)

	given RootJsonFormat[PrivateAuthConfig] = jsonFormat2(PrivateAuthConfig.apply)
	import se.lu.nateko.cp.cpauth.core.JsonSupport.given
	given RootJsonFormat[AuthConfig] = jsonFormat5(AuthConfig.apply)
	given RootJsonFormat[RestHeartConfig] = jsonFormat5(RestHeartConfig.apply)
	given RootJsonFormat[CredentialsConfig] = jsonFormat2(CredentialsConfig.apply)
	given RootJsonFormat[PostgresConfig] = jsonFormat5(PostgresConfig.apply)
	given RootJsonFormat[EmailConfig] = jsonFormat5(EmailConfig.apply)
	given RootJsonFormat[OAuthProviderConfig] = jsonFormat3(OAuthProviderConfig.apply)
	given RootJsonFormat[CpGeoConfig] = jsonFormat3(CpGeoConfig.apply)

	given RootJsonFormat[CpauthConfig] = jsonFormat9(CpauthConfig.apply)

	def fromAppConfig(applicationConfig: Config): CpauthConfig = {
		val renderOpts = ConfigRenderOptions.concise.setJson(true)
		val cpConfJson: String = applicationConfig.getValue("cpauth").render(renderOpts)
		cpConfJson.parseJson.convertTo[CpauthConfig]
	}

}
