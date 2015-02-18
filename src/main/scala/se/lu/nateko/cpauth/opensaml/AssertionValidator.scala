package se.lu.nateko.cpauth.opensaml

import java.security.PublicKey
import org.opensaml.saml2.core.Assertion
import se.lu.nateko.cpauth.xmldsig.SignatureValidator
import org.opensaml.saml2.core.Response
import scala.util.Try
import java.net.URI

case class ValidatedAssertion(assertion: Assertion, error: Option[String])

class AssertionValidator(pubKey: PublicKey) {

	def validate(assertion: Assertion): ValidatedAssertion = {

		val error = if(assertion.isSigned)
				SignatureValidator.getValidationError(assertion.getDOM, pubKey)
			else
				Some("Assertion is not signed!")

		ValidatedAssertion(assertion, error)

	}

}

object AssertionValidator{

	def apply(response: Response, idpLib: IdpLibrary): Try[AssertionValidator] = for {

		idpId <- Try(new URI(response.getIssuer.getValue));

		idpProp <- idpLib.getIdpProps(idpId)

	} yield new AssertionValidator(idpProp.key)

}