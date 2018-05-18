package se.lu.nateko.cp.cpauth

import java.net.URI
import scala.util.Try
import com.typesafe.config.Config
import spray.json._
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigFactory
import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import Envri.Envri
import OAuthProvider.OAuthProvider

object Envri extends Enumeration{
	type Envri = Value
	val ICOS, SITES = Value
}

object OAuthProvider extends Enumeration{
	type OAuthProvider = Value
	val facebook, orcidid = Value
}

case class HttpConfig(
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
	masterAdminUser: String,
	masterAdminPass: String
)

case class RestHeartConfig(
	baseUri: String,
	dbNames: Map[Envri, String],
	usersCollection: String,
	usageCollection: String,
	ipsToIgnore: Seq[String]
){
	def dbName(implicit envri: Envri) = dbNames(envri)
}

case class CpGeoConfig(baseUri: String, maxAgeDays: Int)

case class EmailConfig(
	smtpServer: String,
	username: String,
	password: String,
	fromAddress: String,
	logBccAddress: Option[String]
)

case class CpauthConfig(
	http: HttpConfig,
	saml: SamlConfig,
	database: DatabaseConfig,
	auth: AuthConfig,
	restheart: RestHeartConfig,
	mailing: EmailConfig,
	oauth: CpauthConfig.OAuthConfig,
	geoip: CpGeoConfig
)

object CpauthConfig{
	type EnvriOAuthConfig = Map[OAuthProvider, OAuthProviderConfig]
	type OAuthConfig = Map[Envri, EnvriOAuthConfig]

	def oauthJson(conf: EnvriOAuthConfig): String = {
		conf.mapValues(_.public).toJson(ConfigReader.envriOAuthConfigFormat).prettyPrint
	}
}

case class OAuthProviderConfig(clientId: String, clientSecret: String, redirectPath: String){
	def public = this.copy(clientSecret = "")
}

object ConfigReader extends DefaultJsonProtocol{

	implicit val envriFormat = CpauthJsonProtocol.enumFormat(Envri)
	implicit val oAuthProviderFormat = CpauthJsonProtocol.enumFormat(OAuthProvider)

	implicit object urlFormat extends RootJsonFormat[URI] {
		def write(uri: URI): JsValue = JsString(uri.toString)

		def read(value: JsValue): URI = value match{
			case JsString(uri) => try{
				new URI(uri)
			}catch{
				case err: Throwable => deserializationError(s"Could not parse URI from $uri", err)
			}
			case _ => deserializationError("URI string expected")
		}
	}

	def getDefault: Try[CpauthConfig] = Try(fromAppConfig(getAppConfig))

	def getAppConfig: Config = {
		val default = ConfigFactory.load
		val confFile = new java.io.File("application.conf").getAbsoluteFile
		if(!confFile.exists) default
		else ConfigFactory.parseFile(confFile).withFallback(default)
	}

	implicit val samlSpConfigFormat = jsonFormat3(SamlSpConfig)
	implicit val proxyConfigFormat = jsonFormat3(ProxyConfig)
	implicit val samlAttrFormat = jsonFormat3(SamlAttrConfig)
	implicit val urlsConfigFormat = jsonFormat4(HttpConfig)
	implicit val samlConfigFormat = jsonFormat5(SamlConfig)
	implicit val databaseConfigFormat = jsonFormat4(DatabaseConfig)

	implicit val pubAuthConfigFormat = jsonFormat4(PublicAuthConfig)
	implicit val privAuthConfigFormat = jsonFormat2(PrivateAuthConfig)
	implicit val authConfigFormat = jsonFormat4(AuthConfig)
	implicit val restHeartConfigFormat = jsonFormat5(RestHeartConfig)
	implicit val emailConfigFormat = jsonFormat5(EmailConfig)
	implicit val oauthProviderConfigFormat = jsonFormat3(OAuthProviderConfig)
	implicit val geoConfigFormat = jsonFormat2(CpGeoConfig)

	implicit val cpauthConfigFormat = jsonFormat8(CpauthConfig.apply)

	def fromAppConfig(applicationConfig: Config): CpauthConfig = {


		val renderOpts = ConfigRenderOptions.concise.setJson(true)
		val cpConfJson: String = applicationConfig.getValue("cpauth").render(renderOpts)

		cpConfJson.parseJson.convertTo[CpauthConfig]
	}

	implicit val envriOAuthConfigFormat = implicitly[JsonFormat[CpauthConfig.EnvriOAuthConfig]]
	
}
