package se.lu.nateko.cp.cpauth.oauth

import scala.concurrent.Future
import se.lu.nateko.cp.cpauth.OAuthProviderConfig
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model.HttpCharsets
import akka.stream.Materializer
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import se.lu.nateko.cp.cpauth.utils.SprayJsonUtils
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.MediaTypes


class OrcidAuthenticationService(config: OAuthProviderConfig)(implicit system: ActorSystem, mat: Materializer) {

	private class BasicUserInfo(val orcidId: String, val accessToken: String, val name: String)
	import system.dispatcher

	private val http = Http(system)
	private val nameRegex = """(\w+)\s+(\.+)""".r

	def retrieveUserInfo(singleUseCode: String): Future[UserInfo] = {
		for(
			bui <- getBasicInfo(singleUseCode);
			emails <- getUserEmails(bui)
		) yield {
			val (givenName, surname) = bui.name match {
				case nameRegex(gn, sn) => (gn, sn)
				case _ => ("", "")
			}
			new UserInfo(givenName, surname, emails.headOption.getOrElse(""))
		}
	}

	private def getBasicInfo(singleUseCode: String): Future[BasicUserInfo] = {

		val request = HttpRequest(
			uri = "https://orcid.org/oauth/token",
			method = HttpMethods.POST,
			headers = Accept(MediaTypes.`application/json`) :: Nil,
			entity = FormData(
				"client_secret" -> config.clientSecret,
				"client_id" -> config.clientId,
				"grant_type" -> "authorization_code",
				"redirect_uri" -> config.redirectPath,
				"code" -> singleUseCode
			).toEntity(HttpCharsets.`UTF-8`)
		)

		http.singleRequest(request)
			.flatMap(resp => Unmarshal(resp.entity).to[JsValue])
			.flatMap{jsv =>

				import SprayJsonUtils._

				val basicInfoTry: Try[BasicUserInfo] = for(
					jso <- ensureObject(jsv);
					orcidId <- getStringField(jso, "orcid");
					accessToken <- getStringField(jso, "access_token");
					name <- getStringField(jso, "name")
				) yield new BasicUserInfo(orcidId, accessToken, name)

				Future.fromTry(basicInfoTry)
			}
	}

	private def getUserEmails(bui: BasicUserInfo): Future[Seq[String]] = {

		val request = HttpRequest(
			uri = s"https://pub.orcid.org/v2.0/${bui.orcidId}/email/",
			headers = Accept(MediaTypes.`application/json`) :: Nil
		)

		http.singleRequest(request)
			.flatMap(resp => Unmarshal(resp.entity).to[JsValue])
			.flatMap{jsv =>

				import SprayJsonUtils._
				Future.fromTry(
					for(
						jso <- ensureObject(jsv);
						arr <- getStringArray(jso, "email")
					) yield arr
				)
			}
	}
}

