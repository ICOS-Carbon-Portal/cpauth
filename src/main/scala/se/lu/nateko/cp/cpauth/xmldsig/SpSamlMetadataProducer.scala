package se.lu.nateko.cp.cpauth.xmldsig

import com.ddlab.rnd.xml.digsig.XmlDigitalSignatureGenerator
import se.lu.nateko.cp.cpauth.opensaml.Parser
import org.opensaml.saml2.metadata.EntitiesDescriptor

object SpSamlMetadataProducer {

	private def getPath(resource: String): String = {
		new java.io.File("").getAbsolutePath + resource
	}

	val spMetaPath = "/sites_sp_meta.xml"

	def produceSignedMetadata(): Unit = {
		val generator = new XmlDigitalSignatureGenerator

		generator.generateXMLDigitalSignature(
			getPath("/src/main/resources/sites_sp_meta_unsigned.xml"), //originalXmlFilePath
			getPath("/src/main/resources" + spMetaPath), //destnSignedXmlFilePath
			getPath("/src/main/resources/crypto/private/sites_private8.der"), //privateKeyFilePath
			getPath("/core/src/main/resources/cpauthCore/crypto/sites_public.der") //publicKeyFilePath
		)
	}

	def parseSpMeta = {
		val ents = Parser.fromStream[EntitiesDescriptor](getClass.getResourceAsStream(spMetaPath))
		ents
	}
}