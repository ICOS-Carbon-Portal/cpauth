package se.lu.nateko.cp.cpauth.opensaml

import java.nio.file.Files
import java.nio.file.Paths
import java.security.PrivateKey

import scala.util.Try

import org.opensaml.saml.saml2.core.Assertion
import org.opensaml.saml.saml2.core.EncryptedAssertion
import org.opensaml.saml.saml2.core.Response
import org.opensaml.saml.saml2.encryption.Decrypter
//import org.opensaml.xml.encryption.InlineEncryptedKeyResolver
//import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver
//import org.opensaml.xml.security.x509.BasicX509Credential

import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.SamlConfig
import se.lu.nateko.cp.cpauth.core.Crypto
import se.lu.nateko.cp.cpauth.utils.Utils.SafeJavaCollectionWrapper
import se.lu.nateko.cp.cpauth.PrivateKeyInfo

//import org.apache.xml.serializer.dom3.LSSerializerImpl
import org.opensaml.security.credential.BasicCredential
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver
import org.opensaml.xmlsec.encryption.support.InlineEncryptedKeyResolver
import java.security.PublicKey
import se.lu.nateko.cp.cpauth.core.Authenticator
import org.opensaml.xmlsec.encryption.support.ChainingEncryptedKeyResolver
import org.opensaml.xmlsec.encryption.support.SimpleKeyInfoReferenceEncryptedKeyResolver
import scala.jdk.CollectionConverters.SeqHasAsJava
import org.apache.xml.security.encryption.XMLCipher

class AssertionExtractor(pubKey: PublicKey, privKey: PrivateKey){
	import AssertionExtractor.*

	private val innerDecrypter: Decrypter =
		val decryptionCredential = new BasicCredential(pubKey, privKey)
		val encKeyResolver = new ChainingEncryptedKeyResolver(List(
			new InlineEncryptedKeyResolver(),
			new SimpleKeyInfoReferenceEncryptedKeyResolver()
		).asJava)
		new Decrypter(new StaticKeyInfoCredentialResolver(decryptionCredential), null, encKeyResolver)

	lazy val decrypter: AssertionDecrypter = ass =>
		val xmlCipher = XMLCipher.getProviderInstance("BC")
		xmlCipher.init(XMLCipher.DECRYPT_MODE, privKey)
		try
			val decrBytes = xmlCipher.decryptToByteArray(ass.getEncryptedData().getDOM())
			println(new String(decrBytes))
		catch case err: Exception =>
			err.printStackTrace()
		innerDecrypter.decrypt(ass)

//		encrAss => {
//			val ser = new LSSerializerImpl()
//			println(ser.writeToString(encrAss.getDOM))
//			decrypter.decrypt(encrAss)
//		}


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
		for
			privKey <- readPrivateKey(kinfo.primary)
			pubKey <- Authenticator.pubKey("EC")
		yield new AssertionExtractor(pubKey, privKey)
