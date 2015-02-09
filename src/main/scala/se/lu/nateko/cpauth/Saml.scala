package se.lu.nateko.cpauth

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

import scala.xml.Elem

import se.lu.nateko.cpauth.core.CoreUtils

object Saml {


	def getAuthUrl(idpHttpRedirectUrl: String, httpPostConsumerUrl: String, serviceProviderUrl: String): String = {

		val reqXml = authRequestXml(httpPostConsumerUrl, serviceProviderUrl)

		val trimmedReqStr = scala.xml.Utility.trim(reqXml).toString

		val authRequestBase64 = CoreUtils.compressAndBase64(trimmedReqStr)

		idpHttpRedirectUrl +"?SAMLRequest=" + URLEncoder.encode(authRequestBase64, "UTF-8")
	}


	def authRequestXml(httpPostConsumerUrl: String, serviceProviderUrl: String): Elem = {

		val id="_" + UUID.randomUUID().toString
		val simpleDf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
		val issueInstant = simpleDf.format(new Date())

		<samlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol" ID={id} Version="2.0"
			IssueInstant={issueInstant} ProtocolBinding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
			AssertionConsumerServiceURL={httpPostConsumerUrl}>

			<saml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">{serviceProviderUrl}</saml:Issuer>

			<samlp:NameIDPolicy Format="urn:oasis:names:tc:SAML:2.0:nameid-format:transient" AllowCreate="true"/>

			<samlp:RequestedAuthnContext Comparison="exact">
				<saml:AuthnContextClassRef xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">
					urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
				</saml:AuthnContextClassRef>
			</samlp:RequestedAuthnContext>

		</samlp:AuthnRequest>
	}

}