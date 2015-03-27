package se.lu.nateko.cpauth

import se.lu.nateko.cpauth.core.Config
import se.lu.nateko.cpauth.core.SamlSpConfig
import se.lu.nateko.cpauth.core.ProxyConfig

object Constants extends Config{

	val serviceHost = "cpauth.icos-cp.eu"
	val loginPath = "/login/"
	val servicePrivatePort = 8080

	val drupalProxying = Map("www.icos-cp.eu" -> ProxyConfig("127.0.0.1", 8085))

	val idpMetadataFilePath = "/swamid-idps.xml"
	val samlSpXmlPath = "/icos-cp_sp_meta.xml"
	val privateKeyPath = "/crypto/private/cpauth_private.der"
	val spConfig = SamlSpConfig(serviceUrl + "/saml/cpauth", serviceUrl + "/saml/SAML2/POST")

	val mailAttr = "mail"
	val givenNameAttr = "givenName"
	val surnameAttr = "sn"

	val authTokenValiditySeconds: Int = 100000
	val publicKeyPath = "/crypto/cpauth_public.pem"
}
