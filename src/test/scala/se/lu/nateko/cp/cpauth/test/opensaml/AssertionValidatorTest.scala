package se.lu.nateko.cp.cpauth.test.opensaml

import org.scalatest.funsuite.AnyFunSuite
import se.lu.nateko.cp.cpauth.opensaml.IdpLibrary
import java.net.URI
import se.lu.nateko.cp.cpauth.opensaml.Parser
import org.opensaml.saml2.core.Assertion
import org.opensaml.saml2.core.Response
import se.lu.nateko.cp.cpauth.opensaml.AssertionValidator
import se.lu.nateko.cp.cpauth.core.CoreUtils
import se.lu.nateko.cp.cpauth.core.Crypto

class AssertionValidatorTest extends AnyFunSuite{

	private def getAssertion: Assertion = {
		val assertionStream = getClass.getResourceAsStream("/saml/testshib_assertion.xml")
		Parser.fromStream[Assertion](assertionStream)
	}

	private def getResponse: Response = {
		//dummy response that must not be signed (for the purpose of the test)
		val responseStream = getClass.getResourceAsStream("/saml/response_sample.xml")
		Parser.fromStream[Response](responseStream)
	}

	test("Assertion from TestShib validates successfully"){

		val testShibMetadata = getClass.getResourceAsStream("/saml/testshib_idp_metadata.xml")
		val idpLibrary = IdpLibrary.fromMetaStream(testShibMetadata)
		val idpProps = idpLibrary.getIdpProps(new URI("https://idp.testshib.org/idp/shibboleth")).get
		val validator = new AssertionValidator(idpProps.keys)

		val error: Option[String] = validator.validate(getAssertion, getResponse).error

		assert(error.isEmpty)
	}
	
	test("Attempt to validate against a wrong public key fails"){
		val keyLines = CoreUtils.getResourceLines("/public1.pem")
		val key = Crypto.ecPublicFromPemLines(keyLines.toIndexedSeq).get
		val validator = new AssertionValidator(key :: Nil)

		val error: Option[String] = validator.validate(getAssertion, getResponse).error
		assert(error.isDefined)
	}
}
