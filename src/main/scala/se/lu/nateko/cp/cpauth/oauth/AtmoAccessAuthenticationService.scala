package se.lu.nateko.cp.cpauth.oauth

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import se.lu.nateko.cp.cpauth.OAuthProviderConfig
import se.lu.nateko.cp.cpauth.core.CoreUtils
import se.lu.nateko.cp.cpauth.utils.SprayJsonUtils.*
import spray.json.*

import scala.concurrent.Future
import scala.util.Try


class AtmoAccessAuthenticationService(config: OAuthProviderConfig)(using system: ActorSystem, mat: Materializer):
	import system.dispatcher

	private val http = Http(system)

	def retrieveUserInfo(singleUseCode: String): Future[UserInfo] =
		//val uri = "https://sso.aeris-data.fr/auth/realms/aeris/protocol/openid-connect/token",
		val uri = "https://keycloak.icos-cp.eu/realms/playground/protocol/openid-connect/token"
		val request = HttpRequest(
			uri = uri,
			method = HttpMethods.POST,
			headers = Accept(MediaTypes.`application/json`) :: Nil,
			entity = FormData(
				//"client_secret" -> config.clientSecret,
				"client_id" -> config.clientId,
				"grant_type" -> "authorization_code",
				"redirect_uri" -> config.redirectPath,
				"code" -> singleUseCode
			).toEntity
		)

		http.singleRequest(request)
			.flatMap(resp => Unmarshal(resp.entity).to[JsValue])
			.flatMap{js =>
				val aauiTry = for
					jso <- ensure[JsObject](js)
					idToken <- getStringField(jso, "id_token")
					idTokenParts = idToken.split('.')
					_ = assert(idTokenParts.length >= 2, s"Invalid id_token received from $uri (expected at least 2 parts, got ${idTokenParts.length})")
					payloadJsStr = CoreUtils.decodeBase64UrlToString(idTokenParts(1))
					payload <- ensure[JsObject](payloadJsStr.parseJson)
					email <- getStringField(payload, "email")
					givenName <- getStringField(payload, "given_name")
					familyName <- getStringField(payload, "family_name")
				yield UserInfo(givenName, familyName, email)
				Future.fromTry(aauiTry)
			}

	end retrieveUserInfo


end AtmoAccessAuthenticationService
