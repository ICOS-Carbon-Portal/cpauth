package se.lu.nateko.cp.cpauth.opensaml

import java.security.PublicKey
import org.opensaml.saml.saml2.core.Assertion
import se.lu.nateko.cp.cpauth.xmldsig.SignatureValidator
import org.opensaml.saml.saml2.core.Response
import scala.util.Try
import java.net.URI

case class ValidatedAssertion(assertion: Assertion, error: Option[String])


class AssertionValidator(pubKeys: Seq[PublicKey]) {

	def validate(assertion: Assertion, response: Response): ValidatedAssertion = {

		val error = if(response.isSigned)
			SignatureValidator.getValidationError(response.getDOM, pubKeys)
		else if(assertion.isSigned)
			SignatureValidator.getValidationError(assertion.getDOM, pubKeys)
		else
			Some("Assertion is not signed!")

		ValidatedAssertion(assertion, error)

	}

}

object AssertionValidator{

	def apply(response: Response, idpLib: IdpLibrary): Try[AssertionValidator] = for {

		idpId <- Try(new URI(response.getIssuer.getValue));

		idpProp <- idpLib.getIdpProps(idpId)

	} yield new AssertionValidator(idpProp.keys)

}
