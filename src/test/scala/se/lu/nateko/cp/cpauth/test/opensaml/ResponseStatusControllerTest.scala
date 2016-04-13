package se.lu.nateko.cp.cpauth.test.opensaml

import org.scalatest.FunSuite
import org.opensaml.saml2.core.Response
import se.lu.nateko.cp.cpauth.opensaml.Parser
import se.lu.nateko.cp.cpauth.opensaml.ResponseStatusController
import scala.util.Try
import org.opensaml.common.SAMLException

class ResponseStatusControllerTest extends FunSuite {

	def getResponse(path: String): Response = {
		val responseStream = getClass.getResourceAsStream(path)
		Parser.fromStream[Response](responseStream)
	}

	test("Unsuccessfull response is reported with SAML StatusMessage (if available)"){
		val response: Response = getResponse("/saml/univ_admissions_response.xml")

		val success: Try[Response] = ResponseStatusController.ensureSuccess(response)

		val msg: String = intercept[SAMLException](success.get).getMessage
		assert(msg === "Required NameID format not supported")
	}

	test("If Status Message is unavailable, unsuccessfull response is reported with SAML Response XML"){
		val response: Response = getResponse("/saml/kth_response_fail_no_message.xml")

		val success: Try[Response] = ResponseStatusController.ensureSuccess(response)

		val msg: String = intercept[SAMLException](success.get).getMessage
		assert(msg.contains("<?xml version="))
	}

	test("Successful response is passed through"){
		val response: Response = getResponse("/saml/response_sample.xml")

		val success: Try[Response] = ResponseStatusController.ensureSuccess(response)

		assert(success.isSuccess)
		assert(success.get.eq(response))
	}

	test("Response without an 'InResponseTo' attribute is considered unsuccessful"){
		val response: Response = getResponse("/saml/response_sample.xml")
		response.setInResponseTo(null)

		val success: Try[Response] = ResponseStatusController.ensureSuccess(response)

		assert(success.isFailure)
		val msg: String = intercept[AssertionError](success.get).getMessage
		assert(msg.contains("'InResponseTo' attribute must be present"))
	}

}