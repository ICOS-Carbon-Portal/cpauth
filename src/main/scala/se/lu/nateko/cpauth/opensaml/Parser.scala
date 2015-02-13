package se.lu.nateko.cpauth.opensaml

import org.opensaml.saml2.core.Response
import se.lu.nateko.cpauth.core.CoreUtils
import org.opensaml.xml.parse.ParserPool
import org.opensaml.xml.parse.StaticBasicParserPool
import org.w3c.dom.Document
import java.io.StringReader
import java.io.InputStream
import org.opensaml.xml.XMLObject

object Parser {
	
	OpenSamlUtils.bootstrapOpenSaml()

	private[this] val parserPool: ParserPool = {
		val parserPool = new StaticBasicParserPool()
		parserPool.initialize()
		parserPool
	}
	
	private[this] val unmarshallerFactory = org.opensaml.xml.Configuration.getUnmarshallerFactory()
	
	def fromDocument[T <: XMLObject](doc: Document): T = {
		val metadataRoot = doc.getDocumentElement()
		val unmarshaller = unmarshallerFactory.getUnmarshaller(metadataRoot)
		unmarshaller.unmarshall(metadataRoot).asInstanceOf[T]
	}

	def fromBase64[T <: XMLObject](base64: String): T = fromString[T](CoreUtils.decode64(base64))

	def fromString[T <: XMLObject](objXml: String): T = {
		val reader = new StringReader(objXml)
		val document = parserPool.parse(reader)
		fromDocument[T](document)
	}

	def fromStream[T <: XMLObject](stream: InputStream): T = fromDocument[T](parserPool.parse(stream))

}