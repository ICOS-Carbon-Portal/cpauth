package se.lu.nateko.cpauth

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Timer
import scala.annotation.migration
import scala.collection.JavaConverters.asScalaBufferConverter
import org.apache.commons.httpclient.HttpClient
import org.opensaml.saml2.core.Assertion
import org.opensaml.saml2.core.EncryptedAssertion
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.encryption.Decrypter
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider
import org.opensaml.saml2.metadata.provider.MetadataProvider
import org.opensaml.xml.XMLObject
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver
import org.opensaml.xml.parse.ParserPool
import org.opensaml.xml.parse.StaticBasicParserPool
import org.opensaml.xml.schema.XSString
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver
import org.opensaml.xml.security.x509.BasicX509Credential
import org.slf4j.LoggerFactory
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.w3c.dom.ls.DOMImplementationLS
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import se.lu.nateko.cpauth.core.PKCS8EncodedKey

object OpenSaml{

  type AssertionDecryptor = EncryptedAssertion => Assertion

	def setLoggingLevel(): Unit = {
		import org.slf4j.LoggerFactory
		import ch.qos.logback.classic.{Level, Logger}

		LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
			.asInstanceOf[Logger]
			.setLevel(Level.INFO)
	}

	setLoggingLevel()

	//val idpUrl = "https://idp.lu.se/idp/shibboleth"
	val idpUrl = "https://idp.testshib.org/idp/shibboleth"
	org.opensaml.DefaultBootstrap.bootstrap()

//	implicit val system = ActorSystem("samltests")
//	import system.dispatcher
//
//	val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//	
//	def getIdpMeta: String = {
//		val response: Future[String] = pipeline(Get(idpUrl)).map(_.entity.asString)
//		Await.result(response, 3.seconds)
//	}
	
	val parserPool: ParserPool = {
		val parserPool = new StaticBasicParserPool()
		parserPool.initialize()
		parserPool
	}
	
//	def getIdpMeta: MetadataProvider = {
//		val metaProvider = new HTTPMetadataProvider(new Timer(), new HttpClient(), idpUrl)
//		metaProvider.setParserPool(parserPool)
//		metaProvider.initialize()
//		metaProvider
//	}	
	
	
	val encodedPrivateKey: Array[Byte] = Utils.getResourceBytes("/private_key.pk8")

	import java.io.InputStream
	def getResponseStream: InputStream = getClass.getResourceAsStream("/response_sample.xml")
	
	def extractAssertions(responseStream: InputStream, key: PKCS8EncodedKey): Seq[Assertion] = {
		val document = parserPool.parse(responseStream)
		val metadataRoot = document.getDocumentElement()
		
		val unmarshallerFactory = org.opensaml.xml.Configuration.getUnmarshallerFactory()
		val unmarshaller = unmarshallerFactory.getUnmarshaller(metadataRoot)
		val response = unmarshaller.unmarshall(metadataRoot).asInstanceOf[Response]
		
		lazy val decryptor = getAssDecryptor(key)
    val lazyDecryptor: AssertionDecryptor = decryptor(_)
		response.getEncryptedAssertions.asScala.map(lazyDecryptor) ++ response.getAssertions.asScala
	}
	
	def getAssDecryptor(key: PKCS8EncodedKey): AssertionDecryptor = {
		val privateKeySpec = new PKCS8EncodedKeySpec(key.bytes)
		val privateKey = KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec).asInstanceOf[RSAPrivateKey]
		
		val decryptionCredential = new BasicX509Credential()
		decryptionCredential.setPrivateKey(privateKey)
		
		val decrypter = new Decrypter(null, new StaticKeyInfoCredentialResolver(decryptionCredential), new InlineEncryptedKeyResolver())
		
		decrypter.decrypt
	}
	
	
	val asses = extractAssertions(getResponseStream).toIndexedSeq
	
	def extractAttributes(ass: Assertion): Map[String, Seq[String]] = {
		import org.opensaml.xml.schema.XSString
		
		val attributes = ass.getAttributeStatements.asScala.flatMap(_.getAttributes.asScala)
		
		val namesVsValues: Seq[(String, Seq[String])] = for{
			attr <- attributes;
			attrVals = attr.getAttributeValues.asScala.collect{ case s: XSString => s.getValue}
		} yield (attr.getFriendlyName, attrVals)
		
		Map(namesVsValues: _*)
	}
}
