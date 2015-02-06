package se.lu.nateko.cpauth

import opensaml.ResponseAnalyzer
import ResponseAnalyzer._
import se.lu.nateko.cpauth.core.Crypto
import se.lu.nateko.cpauth.core.CoreUtils
import se.lu.nateko.cpauth.core.Constants

object Playground {

	Utils.setRootLoggingLevelToInfo()

	def keyBytes = CoreUtils.getResourceBytes(Constants.privateKeyPath)
	def privateKey = Crypto.rsaPrivateFromDerBytes(keyBytes).get
	def responseAnalyzer = new ResponseAnalyzer(privateKey)
	
	def responseStream = getClass.getResourceAsStream("/response_sample.xml")
	
	def assertions = responseAnalyzer.extractAssertions(responseStream).toIndexedSeq
	
	def attrValues = extractAttributeStringValues(assertions)
	
	def getResponseSummary(response: String): String = {
		val assertions = responseAnalyzer.extractAssertions(response).toIndexedSeq
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