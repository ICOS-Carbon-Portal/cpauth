package se.lu.nateko.cp.cpauth.test.opensaml

import org.scalatest.FunSuite
import se.lu.nateko.cp.cpauth.opensaml.IdpLibrary
import java.net.URI
import se.lu.nateko.cp.cpauth.opensaml.Parser
import org.opensaml.saml2.core.Assertion
import se.lu.nateko.cp.cpauth.opensaml.AssertionValidator
import se.lu.nateko.cp.cpauth.core.CoreUtils
import se.lu.nateko.cp.cpauth.core.Crypto

class AssertionValidatorTest extends FunSuite{

	private def getAssertion: Assertion = {
		val assertionStream = getClass.getResourceAsStream("/saml/testshib_assertion.xml")
		Parser.fromStream[Assertion](assertionStream)
	}

	test("Assertion from TestShib validates successfully"){

		val testShibMetadata = getClass.getResourceAsStream("/saml/testshib_idp_metadata.xml")
		val idpLibrary = IdpLibrary.fromMetaStream(testShibMetadata)
		val idpProps = idpLibrary.getIdpProps(new URI("https://idp.testshib.org/idp/shibboleth")).get
		val validator = new AssertionValidator(idpProps.key)

		val error: Option[String] = validator.validate(getAssertion).error

		assert(error.isEmpty)
	}
	
	test("Attempt to validate against a wrong public key fails"){
		val keyLines = CoreUtils.getResourceLines("/public1.pem")
		val key = Crypto.rsaPublicFromPemLines(keyLines.toIndexedSeq).get
		val validator = new AssertionValidator(key)

		val error: Option[String] = validator.validate(getAssertion).error
		assert(error.isDefined)
	}
}
