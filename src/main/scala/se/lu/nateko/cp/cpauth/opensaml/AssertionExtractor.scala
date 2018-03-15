package se.lu.nateko.cp.cpauth.opensaml

import java.nio.file.Files
import java.nio.file.Paths
import java.security.interfaces.RSAPrivateKey

import scala.util.Try

import org.opensaml.saml2.core.Assertion
import org.opensaml.saml2.core.EncryptedAssertion
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.encryption.Decrypter
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver
import org.opensaml.xml.security.x509.BasicX509Credential

import se.lu.nateko.cp.cpauth.Envri.Envri
import se.lu.nateko.cp.cpauth.SamlConfig
import se.lu.nateko.cp.cpauth.core.Crypto
import se.lu.nateko.cp.cpauth.utils.Utils.SafeJavaCollectionWrapper

//import org.apache.xml.serializer.dom3.LSSerializerImpl

class AssertionExtractor(key: RSAPrivateKey){
	import AssertionExtractor._

	lazy val decrypter: AssertionDecrypter = {

		val decryptionCredential = new BasicX509Credential()
		decryptionCredential.setPrivateKey(key)

		val decrypter = new Decrypter(null, new StaticKeyInfoCredentialResolver(decryptionCredential), new InlineEncryptedKeyResolver())

//		encrAss => {
//			val ser = new LSSerializerImpl()
//			println(ser.writeToString(encrAss.getDOM))
//			decrypter.decrypt(encrAss)
//		}
		decrypter.decrypt
	}

	def extractAssertions(response: Response): Iterable[Assertion] = {
		val decryptedAssertions = response.getEncryptedAssertions.toSafeIterable.map(decrypter)
		val unencryptedAssertions = response.getAssertions.toSafeIterable
		unencryptedAssertions ++ decryptedAssertions
	}

}

object AssertionExtractor {

	type AssertionDecrypter = EncryptedAssertion => Assertion

	OpenSamlUtils.bootstrapOpenSaml()

	def apply(conf: SamlConfig)(implicit envri: Envri): Try[AssertionExtractor] = fromPrivateKeyAt(conf.privateKeyPath)

	def fromPrivateKeyAt(path: String): Try[AssertionExtractor] = {
		val keyBytes = Files.readAllBytes(Paths.get(path))
		val privateKey = Crypto.rsaPrivateFromDerBytes(keyBytes)
		privateKey.map(key => new AssertionExtractor(key))
	}

}
