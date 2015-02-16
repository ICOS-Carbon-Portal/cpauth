package se.lu.nateko.cpauth.opensaml.test

import org.scalatest.FunSuite
import se.lu.nateko.cpauth.opensaml.ResponseAnalyzer
import org.opensaml.security.SAMLSignatureProfileValidator
import se.lu.nateko.cpauth.opensaml.Parser
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.metadata.EntitiesDescriptor
import se.lu.nateko.cpauth.core.Crypto
import org.opensaml.xml.security.credential.BasicCredential
import java.security.PublicKey
import se.lu.nateko.cpauth.xmldsig.SignatureValidator

class ResponseAnalyzerTest extends FunSuite{

	ignore("Response signature gets verified"){
	}
}

object ResponseTest{
//	val analyzer = ResponseAnalyzer.fromPrivateKeyAt("/saml/test_private_key.der")
	val response = Parser.fromStream[Response](getClass.getResourceAsStream("/saml/response_sample.xml"))
//	val assertion = analyzer.get.extractAssertions(response).head
	

	def entsDescr = Parser.fromStream[EntitiesDescriptor](getClass.getResourceAsStream("/saml/testshib_idp_metadata.xml"))
	def entDescr = entsDescr.getEntityDescriptors.get(0).getIDPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
	
	def keyDescr = entDescr.getKeyDescriptors.get(0)
	
	def cert = keyDescr.getKeyInfo.getX509Datas.get(0).getX509Certificates.get(0).getValue
	
	def key: PublicKey = Crypto.publicKeyFromX509Cert(cert)
	
//	def validationError = SignatureValidator.getValidationError(assertion.getDOM, key)
}