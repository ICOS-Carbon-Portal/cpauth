package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.{Directive1, Directive0, MissingHeaderRejection, RejectionHandler, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.MalformedRequestContentRejection
import se.lu.nateko.cp.cpauth.services.PortalLogger
import spray.json.JsValue
import se.lu.nateko.cp.cpauth.core.DownloadEventInfo


trait PortalLogRouting extends CpauthDirectives{

	def portalLogger: PortalLogger

	val portalLogRoute = extractEnvri{ implicit envri =>
		pathPrefix("logs"){
			path("portaluse"){
				post{
					respondWithHeaders(`Access-Control-Allow-Origin`.*){
						getClientIp{ip =>
							entity(as[JsValue]){js =>
								portalLogger.logUsage(js.asJsObject, ip)
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
				} ~
				complete(StatusCodes.BadRequest -> "Expecting only HTTP POST or OPTIONS on this path")
			} ~
			path("downloads"){
				(post & failOnParsingErrors("Expected DownloadEventInfo, got something else")){
					entity(as[DownloadEventInfo]){dlInfo =>
						portalLogger.logDl(dlInfo)
						complete(StatusCodes.OK)
					}
				} ~
				complete(StatusCodes.BadRequest -> "Expecting only HTTP POST on this path")
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

	def failOnParsingErrors(msg: String): Directive0 = {
		val rejHandler = RejectionHandler.newBuilder().handle{
			case MalformedRequestContentRejection(error, _) => complete((StatusCodes.BadRequest, s"$msg : $error"))
		}.result()
		handleRejections(rejHandler)
	}
}
