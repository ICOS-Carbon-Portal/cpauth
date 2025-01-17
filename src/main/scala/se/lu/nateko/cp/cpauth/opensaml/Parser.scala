package se.lu.nateko.cp.cpauth.opensaml

import se.lu.nateko.cp.cpauth.core.CoreUtils
import org.w3c.dom.Document
import java.io.StringReader
import java.io.InputStream
import org.opensaml.core.xml.XMLObject
import se.lu.nateko.cp.cpauth.utils.Utils
import net.shibboleth.utilities.java.support.xml.ParserPool
import net.shibboleth.utilities.java.support.xml.BasicParserPool
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport

object Parser {
	
	OpenSamlUtils.bootstrapOpenSaml()
	Utils.setRootLoggingLevelToInfo()

	private[this] val parserPool: ParserPool = {
		val parserPool = new BasicParserPool()
		parserPool.initialize()
		parserPool
	}

	def fromDocument[T <: XMLObject](doc: Document): T = {
		val metadataRoot = doc.getDocumentElement()
		val unmarshaller = XMLObjectProviderRegistrySupport.getUnmarshallerFactory.getUnmarshaller(metadataRoot)
		unmarshaller.unmarshall(metadataRoot).asInstanceOf[T]
	}

	def fromBase64[T <: XMLObject](base64: String): T = fromString[T](CoreUtils.decodeBase64ToString(base64))

	def fromString[T <: XMLObject](objXml: String): T = {
		val reader = new StringReader(objXml)
		val document = parserPool.parse(reader)
		fromDocument[T](document)
	}

	def fromStream[T <: XMLObject](stream: InputStream): T = fromDocument[T](parserPool.parse(stream))

}