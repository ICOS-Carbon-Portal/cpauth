package se.lu.nateko.cp.cpauth.opensaml

import org.opensaml.saml2.core.Assertion
import se.lu.nateko.cp.cpauth.Utils.SafeJavaCollectionWrapper
import org.opensaml.xml.schema.XSString
import org.opensaml.saml2.core.Attribute
import scala.util.Try
import scala.util.Failure
import scala.util.control.NoStackTrace
import scala.util.Success

class AllStatements(nameValues: Map[String, Seq[String]]){

	def getSingleValue(attribute: String): Try[String] = {

		nameValues.get(attribute).map(_.toList) match {

			case None | Some(Nil) => fail(s"Attribute '$attribute' not available")

			case Some(vals) if vals.size > 1 => fail(s"Attribute '$attribute' had multiple values")

			case Some(theOnly :: Nil) => Success(theOnly)
		}
	}
	
	private def fail(msg: String) = Failure(new Exception(msg) with NoStackTrace)
}

object StatementExtractor {

	def extractAttributeStringValues(assertions: Iterable[ValidatedAssertion]): AllStatements = {
		val nameValues = assertions.collect{
			case ValidatedAssertion(validated, None) => validated
		}.flatMap(extractAttributeStringValues)
			.groupBy{case (name, value) => name}
			.mapValues(nameValuePairs => nameValuePairs.map{case (name, value) => value}.toSeq)

		new AllStatements(nameValues)
	}

	def extractAttributeStringValues(assertion: Assertion): Iterable[(String, String)] = for(
		statement <- assertion.getAttributeStatements.toSafeIterable;
		attribute <- statement.getAttributes.toSafeIterable;
		name = getName(attribute);
		attrValue <- getStringValues(attribute)
	) yield (name, attrValue)


	def getName(attribute: Attribute): String = eitherOr(attribute.getFriendlyName, attribute.getName)
	
	def getStringValues(attribute: Attribute): Iterable[String] =
		attribute.getAttributeValues.toSafeIterable.collect{ case s: XSString => s.getValue}

	def eitherOr(opt1: String, opt2: String): String = if(opt1 == null || opt1.length == 0) opt2 else opt1

}