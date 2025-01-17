package se.lu.nateko.cp.cpauth.xmldsig

import eu.icoscp.envri.Envri
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor
import org.w3c.dom.Document
import org.xml.sax.SAXException
import se.lu.nateko.cp.cpauth.ConfigReader
import se.lu.nateko.cp.cpauth.core.Authenticator
import se.lu.nateko.cp.cpauth.core.Crypto
import se.lu.nateko.cp.cpauth.core.Crypto.KeyType
import se.lu.nateko.cp.cpauth.opensaml.Parser
import se.lu.nateko.cp.cpauth.utils.SignedTokenMaker

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.InvalidAlgorithmParameterException
import java.security.KeyException
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Collections
import javax.xml.crypto.MarshalException
import javax.xml.crypto.dsig.CanonicalizationMethod
import javax.xml.crypto.dsig.DigestMethod
import javax.xml.crypto.dsig.Reference
import javax.xml.crypto.dsig.SignatureMethod
import javax.xml.crypto.dsig.SignedInfo
import javax.xml.crypto.dsig.Transform
import javax.xml.crypto.dsig.XMLSignature
import javax.xml.crypto.dsig.XMLSignatureException
import javax.xml.crypto.dsig.XMLSignatureFactory
import javax.xml.crypto.dsig.dom.DOMSignContext
import javax.xml.crypto.dsig.keyinfo.KeyInfo
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory
import javax.xml.crypto.dsig.keyinfo.KeyValue
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec
import javax.xml.crypto.dsig.spec.TransformParameterSpec
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import scala.io.Source
import scala.util.Try
import scala.xml.PrettyPrinter
import scala.xml.XML

object SpSamlMetadataProducer:
	val keyType: KeyType = "EC"

	private def getPath(resource: String): String =
		new java.io.File("").getAbsolutePath + resource


	private def xmlFileNamePrefix(using envri: Envri) = envri match
		case Envri.ICOS | Envri.ICOSCities => "icos-cp"
		case Envri.SITES => "sites"

	def unsignedPath(using Envri) =
		getPath(s"/src/main/resources/${xmlFileNamePrefix}_sp_meta_unsigned.xml")

	def targetPath(using Envri) =
		getPath(s"/src/main/resources/${xmlFileNamePrefix}_sp_meta.xml")


	def spMetaPath(using Envri) = s"/${xmlFileNamePrefix}_sp_meta.xml"

	def getPrivKey(using envri: Envri): Try[PrivateKey] =
		ConfigReader.getDefault.flatMap: conf =>
			SignedTokenMaker.privKey(conf.auth.priv, keyType)


	def produceSignedMetadata(using Envri): Unit =

		generateXMLDigitalSignature(
			unsignedPath,
			targetPath,
			getPrivKey.get,
			Authenticator.pubKey(keyType).get
		)
		val xml = XML.loadFile(new java.io.File(targetPath))
		val pretty = new PrettyPrinter(200, 3)
		val sb = StringBuilder()
		sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="no"?>""")
		sb.append("\n")
		pretty.format(xml, sb)
		Files.write(Path.of(targetPath), sb.result().getBytes())
		println(s"Signed XML written to $targetPath")


	def parseSpMeta(using Envri) =
		val ents = Parser.fromStream[EntitiesDescriptor](getClass.getResourceAsStream(spMetaPath))
		ents


	private def getXmlDocument(xmlFilePath: String): Document =
		val dbf = DocumentBuilderFactory.newInstance()
		dbf.setNamespaceAware(true)
		dbf.newDocumentBuilder().parse(new FileInputStream(xmlFilePath))

	private def getKeyInfo(xmlSigFactory: XMLSignatureFactory, publicKey: PublicKey): KeyInfo =
		val keyInfoFact = xmlSigFactory.getKeyInfoFactory()
		val keyValue = keyInfoFact.newKeyValue(publicKey)
		keyInfoFact.newKeyInfo(Collections.singletonList(keyValue))

	private def storeSignedDoc(doc: Document, destnSignedXmlFilePath: String): Unit =
		val transFactory = TransformerFactory.newInstance()
		val trans = transFactory.newTransformer()
		val streamRes = new StreamResult(new File(destnSignedXmlFilePath))
		trans.transform(new DOMSource(doc), streamRes)


	def generateXMLDigitalSignature(
			originalXmlFilePath: String, destnSignedXmlFilePath: String,
			privateKey: PrivateKey, publicKey: PublicKey
	): Unit = {
		val doc = getXmlDocument(originalXmlFilePath)
		val domSignCtx = new DOMSignContext(privateKey, doc.getDocumentElement())
		val xmlSigFactory = XMLSignatureFactory.getInstance("DOM")

		val ref = xmlSigFactory.newReference(
			"",
			xmlSigFactory.newDigestMethod(DigestMethod.SHA256, null),
			Collections.singletonList(xmlSigFactory.newTransform(
				Transform.ENVELOPED,
				null.asInstanceOf[TransformParameterSpec])
			),
			null,
			null
		)
		val signedInfo = xmlSigFactory.newSignedInfo(
			xmlSigFactory.newCanonicalizationMethod(
				CanonicalizationMethod.INCLUSIVE,
				null.asInstanceOf[C14NMethodParameterSpec]
			),
			xmlSigFactory.newSignatureMethod(SignatureMethod.ECDSA_SHA256, null),
			Collections.singletonList(ref)
		)
		val keyInfo = getKeyInfo(xmlSigFactory, publicKey)
		val xmlSignature = xmlSigFactory.newXMLSignature(signedInfo, keyInfo)
		xmlSignature.sign(domSignCtx)
		storeSignedDoc(doc, destnSignedXmlFilePath)
	}
end SpSamlMetadataProducer
