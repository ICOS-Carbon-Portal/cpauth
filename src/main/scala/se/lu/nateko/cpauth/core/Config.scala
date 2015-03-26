package se.lu.nateko.cpauth.core

trait UrlsConfig{
	def serviceHost: String
	def servicePrivatePort: Int
	def serviceUrl: String = "https://" + serviceHost

	def authDomain: String = UrlsConfig.cookieDomainFromHost(serviceHost)
  
	def loginPath: String

	def drupalProxying: Map[String, ProxyConfig]
}

case class SamlSpConfig(url: String, consumerServiceUrl: String)
case class ProxyConfig(ipv4Host: String, port: Int)

trait SamlConfig{
	def idpMetadataFilePath: String
	def samlSpXmlPath: String
	def privateKeyPath: String
	def spConfig: SamlSpConfig

	def mailAttr: String
	def givenNameAttr: String
	def surnameAttr: String
}

trait PrivateAuthConfig{
	def authTokenValiditySeconds: Int
	def privateKeyPath: String
}

trait PublicAuthConfig{
	def publicKeyPath: String
	val authCookieName = "cpauthToken"
	val idpCookieName = "lastChosenIdp"
}

trait AuthConfig extends PrivateAuthConfig with PublicAuthConfig

trait Config extends UrlsConfig with SamlConfig with AuthConfig

object UrlsConfig{
	
	def cookieDomainFromHost(host: String): String = host.count { _ == '.' } match{
    	case 0 => host
    	case x => host.split('.').drop(x - 1).mkString(".", ".", "")
	}

}
