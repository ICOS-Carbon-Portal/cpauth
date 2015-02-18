package se.lu.nateko.cpauth.opensaml

import org.opensaml.saml2.core.Assertion
import se.lu.nateko.cpauth.Utils.SafeJavaCollectionWrapper
import org.opensaml.xml.schema.XSString
import org.opensaml.saml2.core.Attribute

object StatementExtractor {

	def extractAttributeStringValues(assertions: Iterable[Assertion]): Map[String, Seq[String]] =
		assertions.flatMap(extractAttributeStringValues)
			.groupBy{case (name, value) => name}
			.mapValues(nameValuePairs => nameValuePairs.map{case (name, value) => value}.toSeq)


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