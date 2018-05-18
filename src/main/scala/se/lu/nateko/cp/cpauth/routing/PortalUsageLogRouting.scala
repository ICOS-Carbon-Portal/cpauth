package se.lu.nateko.cp.cpauth.routing

import se.lu.nateko.cp.cpauth.services.PortalUsageLogger
import akka.http.scaladsl.server.Directives._
import spray.json.JsValue
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.`X-Real-Ip`
import akka.http.scaladsl.server.{Directive1, MissingHeaderRejection, RejectionHandler}


trait PortalUsageLogRouting extends CpauthDirectives{

	def usageLogger: PortalUsageLogger

	val usageLogRouting = extractEnvri{implicit envri =>
		(post & path("portaluse")){
			getClientIp{ip =>
				entity(as[JsValue]){js =>
					usageLogger.log(js.asJsObject, ip)
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
