package se.lu.nateko.cpauth.opensaml.test

import org.scalatest.FunSuite
import se.lu.nateko.cpauth.opensaml.ResponseAnalyzer
import se.lu.nateko.cpauth.opensaml.ResponseParser
import org.opensaml.security.SAMLSignatureProfileValidator
import org.opensaml.xml.signature.SignatureValidator

class ResponseAnalyzerTest extends FunSuite{

	ignore("Response signature gets verified"){
	}
}

object ResponseTest{
	val analyzer = ResponseAnalyzer.fromPrivateKeyAt("/saml/test_private_key.der")
	val parser = ResponseParser()
	val response = parser.fromStream(getClass.getResourceAsStream("/saml/response_sample.xml"))
	val assertion = analyzer.get.extractAssertions(response).head
	
	val profileValidator = new SAMLSignatureProfileValidator();

	profileValidator.validate(assertion.getSignature());

	//val sigValidator = new SignatureValidator(cred);

	//sigValidator.validate(entityDescriptor.getSignature());
}