package se.lu.nateko.cp.cpauth.opensaml

import java.security.PublicKey
import org.opensaml.saml2.core.Assertion
import se.lu.nateko.cp.cpauth.xmldsig.SignatureValidator
import org.opensaml.saml2.core.Response
import scala.util.Try
import java.net.URI

case class ValidatedAssertion(assertion: Assertion, error: Option[String])


class AssertionValidator(pubKey: PublicKey, whiteListed: Boolean = false) {

	def validate(assertion: Assertion): ValidatedAssertion = {

		val error = if(assertion.isSigned)
				SignatureValidator.getValidationError(assertion.getDOM, pubKey)
		else if (! whiteListed)
			Some("Assertion is not signed!")
	    else
	        None

		ValidatedAssertion(assertion, error)

	}

}

object AssertionValidator{

	def apply(response: Response, idpLib: IdpLibrary): Try[AssertionValidator] = for {

		idpId <- Try(new URI(response.getIssuer.getValue));

		idpProp <- idpLib.getIdpProps(idpId)

	} yield new AssertionValidator(idpProp.key, idpLib.isWhitelisted(idpId))

}
