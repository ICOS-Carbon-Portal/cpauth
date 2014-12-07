package se.lu.nateko.cpauth

import org.opensaml.xml.Configuration
import org.opensaml.common.SAMLObjectBuilder
import org.opensaml.saml2.core.Assertion
import org.opensaml.xml.XMLObjectBuilderFactory
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider
import java.util.Timer
import org.apache.commons.httpclient.HttpClient
import akka.actor.ActorSystem
import spray.http._
import spray.client.pipelining._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import org.opensaml.xml.parse.StaticBasicParserPool
import org.opensaml.xml.parse.ParserPool
import org.opensaml.DefaultBootstrap
import org.opensaml.xml.XMLObject
import org.opensaml.saml2.metadata.provider.MetadataProvider
import org.opensaml.saml2.core.EncryptedAssertion
import java.security.spec.PKCS8EncodedKeySpec
import java.security.interfaces.RSAPrivateKey
import java.security.KeyFactory
import org.opensaml.xml.security.x509.BasicX509Credential
import org.opensaml.saml2.encryption.Decrypter
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver
import org.opensaml.saml2.core.Response

object OpenSaml{

	def setLoggingLevel(): Unit = {
		import org.slf4j.LoggerFactory;
		import ch.qos.logback.classic.{Level, Logger};

		LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
			.asInstanceOf[Logger]
			.setLevel(Level.INFO);
	}

	setLoggingLevel()

	//val idpUrl = "https://idp.lu.se/idp/shibboleth"
	val idpUrl = "https://idp.testshib.org/idp/shibboleth"
	DefaultBootstrap.bootstrap()

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
	
	def getIdpMeta: MetadataProvider = {
		val metaProvider = new HTTPMetadataProvider(new Timer(), new HttpClient(), idpUrl)
		metaProvider.setParserPool(parserPool)
		metaProvider.initialize()
		metaProvider
	}	
	
	// Get the builder factory
	//val builderFactory = Configuration.getSAML2ArtifactBuilderFactory()
 
	// Get the assertion builder based on the assertion element name
	//val builder = builderFactory.getArtifactBuilder(Assertion.DEFAULT_ELEMENT_NAME).asInstanceOf[SAMLObjectBuilder[Assertion]]
 
	// Create the assertion
	//val assertion = builder.buildObject()
	
	def extractAssertion: Assertion = {
		import java.io._
		import java.nio.file.{Files, Paths}

		val responseStream = getClass.getResourceAsStream("/response_sample.xml")
		val document = parserPool.parse(responseStream)
		val metadataRoot = document.getDocumentElement()
		
		// Unmarshall
		val unmarshallerFactory = org.opensaml.xml.Configuration.getUnmarshallerFactory()
		val unmarshaller = unmarshallerFactory.getUnmarshaller(metadataRoot)
		val response = unmarshaller.unmarshall(metadataRoot).asInstanceOf[Response]
		val encryptedAssertion = response.getEncryptedAssertions.get(0)
		
		val encodedPrivateKey = Files.readAllBytes(Paths.get(getClass.getResource("/private_key.pk8").toURI))
		
		// Create the private key.
		val privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey)
		val privateKey = KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec).asInstanceOf[RSAPrivateKey]
		
		// Create the credentials.
		val decryptionCredential = new BasicX509Credential()
		decryptionCredential.setPrivateKey(privateKey)
		
		// Create a decrypter.
		val decrypter = new Decrypter(null, new StaticKeyInfoCredentialResolver(decryptionCredential), new InlineEncryptedKeyResolver())
		
		decrypter.decrypt(encryptedAssertion)
	}
}
