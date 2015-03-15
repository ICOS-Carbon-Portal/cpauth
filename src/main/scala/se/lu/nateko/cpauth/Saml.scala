package se.lu.nateko.cpauth

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import scala.xml.Elem
import java.net.URL
import org.opensaml.saml2.core.NameIDType
import se.lu.nateko.cpauth.core.SamlSpConfig

object Saml {

	def getAuthUri(idpHttpRedirectUrl: URL, reqXml: Elem): spray.http.Uri = {

		val trimmedReqStr = scala.xml.Utility.trim(reqXml).toString
		val authRequestBase64 = Utils.compressAndBase64ForSaml(trimmedReqStr)
		val authUrl = idpHttpRedirectUrl +"?SAMLRequest=" + URLEncoder.encode(authRequestBase64, "UTF-8")

		spray.http.Uri(authUrl)
	}

	def authRequestXmlAndId(spConfig: SamlSpConfig): (Elem, String) = {

		val id = "_" + UUID.randomUUID().toString
		val simpleDf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
		val issueInstant = simpleDf.format(new Date())

		val xml = <samlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol" ID={id} Version="2.0"
			IssueInstant={issueInstant} ProtocolBinding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
			AssertionConsumerServiceURL={spConfig.consumerServiceUrl}>

			<saml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">{spConfig.url}</saml:Issuer>

			<samlp:NameIDPolicy Format={NameIDType.UNSPECIFIED} AllowCreate="true"/>

			<samlp:RequestedAuthnContext Comparison="exact">
				<saml:AuthnContextClassRef xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">
					urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
				</saml:AuthnContextClassRef>
			</samlp:RequestedAuthnContext>

		</samlp:AuthnRequest>
		(xml, id)
	}

}