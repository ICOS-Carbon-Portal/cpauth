package se.lu.nateko.cp.cpauth.opensaml

import java.nio.file.Files
import java.nio.file.Paths
import java.security.PrivateKey

import scala.util.Try

import org.opensaml.saml2.core.Assertion
import org.opensaml.saml2.core.EncryptedAssertion
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.encryption.Decrypter
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver
import org.opensaml.xml.security.x509.BasicX509Credential

import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.SamlConfig
import se.lu.nateko.cp.cpauth.core.Crypto
import se.lu.nateko.cp.cpauth.utils.Utils.SafeJavaCollectionWrapper
import se.lu.nateko.cp.cpauth.PrivateKeyInfo

//import org.apache.xml.serializer.dom3.LSSerializerImpl

class AssertionExtractor(key: PrivateKey, fallbackKey: Option[PrivateKey]){
	import AssertionExtractor.*

	private def innerDecrypter(pkey: PrivateKey): Decrypter =
		val decryptionCredential = new BasicX509Credential()
		decryptionCredential.setPrivateKey(key)
		new Decrypter(null, new StaticKeyInfoCredentialResolver(decryptionCredential), new InlineEncryptedKeyResolver())

	lazy val decrypter: AssertionDecrypter =
		val decrypter = innerDecrypter(key)

//		encrAss => {
//			val ser = new LSSerializerImpl()
//			println(ser.writeToString(encrAss.getDOM))
//			decrypter.decrypt(encrAss)
//		}
		fallbackKey match
			case None => decrypter.decrypt
			case Some(fallbackKey) =>
				val fallbackDecr = innerDecrypter(fallbackKey)
				assertion =>
					try decrypter.decrypt(assertion)
					catch case _: Throwable => fallbackDecr.decrypt(assertion)


	def extractAssertions(response: Response): Iterable[Assertion] = {
		val decryptedAssertions = response.getEncryptedAssertions.toSafeIterable.map(decrypter)
		val unencryptedAssertions = response.getAssertions.toSafeIterable
		unencryptedAssertions ++ decryptedAssertions
	}

}

object AssertionExtractor:

	type AssertionDecrypter = EncryptedAssertion => Assertion
	import Crypto.KeyType

	OpenSamlUtils.bootstrapOpenSaml()

	def readPrivateKey(kinfo: PrivateKeyInfo): Try[PrivateKey] =
		readPrivateKey(kinfo.filePath, kinfo.keyType)

	def readPrivateKey(filePath: String, keyType: KeyType): Try[PrivateKey] =
		Try:
			Files.readAllBytes(Paths.get(filePath))
		.flatMap: keyBytes =>
			Crypto.privateFromDerBytes(keyBytes, keyType)

	def apply(conf: SamlConfig)(using Envri): Try[AssertionExtractor] =
		val kinfo = conf.privateKeyInfo
		Try:
			val key = readPrivateKey(kinfo.primary).get
			val fallback = kinfo.fallback.map(ki => readPrivateKey(ki).get)
			new AssertionExtractor(key, fallback)
