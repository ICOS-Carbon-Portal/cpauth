package se.lu.nateko.cp.cpauth.xmldsig

import java.security.PublicKey
import javax.xml.crypto.dsig.XMLSignature
import javax.xml.crypto.dsig.dom.DOMValidateContext
import javax.xml.crypto.dsig.XMLSignatureFactory
import org.w3c.dom.Node

object SignatureValidator {

	private case class VerificationAcc(success: Boolean, errors: Seq[String])

	private[this] val fac: XMLSignatureFactory = XMLSignatureFactory.getInstance("DOM")

	/**
	 * Validates an http://www.w3.org/2000/09/xmldsig# enveloped signature against
	 * an explicitly provided public key, instead of relying on the key info specified in the signature.
	 * Assumes that the top element has 'ID' attribute referred to by the enveloped signature
	 */
	def getValidationError(signedElem: org.w3c.dom.Element, pubKeys: Seq[PublicKey]): Option[String] = {

		val nodeList = signedElem.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature")

		if (nodeList.getLength == 0) 
			Some("No XML digital signature found!")
		else if(pubKeys.isEmpty)
			Some("The list of public keys was empty, cannot verify signature")
		else {
			val signatureXml = nodeList.item(0)
			val verification = pubKeys.foldLeft(VerificationAcc(false, Nil)){ (acc, key) =>
				if(acc.success) acc
				else singleKeyError(signedElem, signatureXml, key) match{
					case None => acc.copy(success = true)
					case Some(errMsg) => acc.copy(errors = acc.errors :+ errMsg)
				}
			}
			if(verification.success) None
			else Some(verification.errors.mkString("\n"))
		}
	}

	private def singleKeyError(signedElem: org.w3c.dom.Element, signatureXml: Node, pubKey: PublicKey): Option[String] = try{

		val valContext = new DOMValidateContext(pubKey, signatureXml)
		valContext.setIdAttributeNS(signedElem, null, "ID")

		val signature: XMLSignature = fac.unmarshalXMLSignature(valContext)

/*import org.jcp.xml.dsig.internal.dom.DOMX509Data

println(s"Signature's key info:")
signature.getKeyInfo.getContent.forEach(elem => {
	val x509 = elem.asInstanceOf[DOMX509Data]
	x509.getContent.forEach(println)
})
println(s"Actually used key: ${pubKey}")*/

		if(signature.validate(valContext))
			None
		else
			Some("Signature is invalid!")

	}catch {
		case (e: Exception) => Some(e.getMessage)
	}

}
