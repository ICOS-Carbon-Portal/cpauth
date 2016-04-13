package se.lu.nateko.cp.cpauth.opensaml

import org.opensaml.saml2.core.Assertion
import se.lu.nateko.cp.cpauth.Utils.SafeJavaCollectionWrapper
import org.opensaml.xml.schema.XSString
import org.opensaml.saml2.core.Attribute
import scala.util.Try
import scala.util.Failure
import scala.util.control.NoStackTrace
import scala.util.Success
import org.slf4j.LoggerFactory

class AllStatements(nameValues: Map[String, Seq[String]]){

	def getSingleValue(attributes: Seq[String]): Try[String] =
		attributes.flatMap(attr => nameValues.get(attr).toSeq.flatten).distinct.toList match {

			case Nil => fail(s"Attribute(s) '${attributes.mkString("', '")}' not available")

			case theOnly :: Nil => Success(theOnly)

			case vals =>
				fail(s"Attribute(s) '${attributes.mkString("', '")}' had contradicting values " + vals.mkString(", "))

		}

	private def fail(msg: String) = Failure(new Exception(msg) with NoStackTrace)
}

object StatementExtractor {

	private[this] val log = LoggerFactory.getLogger(getClass)

	def extractAttributeStringValues(assertions: Iterable[ValidatedAssertion]): AllStatements = {
		val nameValues = assertions.collect{
			case ValidatedAssertion(validated, None) => validated
			case ValidatedAssertion(validated, Some(validationError)) =>
				log.warn("Assertion validation error: " + validationError)
				validated
		}.flatMap(extractAttributeStringValues)
			.groupBy{case (name, value) => name}
			.mapValues(nameValuePairs => nameValuePairs.map{case (name, value) => value}.toSeq)

		new AllStatements(nameValues)
	}

	def extractAttributeStringValues(assertion: Assertion): Iterable[(String, String)] = for(
		statement <- assertion.getAttributeStatements.toSafeIterable;
		attribute <- statement.getAttributes.toSafeIterable;
		name <- getNames(attribute);
		attrValue <- getStringValues(attribute)
	) yield (name, attrValue)


	def getNames(attribute: Attribute): Seq[String] = Seq(attribute.getFriendlyName, attribute.getName)
		.filter(s => s != null && s.length != 0)
	
	def getStringValues(attribute: Attribute): Iterable[String] =
		attribute.getAttributeValues.toSafeIterable.collect{ case s: XSString => s.getValue}

}
