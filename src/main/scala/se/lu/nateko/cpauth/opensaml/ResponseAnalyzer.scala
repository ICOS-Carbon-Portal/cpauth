package se.lu.nateko.cpauth.opensaml

import java.io.InputStream
import java.io.StringReader
import java.security.interfaces.RSAPrivateKey

import scala.collection.JavaConverters.asScalaBufferConverter

import org.opensaml.saml2.core.Assertion
import org.opensaml.saml2.core.EncryptedAssertion
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.encryption.Decrypter
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver
import org.opensaml.xml.parse.ParserPool
import org.opensaml.xml.parse.StaticBasicParserPool
import org.opensaml.xml.schema.XSString
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver
import org.opensaml.xml.security.x509.BasicX509Credential
import org.w3c.dom.Document

class ResponseAnalyzer(key: RSAPrivateKey){
	import ResponseAnalyzer._

	lazy val decrypter: AssertionDecrypter = {
		
		val decryptionCredential = new BasicX509Credential()
		decryptionCredential.setPrivateKey(key)
		
		val decrypter = new Decrypter(null, new StaticKeyInfoCredentialResolver(decryptionCredential), new InlineEncryptedKeyResolver())

		decrypter.decrypt
	}

	def extractAssertions(responseStream: InputStream): Seq[Assertion] =
		extractAssertions(parserPool.parse(responseStream))

	def extractAssertions(response: String): Seq[Assertion] = {
		val reader = new StringReader(response)
		val document = parserPool.parse(reader)
		extractAssertions(document)
	}
	
	private def extractAssertions(responseDoc: Document): Seq[Assertion] = {
		val metadataRoot = responseDoc.getDocumentElement()
		
		val unmarshallerFactory = org.opensaml.xml.Configuration.getUnmarshallerFactory()
		val unmarshaller = unmarshallerFactory.getUnmarshaller(metadataRoot)
		val response = unmarshaller.unmarshall(metadataRoot).asInstanceOf[Response]
		
		val decryptedAssertions = response.getEncryptedAssertions.asScala.map(a => decrypter(a))
		val unencryptedAssertions = response.getAssertions.asScala
		unencryptedAssertions ++ decryptedAssertions
	}

}

object ResponseAnalyzer {

	type AssertionDecrypter = EncryptedAssertion => Assertion

	OpenSamlUtils.bootstrapOpenSaml()

	val parserPool: ParserPool = {
		val parserPool = new StaticBasicParserPool()
		parserPool.initialize()
		parserPool
	}

	def extractAttributeStringValues(assertions: Seq[Assertion]): Map[String, Seq[String]] = {
		import org.opensaml.xml.schema.XSString
		
		val attrNamesAndStringValues: Seq[(String, String)] = for(
			assertion <- assertions;
			statement <- assertion.getAttributeStatements.asScala;
			attribute <- statement.getAttributes.asScala;
			attrValue <- attribute.getAttributeValues.asScala.collect{ case s: XSString => s.getValue}
		) yield (attribute.getFriendlyName, attrValue)
		
		attrNamesAndStringValues
			.groupBy{case (name, value) => name}
			.mapValues(nameValuePairs => nameValuePairs.map{case (name, value) => value})
	}

}