package se.lu.nateko.cp.cpauth.utils

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import scala.xml.Elem
import java.net.URI
import org.opensaml.saml.saml2.core.NameIDType
import java.util.TimeZone
import akka.http.scaladsl.model.Uri
import se.lu.nateko.cp.cpauth.SamlSpConfig

object Saml {

	def getAuthUri(idpHttpRedirectUrl: URI, reqXml: Elem): Uri = {

		val trimmedReqStr = scala.xml.Utility.trim(reqXml).toString
		val authRequestBase64 = Utils.compressAndBase64ForSaml(trimmedReqStr)
		val authUrl = idpHttpRedirectUrl.toString + "?SAMLRequest=" + URLEncoder.encode(authRequestBase64, "UTF-8")

		Uri(authUrl)
	}

	def authRequestXmlAndId(spConfig: SamlSpConfig): (Elem, String) = {

		val id = "_" + UUID.randomUUID().toString
		val issueInstant = nowUtcIso

		val xml = <samlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol" ID={id} Version="2.0"
			IssueInstant={issueInstant} ProtocolBinding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
			AssertionConsumerServiceURL={spConfig.consumerServiceUrl}>

			<saml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">{spConfig.url}</saml:Issuer>

			<samlp:NameIDPolicy Format={NameIDType.TRANSIENT} AllowCreate="true"/>

			<samlp:RequestedAuthnContext Comparison="exact">
				<saml:AuthnContextClassRef xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">
					urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
				</saml:AuthnContextClassRef>
			</samlp:RequestedAuthnContext>

		</samlp:AuthnRequest>
		(xml, id)
	}
	
	def nowUtcIso: String = {
		val tz = TimeZone.getTimeZone("UTC");
		val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		df.setTimeZone(tz);
		df.format(new Date());
	}

}
