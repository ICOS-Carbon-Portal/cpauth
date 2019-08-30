package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.{Directive1, MissingHeaderRejection, RejectionHandler, Route}
import akka.http.scaladsl.server.Directives._
import se.lu.nateko.cp.cpauth.services.PortalLoggerFactory
import spray.json.JsValue


trait PortalLogRouting extends CpauthDirectives{

	def loggerFactory: PortalLoggerFactory

	lazy val usageLogger = loggerFactory.usage
	lazy val downloadLoggers = Map(
		"dobjdls" -> loggerFactory.objectDownloads,
		"colldls" -> loggerFactory.collDownloads
	)

	val portalLogRoute = extractEnvri{ implicit envri =>
		pathPrefix("logs"){
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
				} ~
				complete(StatusCodes.BadRequest -> "Expecting only HTTP POST or OPTIONS on this path")
			} ~
			path(Segment){loggerKey =>
				downloadLoggers.get(loggerKey).fold[Route](reject){dowloadLogger =>
					post{
						entity(as[JsValue]){js =>
							dowloadLogger.log(js.asJsObject)
							complete(StatusCodes.OK)
						}
					} ~
					complete(StatusCodes.BadRequest -> "Expecting only HTTP POST on this path")
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
