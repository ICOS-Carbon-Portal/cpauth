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
import org.opensaml.DefaultBootstrap
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

object OpenSaml{

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
	
	val encodedPrivateKey: Array[Byte] = {
		import java.nio.file.{Files, Paths}
		Files.readAllBytes(Paths.get(getClass.getResource("/private_key.pk8").toURI))
	}
	import java.io.InputStream
	def getResponseStream: InputStream = getClass.getResourceAsStream("/response_sample.xml")
	
	def extractAssertions(responseStream: InputStream): Seq[Assertion] = {
		// Parse the response XML
		val document = parserPool.parse(responseStream)
		val metadataRoot = document.getDocumentElement()
		
		// Unmarshall
		val unmarshallerFactory = org.opensaml.xml.Configuration.getUnmarshallerFactory()
		val unmarshaller = unmarshallerFactory.getUnmarshaller(metadataRoot)
		val response = unmarshaller.unmarshall(metadataRoot).asInstanceOf[Response]
		
		val decryptor = getAssDecryptor(encodedPrivateKey) 
		response.getEncryptedAssertions.asScala.map(decryptor) ++ response.getAssertions.asScala
	}
	
	def getAssDecryptor(encodedPk8Key: Array[Byte]): EncryptedAssertion => Assertion = {
		// Create the private key.
		val privateKeySpec = new PKCS8EncodedKeySpec(encodedPk8Key)
		val privateKey = KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec).asInstanceOf[RSAPrivateKey]
		
		// Create the credentials.
		val decryptionCredential = new BasicX509Credential()
		decryptionCredential.setPrivateKey(privateKey)
		
		// Create a decrypter.
		val decrypter = new Decrypter(null, new StaticKeyInfoCredentialResolver(decryptionCredential), new InlineEncryptedKeyResolver())
		
		decrypter.decrypt
	}
	
	
	def extractClasses(xmlObj: XMLObject): Seq[Class[_]] = {
		if(xmlObj == null)
			Nil
		else if(xmlObj.hasChildren)
			xmlObj.getOrderedChildren.asScala.flatMap(extractClasses)
		else
			Seq(xmlObj.getClass)
	}
	
	val domSerializer: org.w3c.dom.ls.LSSerializer = {
		import  org.w3c.dom.bootstrap.DOMImplementationRegistry
		import  org.w3c.dom.ls.DOMImplementationLS
		
		val registry = DOMImplementationRegistry.newInstance()
		
		val domImpl = registry.getDOMImplementation("LS").asInstanceOf[DOMImplementationLS]
		domImpl.createLSSerializer()
	}
	
	def xmlToStr(xml: org.w3c.dom.Element): String = domSerializer.writeToString(xml)
	
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
