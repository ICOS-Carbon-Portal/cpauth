package se.lu.nateko.cp.cpauth.xmldsig

import java.security.PublicKey
import javax.xml.crypto.dsig.XMLSignature
import javax.xml.crypto.dsig.dom.DOMValidateContext
import javax.xml.crypto.dsig.XMLSignatureFactory

object SignatureValidator {

	private[this] val fac: XMLSignatureFactory = XMLSignatureFactory.getInstance("DOM")

	/**
	 * Validates an http://www.w3.org/2000/09/xmldsig# enveloped signature against
	 * an explicitly provided public key, instead of relying on the key info specified in the signature.
	 * Assumes that the top element has 'ID' attribute referred to by the enveloped signature
	 */
	def getValidationError(signedElem: org.w3c.dom.Element, pubKey: PublicKey): Option[String] = {

		val nodeList = signedElem.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature")

		if (nodeList.getLength == 0) 
			Some("No XML digital signature found!")
		else try{

			val valContext = new DOMValidateContext(pubKey, nodeList.item(0))
			valContext.setIdAttributeNS(signedElem, null, "ID")

			val signature: XMLSignature = fac.unmarshalXMLSignature(valContext)

			if(signature.validate(valContext))
				None
			else
				Some("Signature is invalid!")

		}catch {
			case (e: Exception) => Some(e.getMessage)
		}
	}
}