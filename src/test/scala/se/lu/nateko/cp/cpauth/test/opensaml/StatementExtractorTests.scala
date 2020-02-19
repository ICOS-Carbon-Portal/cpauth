package se.lu.nateko.cp.cpauth.test.opensaml

import org.opensaml.saml2.core.Assertion
import org.scalatest.funsuite.AnyFunSuite

import se.lu.nateko.cp.cpauth.opensaml.Parser
import se.lu.nateko.cp.cpauth.opensaml.StatementExtractor

class StatementExtractorTests extends AnyFunSuite{

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

	test("assertion from unitus.it is parsed correctly"){
		val stream = getClass.getResourceAsStream("/saml/unitus_it_assertion.xml")
		val assertion = Parser.fromStream[Assertion](stream)
		val stats = StatementExtractor.extractAttributeStringValues(assertion).toMap
		assert(stats("mail") === "nickname@unitus.it")
	}
}
