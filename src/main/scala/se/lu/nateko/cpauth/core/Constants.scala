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
}

trait Config extends UrlsConfig with SamlConfig

object Constants extends Config{

	val serviceHost = "cpauth.icos-cp.eu"
	val loginPath = "/login"

	val consumerServiceUrl = serviceUrl + "/saml/SAML2/POST"
	val idpMetadataFilePath = "/swamid-idps.xml"
	val samlSpXmlPath = "/icos-cp_sp_meta.xml"
	val privateKeyPath = "/crypto/private/cpauth_private.der"
	val spUrl = serviceUrl + "/saml/cpauth"

//	val emailAttrName = "eduPersonPrincipalName"
//	val fnameAttrName = "givenName"
//	val lnameAttrName = "sn"

}