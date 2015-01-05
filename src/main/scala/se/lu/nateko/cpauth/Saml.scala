package se.lu.nateko.cpauth

import com.onelogin._
import com.onelogin.saml._
import java.net.URLEncoder

object Saml {

	//val idpUrl = "https://idp.lu.se/idp/profile/SAML2/Redirect/SSO"
	val idpUrl = "https://idp.testshib.org/idp/profile/SAML2/Redirect/SSO"
	
	def getAuthRequest: String = {
		
		val appSetts = new AppSettings()
		appSetts.setAssertionConsumerServiceUrl("https://oleg.mirzov.com/saml/SAML2/POST")
		appSetts.setIssuer("https://oleg.mirzov.com/saml/cpauth/")
		
		val req = new AuthRequest(appSetts, null)
		return req.getRequest(0)
	}
	
	def getAuthUrl: String = {
		idpUrl +"?SAMLRequest=" + URLEncoder.encode(getAuthRequest, "UTF-8")
	}

}