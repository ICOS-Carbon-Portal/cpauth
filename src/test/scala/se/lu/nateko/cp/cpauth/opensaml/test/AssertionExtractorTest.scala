package se.lu.nateko.cp.cpauth.opensaml.test

import org.opensaml.saml2.core.Response
import org.opensaml.xml.encryption.DecryptionException
import org.scalatest.FunSuite

import se.lu.nateko.cp.cpauth.Utils
import se.lu.nateko.cp.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cp.cpauth.opensaml.Parser

class AssertionExtractorTest extends FunSuite{

	private def response: Response = Parser.fromStream[Response](getClass.getResourceAsStream("/saml/response_sample.xml"))

	test("Assertions from TestShib get extracted and decrypted correctly"){

		val analyzer = AssertionExtractor.fromPrivateKeyAt("/saml/test_private_key.der")
		val assertions = analyzer.get.extractAssertions(response)

		assert(assertions.size === 1)
	}
	
	test("Attempt to extract assertions with a wrong private key fails"){

		val analyzer = AssertionExtractor.fromPrivateKeyAt("/private1.der")

		val enableLogging = Utils.disableLogging()

		intercept[DecryptionException] {
			analyzer.get.extractAssertions(response)
		}

		enableLogging()
	}
	
	
}
