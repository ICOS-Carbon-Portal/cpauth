package se.lu.nateko.cpauth.opensaml.test

import org.scalatest.FunSuite
import se.lu.nateko.cpauth.opensaml.ResponseAnalyzer
import org.opensaml.security.SAMLSignatureProfileValidator
import org.opensaml.xml.signature.SignatureValidator
import se.lu.nateko.cpauth.opensaml.Parser
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.metadata.EntitiesDescriptor
import se.lu.nateko.cpauth.core.Crypto
import org.opensaml.xml.security.credential.BasicCredential
import java.security.PublicKey

class ResponseAnalyzerTest extends FunSuite{

	ignore("Response signature gets verified"){
	}
}

object ResponseTest{
	val analyzer = ResponseAnalyzer.fromPrivateKeyAt("/saml/test_private_key.der")
	val response = Parser.fromStream[Response](getClass.getResourceAsStream("/saml/response_sample.xml"))
	val assertion = analyzer.get.extractAssertions(response).head
	

	def validateProfile(): Unit = {
		val profileValidator = new SAMLSignatureProfileValidator();
		profileValidator.validate(assertion.getSignature());

	}
	
	def validateSignature(): Unit = {
		val credential = new BasicCredential()
		credential.setPublicKey(key)
		val sigValidator = new SignatureValidator(credential);
		sigValidator.validate(assertion.getSignature());
	}
	
	def entsDescr = Parser.fromStream[EntitiesDescriptor](getClass.getResourceAsStream("/saml/testshib_idp_metadata.xml"))
	def entDescr = entsDescr.getEntityDescriptors.get(0).getIDPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol")
	
	def keyDescr = entDescr.getKeyDescriptors.get(0)
	
	def cert = keyDescr.getKeyInfo.getX509Datas.get(0).getX509Certificates.get(0).getValue
	
	def key: PublicKey = Crypto.publicKeyFromX509Cert(cert)
}