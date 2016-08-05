package se.lu.nateko.cp.cpauth

import org.opensaml.saml2.core.Response
import se.lu.nateko.cp.cpauth.opensaml.AssertionValidator
import se.lu.nateko.cp.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cp.cpauth.opensaml.AssertionExtractor._
import scala.util.Try
import se.lu.nateko.cp.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cp.cpauth.opensaml.ValidatedAssertion
import se.lu.nateko.cp.cpauth.opensaml.StatementExtractor
import org.opensaml.saml2.core.Assertion
import se.lu.nateko.cp.cpauth.opensaml.Parser
import se.lu.nateko.cp.cpauth.core.CoreUtils
import se.lu.nateko.cp.cpauth.opensaml.ResponseStatusController
import org.opensaml.xml.XMLObject
import scala.collection.JavaConverters.asScalaBufferConverter
import se.lu.nateko.cp.cpauth.core.UserInfo

object Playground {

	Utils.setRootLoggingLevelToInfo()

	def getResponseSummary(response: Response, extractorTry: Try[AssertionExtractor], idpLib: IdpLibrary): Try[String] = for(
		goodResponse <- ResponseStatusController.ensureSuccess(response);
		validator <- AssertionValidator(goodResponse, idpLib);
		extractor <- extractorTry
	) yield {
			extractor.extractAssertions(goodResponse).map(validator.validate)
				.flatMap(getAssertionSummary).toSeq.sortBy(s => s).mkString("\n")
	}

	private def getAssertionSummary(validated: ValidatedAssertion): Iterable[String] = {
		val validityInfo = validated.error match{
			case None => "Signature OK"
			case Some(error) => "*** " + error
		}
		StatementExtractor.extractAttributeStringValues(validated.assertion).map{
			case (name, value) => s"$name: $value\t$validityInfo"
		}
	}

	def extractClasses(xmlObj: XMLObject): Seq[Class[_]] = {
		if(xmlObj == null)
			Nil
		else if(xmlObj.hasChildren)
			xmlObj.getOrderedChildren.asScala.flatMap(extractClasses)
		else
			Seq(xmlObj.getClass)
	}

	def makeLongLifeCookie(fname: String, lname: String, email: String): String = {
		val validity = 3600 * 24 * 365 * 30 //30 years in seconds
		val defConf = ConfigReader.getDefault
		val privAuthConf = defConf.auth.priv.copy(authTokenValiditySeconds = validity)
		val conf = defConf.copy(auth = defConf.auth.copy(priv = privAuthConf))
		val factory = new CookieFactory(conf)
		val uinfo = UserInfo(givenName = fname, surname = lname, mail = email)
		factory.makeAuthenticationCookie(uinfo).get.value
	}
}