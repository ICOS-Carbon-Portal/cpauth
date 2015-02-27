package se.lu.nateko.cpauth.core

trait UrlsConfig{
	def serviceHost: String
	def serviceUrl: String = "https://" + serviceHost
	def authDomain: String = serviceHost.substring(serviceHost.indexOf("."))
	def loginPath: String
}

trait SamlConfig{
	def idpMetadataFilePath: String
	def samlSpXmlPath: String
	def privateKeyPath: String
	def consumerServiceUrl: String
	def spUrl: String

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

