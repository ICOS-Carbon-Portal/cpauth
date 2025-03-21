package se.lu.nateko.cp.cpauth.opensaml

import org.opensaml.saml.saml2.core.Assertion
import se.lu.nateko.cp.cpauth.utils.Utils.toSafeIterable
import org.opensaml.core.xml.schema.XSAny
import org.opensaml.core.xml.schema.XSString
import org.opensaml.saml.saml2.core.Attribute
import scala.util.Try
import scala.util.Failure
import scala.util.control.NoStackTrace
import scala.util.Success
import org.slf4j.LoggerFactory

class AllStatements(nameValues: Map[String, Seq[String]]){

	import StatementExtractor.fail

	def getSingleValue(attributes: Seq[String]): Try[String] =
		attributes.flatMap(attr => nameValues.get(attr).toSeq.flatten).distinct.toList match {

			case Nil => fail(s"Attribute(s) '${attributes.mkString("', '")}' not available. Available attibutes:\n$contentDebugInfo\n")

			case theOnly :: Nil => Success(theOnly)

			case vals =>
				fail(s"Attribute(s) '${attributes.mkString("', '")}' had contradicting values " + vals.mkString(", "))

		}

	private def contentDebugInfo: String = nameValues.iterator.map{
			case (name, vals) => name + ": " + vals.mkString(", ")
		}.mkString("\n")
}

object StatementExtractor {

	private[this] val log = LoggerFactory.getLogger(getClass)

	def fail(msg: String) = Failure(new Exception(msg) with NoStackTrace)

	def extractAttributeStringValues(assertions: Iterable[ValidatedAssertion]): Try[AllStatements] = Try{
		val nameValues: Map[String, Seq[String]] = assertions.toSeq.collect{
			case ValidatedAssertion(validated, None) => validated
			case ValidatedAssertion(_, Some(validationError)) =>
				val s = "Assertion validation error: " + validationError
				log.warn(s)
				throw new Exception(s) with NoStackTrace
		}.flatMap(extractAttributeStringValues)
			.groupMap(_._1)(_._2)

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
		attribute.getAttributeValues.toSafeIterable.collect{
			case s: XSString => s.getValue
			case xsAny: XSAny => xsAny.getTextContent
		}

}
