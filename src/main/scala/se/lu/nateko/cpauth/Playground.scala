package se.lu.nateko.cpauth

import org.opensaml.saml2.core.Response

import opensaml.ResponseAnalyzer
import opensaml.ResponseAnalyzer.extractAttributeStringValues
import se.lu.nateko.cpauth.core.Constants

object Playground {

	Utils.setRootLoggingLevelToInfo()

	def getResponseSummary(response: Response, analyzer: ResponseAnalyzer): String = {
		val assertions = analyzer.extractAssertions(response).toIndexedSeq
		val attrValues = extractAttributeStringValues(assertions)
		attrValues.map{
			case (attrName, values) => attrName + ": " + values.mkString(" | ")
		}.mkString("\n")
	}

  def testXmlSignature(projRootPath: String): Unit = {
    val resPath = projRootPath + "/src/main/resources"
    val xmlPath = resPath + "/icos-cp_sp_meta_unsigned.xml"
    val privKeyPath = resPath + Constants.privateKeyPath
    val pubKeyPath =  resPath + "/crypto/cpauth_public.der"
    val destDoc =  resPath + "/icos-cp_sp_meta_signed.xml"
    
    val signGen = new com.ddlab.rnd.xml.digsig.XmlDigitalSignatureGenerator()
    signGen.generateXMLDigitalSignature(xmlPath, destDoc, privKeyPath, pubKeyPath)
  }
}