package se.lu.nateko.cpauth.core

trait UrlsConfig{
	def serviceHost: String
	def serviceUrl: String = "https://" + serviceHost
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
	def validitySeconds: Int
	def privateKeyPath: String
}

trait PublicAuthConfig{
	def publicKeyPath: String
}

trait Config extends UrlsConfig with SamlConfig with PrivateAuthConfig with PublicAuthConfig

