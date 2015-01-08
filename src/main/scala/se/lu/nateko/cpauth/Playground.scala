package se.lu.nateko.cpauth

import opensaml.ResponseAnalyzer
import ResponseAnalyzer._
import se.lu.nateko.cpauth.core.Crypto
import se.lu.nateko.cpauth.core.CoreUtils

object Playground {

	Utils.setRootLoggingLevelToInfo()

	val keyBytes = CoreUtils.getResourceBytes("/private_key.der")
	val privateKey = Crypto.rsaPrivateFromDerBytes(keyBytes).get
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