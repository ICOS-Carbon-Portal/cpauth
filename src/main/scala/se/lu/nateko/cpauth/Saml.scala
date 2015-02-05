package se.lu.nateko.cpauth

import se.lu.nateko.cpauth.core.Constants
import com.onelogin._
import com.onelogin.saml._
import java.net.URLEncoder

object Saml {

	
	def getAuthRequest: String = {
		
		val appSetts = new AppSettings()
		appSetts.setAssertionConsumerServiceUrl(Constants.consumerServiceUrl)
		appSetts.setIssuer(Constants.spUrl)
		
		val req = new AuthRequest(appSetts, null)
		return req.getRequest(0)
	}
	
	def getAuthUrl: String = {
		Constants.idpUrl +"?SAMLRequest=" + URLEncoder.encode(getAuthRequest, "UTF-8")
	}

}