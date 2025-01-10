package se.lu.nateko.cp.cpauth.xmldsig

import com.ddlab.rnd.xml.digsig.XmlDigitalSignatureGenerator
import se.lu.nateko.cp.cpauth.opensaml.Parser
import org.opensaml.saml2.metadata.EntitiesDescriptor
import scala.xml.XML
import scala.xml.PrettyPrinter
import java.nio.file.Files
import java.nio.file.Path

object SpSamlMetadataProducer {

	private def getPath(resource: String): String = {
		new java.io.File("").getAbsolutePath + resource
	}

	val spMetaPath = "/icos-cp_sp_meta.xml"
	//val spMetaPath = "/sites_sp_meta.xml"

	def produceSignedMetadata(): Unit = {
		val generator = new XmlDigitalSignatureGenerator
		val targetPath = getPath("/src/main/resources" + spMetaPath)

		generator.generateXMLDigitalSignature(
			getPath("/src/main/resources/icos-cp_sp_meta_unsigned.xml"), //originalXmlFilePath
			//getPath("/src/main/resources/sites_sp_meta_unsigned.xml"), //originalXmlFilePath
			targetPath, //destnSignedXmlFilePath
			getPath("/privateKeys/cpauth_private.der"), //privateKeyFilePath
			//getPath("/privateKeys/sites_private.der"), //privateKeyFilePath
			getPath("/core/src/main/resources/cpauthCore/crypto/cpauth_public.der") //publicKeyFilePath
			//getPath("/core/src/main/resources/cpauthCore/crypto/sites_public.der") //publicKeyFilePath
		)
		val xml = XML.loadFile(new java.io.File(targetPath))
		val pretty = new PrettyPrinter(200, 3)
		val sb = StringBuilder()
		sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="no"?>""")
		sb.append("\n")
		pretty.format(xml, sb)
		Files.write(Path.of(targetPath), sb.result().getBytes())
		//XML.save(targetPath, xml, xmlDecl = true)
	}

	def parseSpMeta = {
		val ents = Parser.fromStream[EntitiesDescriptor](getClass.getResourceAsStream(spMetaPath))
		ents
	}
}