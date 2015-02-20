package se.lu.nateko.cpauth.opensaml

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.opensaml.common.SAMLException
import org.opensaml.saml2.core.Response
import org.opensaml.saml2.core.StatusCode
import se.lu.nateko.cpauth.Utils

object ResponseStatusController {

	def ensureSuccess(response: Response): Try[Response] = for(

		status <- Try(response.getStatus);

		statusCode <- Try(status.getStatusCode.getValue);

		successfullResponse <- if(StatusCode.SUCCESS_URI != statusCode){

				val msg = Try(status.getStatusMessage.getMessage).getOrElse{

					val idp = Try(response.getIssuer.getValue).getOrElse("the Identity provider")
					val responseXml = Try(":\n" + Utils.xmlToStr(response.getDOM)).getOrElse("")
					"Got a failure response from " + idp + responseXml
				}

				Failure(new SAMLException(msg))

			}else Success(response)

	) yield successfullResponse

}