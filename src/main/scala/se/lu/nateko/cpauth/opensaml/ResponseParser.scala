package se.lu.nateko.cpauth.opensaml

import org.opensaml.saml2.core.Response
import se.lu.nateko.cpauth.core.CoreUtils
import org.opensaml.xml.parse.ParserPool
import org.opensaml.xml.parse.StaticBasicParserPool
import org.w3c.dom.Document
import java.io.StringReader
import java.io.InputStream

class ResponseParser {

	import ResponseParser._

	def fromBase64(base64: String): Response = fromString(CoreUtils.decode64(base64))

	def fromString(response: String): Response = {
		val reader = new StringReader(response)
		val document = parserPool.parse(reader)
		fromDocument(document)
	}

	def fromStream(responseStream: InputStream): Response = fromDocument(parserPool.parse(responseStream))

	private val unmarshallerFactory = org.opensaml.xml.Configuration.getUnmarshallerFactory()

	private def fromDocument(responseDoc: Document): Response = {
		val metadataRoot = responseDoc.getDocumentElement()
		val unmarshaller = unmarshallerFactory.getUnmarshaller(metadataRoot)
		unmarshaller.unmarshall(metadataRoot).asInstanceOf[Response]
	}


}

object ResponseParser {
	OpenSamlUtils.bootstrapOpenSaml()

	val parserPool: ParserPool = {
		val parserPool = new StaticBasicParserPool()
		parserPool.initialize()
		parserPool
	}

	def apply() = new ResponseParser

}