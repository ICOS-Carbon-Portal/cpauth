package se.lu.nateko.cp.cpauth.test.opensaml

import org.opensaml.saml2.core.Assertion
import org.scalatest.FunSuite
import scala.collection.JavaConverters._

import se.lu.nateko.cp.cpauth.opensaml.Parser
import se.lu.nateko.cp.cpauth.opensaml.StatementExtractor

class StatementExtractorTests extends FunSuite{

	private val assertion: Assertion = {
		val stream = getClass.getResourceAsStream("/saml/gu_response_assertion.xml")
		Parser.fromStream[Assertion](stream)
	}

	test("correctly extracts all names from an attribute"){
		val snAttr = assertion.getAttributeStatements.get(0).getAttributes.get(4)
		val names = StatementExtractor.getNames(snAttr).distinct

		assert(names === Seq("givenName", "urn:oid:2.5.4.42"))
	}

	test("correctly extracts attribute string values from an assertion"){
		val attrVals = StatementExtractor.extractAttributeStringValues(assertion).toSeq
		assert(attrVals.contains(("givenName", "FirstName")))
	}
}