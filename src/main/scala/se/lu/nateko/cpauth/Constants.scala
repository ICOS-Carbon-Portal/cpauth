package se.lu.nateko.cpauth

import se.lu.nateko.cpauth.core.Config

object Constants extends Config{

	val serviceHost = "cpauth.icos-cp.eu"
	val loginPath = "/login"

	val consumerServiceUrl = serviceUrl + "/saml/SAML2/POST"
	val idpMetadataFilePath = "/swamid-idps.xml"
	val samlSpXmlPath = "/icos-cp_sp_meta.xml"
	val privateKeyPath = "/crypto/private/cpauth_private.der"
	val spUrl = serviceUrl + "/saml/cpauth"

	val mailAttr = "mail"
	val givenNameAttr = "givenName"
	val surnameAttr = "sn"

	val validitySeconds: Int = 1800
	val publicKeyPath = "/crypto/cpauth_public.pem"
}
