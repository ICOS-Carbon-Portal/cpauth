package se.lu.nateko.cp.cpauth

import com.typesafe.config.Config
import spray.json.DefaultJsonProtocol
import spray.json._
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigFactory
import se.lu.nateko.cp.cpauth.core.PublicAuthConfig

case class HttpConfig(
		serviceHost: String,
		servicePrivatePort: Int,
		loginPath: String,
		drupalProxying: Map[String, ProxyConfig]){
	def serviceUrl: String = "https://" + serviceHost
	def authDomain: String = HttpConfig.cookieDomainFromHost(serviceHost)
}

case class SamlSpConfig(url: String, consumerServiceUrl: String)
case class ProxyConfig(ipv4Host: String, path: Option[String], port: Int)
case class SamlAttrConfig(mail: Seq[String], givenName: Seq[String], surname: Seq[String])

case class SamlConfig(
	idpMetadataFilePath: String,
	idpCookieName: String,
	privateKeyPath: String,
	spConfig: SamlSpConfig,
	attributes: SamlAttrConfig
)

case class PrivateAuthConfig(authTokenValiditySeconds: Int, privateKeyPath: String)
case class AuthConfig(priv: PrivateAuthConfig, pub: PublicAuthConfig)
case class RestHeartConfig(baseUri: String, dbName: String, usersCollection: String)
case class EmailConfig(smtpServer: String, fromAddress: String, logBccAddress: Option[String])

case class CpauthConfig(
	http: HttpConfig,
	saml: SamlConfig,
	auth: AuthConfig,
	restheart: RestHeartConfig,
	mailing: EmailConfig,
	oauth: OAuthConfig
)

case class OAuthConfig(facebook: OAuthProviderConfig){
	def public = OAuthConfig(facebook.public)
	def jsonString: String = public.toJson(ConfigReader.oauthConfigFormat).prettyPrint
}

case class OAuthProviderConfig(clientId: String, clientSecret: String, redirectPath: String){
	def public = this.copy(clientSecret = "")
}

object HttpConfig{

	def cookieDomainFromHost(host: String): String = host.count(_ == '.') match{
		case 0 => host
		case x => host.split('.').drop(x - 1).mkString(".", ".", "")
	}

}

object ConfigReader extends DefaultJsonProtocol{

	def getDefault: CpauthConfig = fromAppConfig(getAppConfig)

	def getAppConfig: Config = {
		val default = ConfigFactory.load
		val confFile = new java.io.File("application.conf").getAbsoluteFile
		if(!confFile.exists) default
		else ConfigFactory.parseFile(confFile).withFallback(default)
	}

	implicit val samlSpConfigFormat = jsonFormat2(SamlSpConfig)
	implicit val proxyConfigFormat = jsonFormat3(ProxyConfig)
	implicit val samlAttrFormat = jsonFormat3(SamlAttrConfig)
	//.apply needed because of the companion object that HttpConfig has
	implicit val urlsConfigFormat = jsonFormat4(HttpConfig.apply)
	implicit val samlConfigFormat = jsonFormat5(SamlConfig)

	implicit val pubAuthConfigFormat = jsonFormat2(PublicAuthConfig)
	implicit val privAuthConfigFormat = jsonFormat2(PrivateAuthConfig)
	implicit val authConfigFormat = jsonFormat2(AuthConfig)
	implicit val restHeartConfigFormat = jsonFormat3(RestHeartConfig)
	implicit val emailConfigFormat = jsonFormat3(EmailConfig)
	implicit val facebookConfigFormat = jsonFormat3(OAuthProviderConfig)
	implicit val oauthConfigFormat = jsonFormat1(OAuthConfig)

	implicit val cpauthConfigFormat = jsonFormat6(CpauthConfig)

	def fromAppConfig(applicationConfig: Config): CpauthConfig = {


		val renderOpts = ConfigRenderOptions.concise.setJson(true)
		val cpConfJson: String = applicationConfig.getValue("cpauth").render(renderOpts)

		cpConfJson.parseJson.convertTo[CpauthConfig]
	}

}
