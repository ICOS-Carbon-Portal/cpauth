package eu.icoscp.georestheart

import akka.Done
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import eu.icoscp.envri.Envri
import eu.icoscp.geoipclient.CpGeoClient
import eu.icoscp.geoipclient.GeoIpInfo
import eu.icoscp.utils.akkauri.appendPathSegment
import eu.icoscp.utils.akkauri.uriJavaToAkka
import se.lu.nateko.cp.cpauth.core.SprayJsonUtils.*
import se.lu.nateko.cp.cpauth.core.UserId
import spray.json.RootJsonFormat
import spray.json.*

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success


class RestHeartClientBase(conf: RestHeartConfig, geoClient: CpGeoClient, http: HttpExt)(using Materializer):
	import http.system.{dispatcher, log}

	private val httpCreds: Map[Envri, BasicHttpCredentials] = conf.db.flatMap{(envri, conf) =>
		for user <- conf.username; pass <- conf.password
		yield envri -> BasicHttpCredentials(user, pass)
	}

	def logPortalUsage(logEntry: JsObject, ip: String)(using Envri): Future[Done] =
		import CpGeoClient.{given RootJsonFormat[GeoIpInfo]}

		geoClient.lookup(ip)
			.map(_.toJson.asJsObject)
			.recover{
				case err: Throwable =>
					log.error(err, s"Could not fetch GeoIP information for ip $ip")
					JsObject("ip" -> JsString(ip))
			}
			.map{ipInfo => JsObject(logEntry.fields ++ ipInfo.fields)}
			.flatMap{geoRichEntry => Marshal(geoRichEntry).to[RequestEntity]}
			.flatMap{ent => singleRequest(
				HttpRequest(
					uri = conf.portalUsageCollUri,
					method = HttpMethods.POST,
					headers = Accept(MediaTypes.`application/json`) :: Nil,
					entity = ent
				)
			)}
			.map(_ => Done)


	def getUserUri(uid: UserId)(using Envri): Uri =
		conf.usersCollUri.appendPathSegment(uid.email)

	def getUserProps(uid: UserId, projections: Seq[String] = Nil)(using Envri): Future[JsObject] =
		val query = if(projections.isEmpty)
			Uri.Query.Empty
		else {
			val keys = projections.map(proj => "\"" + proj + "\": 1").mkString("{", ", ", "}")
			Uri.Query("keys" -> keys)
		}
		val uri = getUserUri(uid).withQuery(query)
		for(
			resp <- singleRequest(HttpRequest(uri = uri));
			userVal <- Unmarshal(resp.entity.withContentType(ContentTypes.`application/json`)).to[JsValue];
			userObj <- Future.fromTry(ensure[JsObject](userVal))
		) yield userObj


	protected def createDbsAndColls: Future[Done] = if conf.skipInit then Future.successful(Done) else
		Future.sequence(
			conf.db.flatMap { (envri, db) =>
				given Envri = envri
				val uri = uriJavaToAkka(db.uri)
				Seq(
					createIfNotExists("Mongo db", uri, uri),
					createCollIfNotExists(conf.portalUsageCollUri, "portal use collection"),
					createCollIfNotExists(conf.usersCollUri, "user collection")
				)
			}
		).map(_ => Done)

	protected def createCollIfNotExists(collUri: Uri, thing: String)(using Envri): Future[Done] = {
		val checkUri = collUri.withPath(collUri.path / "_indexes")
		createIfNotExists(thing, checkUri, collUri)
	}

	protected def createIfNotExists(thing: String, checkUri: Uri, putUri: Uri)(using Envri): Future[Done] =
		requestDiscardResp(HttpRequest(uri = checkUri)).flatMap{
			case StatusCodes.OK => ok

			case StatusCodes.NotFound =>
				requestDiscardResp(HttpRequest(HttpMethods.PUT, uri = putUri)).flatMap{
					case StatusCodes.Created => ok
					case status => fail(
						s"Could not create $thing at $putUri, got response $status"
					)
				}
			case status => fail(
				s"Got Unexpected status $status when trying to check if $thing exists at $checkUri"
			)
		}

	protected def requestDiscardResp(req: HttpRequest)(using Envri): Future[StatusCode] =
		singleRequest(req).map{resp =>
			resp.discardEntityBytes()
			resp.status
		}

	protected def singleRequest(req: HttpRequest)(using envri: Envri): Future[HttpResponse] =
		val credRequest = httpCreds.get(envri).fold(req)(req.addCredentials(_))
		http.singleRequest(credRequest)

	protected val ok = Future.successful(Done)

	protected def fail[T](msg: String) = Future.failed[T](new Exception(msg))

end RestHeartClientBase
