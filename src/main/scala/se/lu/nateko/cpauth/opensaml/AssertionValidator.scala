package se.lu.nateko.cpauth.opensaml

import java.security.PublicKey
import org.opensaml.saml2.core.Assertion
import se.lu.nateko.cpauth.xmldsig.SignatureValidator

class AssertionValidator(pubKey: PublicKey) {

	def getValidationError(assertion: Assertion): Option[String] = {

		SignatureValidator.getValidationError(assertion.getDOM, pubKey)

	}

}