package se.lu.nateko.cp.cpauth.test.opensaml

import org.opensaml.saml.saml2.core.Response
import org.scalatest.funsuite.AnyFunSuite
import se.lu.nateko.cp.cpauth.utils.Utils
import se.lu.nateko.cp.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cp.cpauth.opensaml.Parser
import java.nio.file.Paths
import java.nio.file.Files
import se.lu.nateko.cp.cpauth.core.Crypto
import org.opensaml.xmlsec.encryption.support.DecryptionException
import java.security.PublicKey
import scala.util.Try
import se.lu.nateko.cp.cpauth.core.CoreUtils

class AssertionExtractorTest extends AnyFunSuite{

	private def response: Response = Parser.fromStream[Response](getClass.getResourceAsStream("/saml/response_sample.xml"))

	private def getPublicKey: Try[PublicKey] =
		val keyFileLines = CoreUtils.getResourceLines("/saml/test_public_key.pem")
		Crypto.publicFromPemLines(keyFileLines.toIndexedSeq, "RSA")


	test("Assertions from TestShib get extracted and decrypted correctly"){

		val analyzer =
			val pubKey = getPublicKey.get
			val privKey = AssertionExtractor.readPrivateKey("src/test/resources/saml/test_private_key.der", "RSA").get
			new AssertionExtractor(pubKey, privKey)
		val assertions = analyzer.extractAssertions(response)

		assert(assertions.size === 1)
	}
	
	test("Attempt to extract assertions with a wrong private key fails"){

		val analyzer =
			val privKey = AssertionExtractor.readPrivateKey("src/test/resources/private1.der", "EC").get
			new AssertionExtractor(getPublicKey.get, privKey)

		//val enableLogging = Utils.disableLogging()

		intercept[DecryptionException] {
			analyzer.extractAssertions(response)
		}

		//enableLogging()
	}
	
	
}
