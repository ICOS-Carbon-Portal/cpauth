package se.lu.nateko.cpauth.opensaml.test

import org.scalatest.FunSuite
import se.lu.nateko.cpauth.Utils
import se.lu.nateko.cpauth.core.CoreUtils
import org.opensaml.saml2.core.Response
import se.lu.nateko.cpauth.opensaml.Parser
import se.lu.nateko.cpauth.opensaml.ResponseStatusController
import scala.util.Try
import org.opensaml.common.SAMLException

class ResponseStatusControllerTest extends FunSuite {

	test("Unsuccessfull response is reported with SAML StatusMessage (if available)"){
		val responseStream = getClass.getResourceAsStream("/saml/univ_admissions_response.xml")
		val response: Response = Parser.fromStream[Response](responseStream)

		val success: Try[Response] = ResponseStatusController.ensureSuccess(response)

		val msg: String = intercept[SAMLException](success.get).getMessage
		assert(msg === "Required NameID format not supported")
	}

	test("If Status Message is unavailable, unsuccessfull response is reported with SAML Response XML"){
		val responseStream = getClass.getResourceAsStream("/saml/kth_response_fail_no_message.xml")
		val response: Response = Parser.fromStream[Response](responseStream)

		val success: Try[Response] = ResponseStatusController.ensureSuccess(response)

		val msg: String = intercept[SAMLException](success.get).getMessage
		assert(msg.contains("<?xml version="))
	}

	test("Successful response is passed through"){
		val response = Parser.fromStream[Response](getClass.getResourceAsStream("/saml/response_sample.xml"))

		val success: Try[Response] = ResponseStatusController.ensureSuccess(response)

		assert(success.isSuccess)
		assert(success.get.eq(response))
	}

}