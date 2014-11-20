package eu.carbonportal.cpauth

import com.onelogin._
import com.onelogin.saml._

object Saml {

	def getAuthRequest: String = {
		
		val appSetts = new AppSettings()
		appSetts.setAssertionConsumerServiceUrl("http://localhost.local:8080/loginRedirectTarget")
		appSetts.setIssuer("Carbon Portal Authentication Service")
		
		val req = new AuthRequest(appSetts, null)
		return req.getRequest(0)
	}

}