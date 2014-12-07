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
	
	val response = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48c2FtbDJwOlJlc3BvbnNlIHhtbG5zOnNhbWwycD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOnByb3RvY29sIiBEZXN0aW5hdGlvbj0iaHR0cHM6Ly9vbGVnLm1pcnpvdi5jb20vc2FtbC9TQU1MMi9QT1NUIiBJRD0iXzg4MGY0NTVkOTlmZGE1MzQyYTlmMzE5ODgxMDQyYjg0IiBJblJlc3BvbnNlVG89Il8xZTU1OTM3Mi01ZjQwLTQyYjAtYjdmMi0wYTA3M2Q0OTJkODYiIElzc3VlSW5zdGFudD0iMjAxNC0xMi0wNlQxNzoxNzoyNS4xMjNaIiBWZXJzaW9uPSIyLjAiPjxzYW1sMjpJc3N1ZXIgeG1sbnM6c2FtbDI9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iIEZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOm5hbWVpZC1mb3JtYXQ6ZW50aXR5Ij5odHRwczovL2lkcC50ZXN0c2hpYi5vcmcvaWRwL3NoaWJib2xldGg8L3NhbWwyOklzc3Vlcj48c2FtbDJwOlN0YXR1cz48c2FtbDJwOlN0YXR1c0NvZGUgVmFsdWU9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpzdGF0dXM6UmVzcG9uZGVyIj48c2FtbDJwOlN0YXR1c0NvZGUgVmFsdWU9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpzdGF0dXM6SW52YWxpZE5hbWVJRFBvbGljeSIvPjwvc2FtbDJwOlN0YXR1c0NvZGU+PHNhbWwycDpTdGF0dXNNZXNzYWdlPlJlcXVpcmVkIE5hbWVJRCBmb3JtYXQgbm90IHN1cHBvcnRlZDwvc2FtbDJwOlN0YXR1c01lc3NhZ2U+PC9zYW1sMnA6U3RhdHVzPjwvc2FtbDJwOlJlc3BvbnNlPg=="

	def decode64(in: String): String = {
		import org.apache.commons.codec.binary.Base64
		val decoder = new Base64()
		new String(decoder.decode(in), "UTF-8")
	}
}