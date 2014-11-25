package eu.carbonportal.cpauth

import com.onelogin._
import com.onelogin.saml._
import java.net.URLEncoder

object Saml {

	val idpUrl = "https://idp.lu.se/idp/profile/SAML2/Redirect/SSO"
	
	def getAuthRequest: String = {
		
		val appSetts = new AppSettings()
		appSetts.setAssertionConsumerServiceUrl("https://localhost.local:8080/welcome")
		appSetts.setIssuer("Carbon Portal Authentication Service")
		
		val req = new AuthRequest(appSetts, null)
		return req.getRequest(0)
	}
	
	def getAuthUrl: String = {
		idpUrl +"?SAMLRequest=" + URLEncoder.encode(getAuthRequest, "UTF-8")
	}

}