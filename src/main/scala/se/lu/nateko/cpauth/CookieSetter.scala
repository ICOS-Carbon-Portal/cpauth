package se.lu.nateko.cpauth

import spray.http.HttpCookie
import se.lu.nateko.cpauth.core.UrlsConfig

class CookieSetter(config: UrlsConfig) {

	def getLastIdpCookie(idpId: String): HttpCookie = HttpCookie(
		name = "lastChosenIdp",
		content = idpId,
		secure = false,
		domain = Some(config.serviceHost),
		path = Some(config.loginPath),
		httpOnly = false, //needs to be accessed by Javascript on the client
		maxAge = Some(31536000)
	)

	
}