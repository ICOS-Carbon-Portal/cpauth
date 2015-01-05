package se.lu.nateko.cpauth

import core.PKCS8EncodedKey
import opensaml.ResponseAnalyzer
import ResponseAnalyzer._

object Playground {

	Utils.setRootLoggingLevelToInfo()

	val privateKey = new PKCS8EncodedKey(Utils.getResourceBytes("/private_key.pk8"))
	val responseAnalyzer = new ResponseAnalyzer(privateKey)
	
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
}