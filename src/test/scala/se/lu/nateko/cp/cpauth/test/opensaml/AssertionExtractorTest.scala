package se.lu.nateko.cp.cpauth.test.opensaml

import org.opensaml.saml2.core.Response
import org.opensaml.xml.encryption.DecryptionException
import org.scalatest.funsuite.AnyFunSuite
import se.lu.nateko.cp.cpauth.utils.Utils
import se.lu.nateko.cp.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cp.cpauth.opensaml.Parser
import java.nio.file.Paths
import java.nio.file.Files
import se.lu.nateko.cp.cpauth.core.Crypto

class AssertionExtractorTest extends AnyFunSuite{

	private def response: Response = Parser.fromStream[Response](getClass.getResourceAsStream("/saml/response_sample.xml"))

	test("Assertions from TestShib get extracted and decrypted correctly"){

		val analyzer = AssertionExtractor.fromPrivateKeyAt("src/test/resources/saml/test_private_key.der", "RSA")
		val assertions = analyzer.get.extractAssertions(response)

		assert(assertions.size === 1)
	}
	
	test("Attempt to extract assertions with a wrong private key fails"){

		val analyzer = AssertionExtractor.fromPrivateKeyAt("src/test/resources/private1.der", "EC")

		val enableLogging = Utils.disableLogging()

		intercept[DecryptionException] {
			analyzer.get.extractAssertions(response)
		}

		enableLogging()
	}
	
	
}
