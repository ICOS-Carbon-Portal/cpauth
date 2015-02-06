package se.lu.nateko.cpauth.core

object Constants {

  //val idpUrl = "https://idp.lu.se/idp/profile/SAML2/Redirect/SSO"
  val idpUrl = "https://idp.testshib.org/idp/profile/SAML2/Redirect/SSO"
  val spUrl = "https://cpauth.icos-cp.eu/saml/cpauth"
  val consumerServiceUrl = "https://cpauth.icos-cp.eu/saml/SAML2/POST"
  
  val samlSpXmlPath = "/icos-cp_sp_meta.xml"
  val privateKeyPath = "/crypto/private/cpauth_private.der"
  
  val emailAttrName = "eduPersonPrincipalName"
  val fnameAttrName = "givenName"
  val lnameAttrName = "sn"
  
  
}