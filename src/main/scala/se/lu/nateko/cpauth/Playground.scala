package se.lu.nateko.cpauth

import org.opensaml.saml2.core.Response
import se.lu.nateko.cpauth.opensaml.AssertionValidator
import se.lu.nateko.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cpauth.opensaml.AssertionExtractor._
import se.lu.nateko.cpauth.core.Constants
import scala.util.Try
import se.lu.nateko.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cpauth.opensaml.ValidatedAssertion
import se.lu.nateko.cpauth.opensaml.StatementExtractor
import org.opensaml.saml2.core.Assertion
import se.lu.nateko.cpauth.opensaml.Parser
import se.lu.nateko.cpauth.core.CoreUtils

object Playground {

	Utils.setRootLoggingLevelToInfo()

	def getResponseSummary(response: Response, extractor: AssertionExtractor, idpLib: IdpLibrary): Try[String] = 
		AssertionValidator(response, idpLib).map(validator => 
			extractor.extractAssertions(response).map(validator.validate)
				.flatMap(getAssertionSummary).toSeq.sortBy(s => s).mkString("\n")
		)

	private def getAssertionSummary(validated: ValidatedAssertion): Iterable[String] = {
		val validityInfo = validated.error match{
			case None => "Signature OK"
			case Some(error) => "*** " + error
		}
		StatementExtractor.extractAttributeStringValues(validated.assertion).map{
			case (name, value) => s"$name: $value\t$validityInfo"
		}
	}


//	def getAssertions: Iterable[Assertion] = {
//		val responseBase64 = CoreUtils.getResourceLines("/shib_net_lu_se_response.txt").mkString("")
//		val response = Parser.fromBase64[Response](responseBase64)
//		val assExtractor = AssertionExtractor(Constants).get
//		assExtractor.extractAssertions(response)
//	}

	def produceXmlSignature(projRootPath: String): Unit = {
		val resPath = projRootPath + "/src/main/resources"
		val xmlPath = resPath + "/icos-cp_sp_meta_unsigned.xml"
		val privKeyPath = resPath + Constants.privateKeyPath
		val pubKeyPath =  resPath + "/crypto/cpauth_public.der"
		val destDoc =  resPath + "/icos-cp_sp_meta_signed.xml"
		
		val signGen = new com.ddlab.rnd.xml.digsig.XmlDigitalSignatureGenerator()
		signGen.generateXMLDigitalSignature(xmlPath, destDoc, privKeyPath, pubKeyPath)
	}
}