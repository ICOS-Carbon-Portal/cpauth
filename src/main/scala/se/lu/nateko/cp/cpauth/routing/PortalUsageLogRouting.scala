package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.{ Directive1, MissingHeaderRejection, RejectionHandler }
import akka.http.scaladsl.server.Directives._
import se.lu.nateko.cp.cpauth.services.PortalUsageLogger
import spray.json.JsValue


trait PortalUsageLogRouting extends CpauthDirectives{

	def usageLogger: PortalUsageLogger

	val usageLogRouting = extractEnvri{implicit envri =>
		path("portaluse"){
			post{
				respondWithHeaders(`Access-Control-Allow-Origin`.*){
					getClientIp{ip =>
						entity(as[JsValue]){js =>
							usageLogger.log(js.asJsObject, ip)
							complete(StatusCodes.OK)
						}
					}
				}
			} ~
			options{
				respondWithHeaders(
					`Access-Control-Allow-Origin`.*,
					`Access-Control-Allow-Methods`(HttpMethods.POST),
					`Access-Control-Allow-Headers`("Content-Type")
				){
					complete(StatusCodes.OK)
				}
			}
		}
	}

	val getClientIp: Directive1[String] = {

		val errMsg = "Missing 'X-Real-Ip' header, bad reverse proxy configuration on the server"

		val rejHandler = RejectionHandler.newBuilder().handle{
			case MissingHeaderRejection(_) => complete((StatusCodes.BadRequest, errMsg))
		}.result()

		handleRejections(rejHandler) & headerValueByType[`X-Real-Ip`](()).map(_.value)
	}
}
