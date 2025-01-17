package se.lu.nateko.cp.cpauth.opensaml

import eu.icoscp.envri.Envri
import org.opensaml.saml.saml2.core.Assertion
import org.opensaml.saml.saml2.core.EncryptedAssertion
import org.opensaml.saml.saml2.core.Response
import org.opensaml.saml.saml2.encryption.Decrypter
import org.opensaml.saml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver
import org.opensaml.security.credential.BasicCredential
import org.opensaml.xmlsec.encryption.EncryptedData
import org.opensaml.xmlsec.encryption.EncryptedKey
import org.opensaml.xmlsec.encryption.support.ChainingEncryptedKeyResolver
import org.opensaml.xmlsec.encryption.support.EncryptedKeyResolver
import org.opensaml.xmlsec.encryption.support.InlineEncryptedKeyResolver
import org.opensaml.xmlsec.encryption.support.SimpleKeyInfoReferenceEncryptedKeyResolver
import org.opensaml.xmlsec.encryption.support.SimpleRetrievalMethodEncryptedKeyResolver
import org.opensaml.xmlsec.keyinfo.impl.ChainingKeyInfoCredentialResolver
import org.opensaml.xmlsec.keyinfo.impl.KeyInfoProvider
import org.opensaml.xmlsec.keyinfo.impl.LocalKeyInfoCredentialResolver
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver
import org.opensaml.xmlsec.keyinfo.impl.provider.AgreementMethodKeyInfoProvider
import org.opensaml.xmlsec.keyinfo.impl.provider.DEREncodedKeyValueProvider
import org.opensaml.xmlsec.keyinfo.impl.provider.DSAKeyValueProvider
import org.opensaml.xmlsec.keyinfo.impl.provider.ECKeyValueProvider
import org.opensaml.xmlsec.keyinfo.impl.provider.InlineX509DataProvider
import org.opensaml.xmlsec.keyinfo.impl.provider.KeyInfoReferenceProvider
import org.opensaml.xmlsec.keyinfo.impl.provider.RSAKeyValueProvider
import se.lu.nateko.cp.cpauth.PrivateKeyInfo
import se.lu.nateko.cp.cpauth.SamlConfig
import se.lu.nateko.cp.cpauth.core.Authenticator
import se.lu.nateko.cp.cpauth.core.Crypto
import se.lu.nateko.cp.cpauth.utils.Utils.SafeJavaCollectionWrapper

import java.nio.file.Files
import java.nio.file.Paths
import java.security.PrivateKey
import java.security.PublicKey
import java.util as ju
import scala.jdk.CollectionConverters.*
import scala.util.Try

class AssertionExtractor(pubKey: PublicKey, privKey: PrivateKey){
	import AssertionExtractor.*

	private val innerDecrypter: Decrypter =
		val decryptionCredential = new BasicCredential(pubKey, privKey)

		val keyInfoProviders = Seq[KeyInfoProvider](
			new AgreementMethodKeyInfoProvider(),
			new RSAKeyValueProvider(),
			new ECKeyValueProvider(),
			new DSAKeyValueProvider(),
			new DEREncodedKeyValueProvider(),
			new InlineX509DataProvider(),
			new KeyInfoReferenceProvider()
		).asJava
		val staticKeyInfoResolver = new StaticKeyInfoCredentialResolver(decryptionCredential)
		val keyResolver = new ChainingKeyInfoCredentialResolver(Seq(
			new LocalKeyInfoCredentialResolver(keyInfoProviders, staticKeyInfoResolver),
			staticKeyInfoResolver
		).asJava)
		val encKeyResolver = new ChainingEncryptedKeyResolver(Seq(
			new InlineEncryptedKeyResolver(),
			new EncryptedElementTypeEncryptedKeyResolver(),
			new SimpleRetrievalMethodEncryptedKeyResolver(),
			new SimpleKeyInfoReferenceEncryptedKeyResolver()
		).asJava)
		//val debugEncRes = new DebuggingEncKeyResolver(encKeyResolver)
		val decr = new Decrypter(keyResolver, keyResolver, encKeyResolver)
		decr.setRootInNewDocument(true)
		decr

	lazy val decrypter: AssertionDecrypter = ass =>
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


// class DebuggingEncKeyResolver(inner: EncryptedKeyResolver) extends EncryptedKeyResolver:
// 	override def getRecipients(): ju.Set[String] = inner.getRecipients()

// 	override def resolve(encryptedData: EncryptedData): java.lang.Iterable[EncryptedKey] = {
// 		val res = inner.resolve(encryptedData)
// 		val keys = res.asScala.toIndexedSeq
// 		println(s"Resolved encrypted keys: ${keys.size}")
// 		keys.foreach(println)
// 		println(s"Recipients: ${getRecipients().asScala.toIndexedSeq}")
// 		res
// 	}
