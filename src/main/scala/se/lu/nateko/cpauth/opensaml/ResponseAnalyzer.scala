package se.lu.nateko.cpauth.opensaml

import java.io.InputStream
import java.io.StringReader
import java.security.interfaces.RSAPrivateKey
import scala.collection.JavaConverters._
import org.opensaml.saml2.core.Assertion
import org.opensaml.saml2.core.EncryptedAssertion
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.encryption.Decrypter
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver
import org.opensaml.xml.schema.XSString
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver
import org.opensaml.xml.security.x509.BasicX509Credential
import se.lu.nateko.cpauth.core.Config
import se.lu.nateko.cpauth.core.CoreUtils
import se.lu.nateko.cpauth.core.Crypto
import scala.util.Try
import java.net.URI

class ResponseAnalyzer(key: RSAPrivateKey, idpLib: IdpLibrary){
	import ResponseAnalyzer._

	lazy val decrypter: AssertionDecrypter = {

		val decryptionCredential = new BasicX509Credential()
		decryptionCredential.setPrivateKey(key)

		val decrypter = new Decrypter(null, new StaticKeyInfoCredentialResolver(decryptionCredential), new InlineEncryptedKeyResolver())

		decrypter.decrypt
	}

	def extractAssertions(response: Response): Try[Seq[Assertion]] = {
		val decryptedAssertions = response.getEncryptedAssertions.asScala.map(decrypter)
		val unencryptedAssertions = response.getAssertions.asScala
		val unvalidated = unencryptedAssertions ++ decryptedAssertions
		
		getAssertionValidator(response).map{
			validator => unvalidated.filter(ass => validator.getValidationError(ass).isEmpty)
		}
	}
	
	private def getAssertionValidator(response: Response): Try[AssertionValidator] = for {
		idpId <- Try(new URI(response.getIssuer.getValue));
		idpProp <- idpLib.getIdpProps(idpId)
	} yield new AssertionValidator(idpProp.key)

}

object ResponseAnalyzer {

	type AssertionDecrypter = EncryptedAssertion => Assertion

	OpenSamlUtils.bootstrapOpenSaml()

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

	def apply(conf: Config): Try[ResponseAnalyzer] = fromPrivateKeyAt(conf.privateKeyPath, IdpLibrary.fromConfig(conf))

	def fromPrivateKeyAt(path: String, idpLib: IdpLibrary): Try[ResponseAnalyzer] = {
		val keyBytes = CoreUtils.getResourceBytes(path)
		val privateKey = Crypto.rsaPrivateFromDerBytes(keyBytes)
		privateKey.map(key => new ResponseAnalyzer(key, idpLib))
	}

}