package se.lu.nateko.cpauth.core

trait Config{
	def privateKeyPath: String
	def idpMetadataFilePath: String
}

object Constants extends Config{

	val spUrl = "https://cpauth.icos-cp.eu/saml/cpauth"
	val consumerServiceUrl = "https://cpauth.icos-cp.eu/saml/SAML2/POST"

	val idpMetadataFilePath = "/swamid-idps.xml"
	val samlSpXmlPath = "/icos-cp_sp_meta.xml"
	val privateKeyPath = "/crypto/private/cpauth_private.der"

	val emailAttrName = "eduPersonPrincipalName"
	val fnameAttrName = "givenName"
	val lnameAttrName = "sn"

}