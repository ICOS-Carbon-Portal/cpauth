package se.lu.nateko.cpauth

import org.opensaml.saml2.core.Response
import se.lu.nateko.cpauth.opensaml.AssertionValidator
import se.lu.nateko.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cpauth.opensaml.AssertionExtractor._
import scala.util.Try
import se.lu.nateko.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cpauth.opensaml.ValidatedAssertion
import se.lu.nateko.cpauth.opensaml.StatementExtractor
import org.opensaml.saml2.core.Assertion
import se.lu.nateko.cpauth.opensaml.Parser
import se.lu.nateko.cpauth.core.CoreUtils
import se.lu.nateko.cpauth.opensaml.ResponseStatusController
import org.opensaml.xml.XMLObject
import scala.collection.JavaConverters.asScalaBufferConverter

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

}