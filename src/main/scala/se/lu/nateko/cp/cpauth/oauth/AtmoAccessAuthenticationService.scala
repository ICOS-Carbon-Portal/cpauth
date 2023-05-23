package se.lu.nateko.cp.cpauth.oauth

import scala.concurrent.Future
import scala.util.Try

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
import se.lu.nateko.cp.cpauth.utils.SprayJsonUtils.*
import spray.json.*


class AtmoAccessAuthenticationService(config: OAuthProviderConfig)(using system: ActorSystem, mat: Materializer):
	import system.dispatcher

	private val http = Http(system)

	def getBasicInfo(singleUseCode: String): Future[JsValue] =

		val request = HttpRequest(
			//uri = "https://sso.aeris-data.fr/auth/realms/aeris/protocol/openid-connect/token",
			uri = "https://keycloak.icos-cp.eu/realms/playground/protocol/openid-connect/token",
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

	end getBasicInfo


end AtmoAccessAuthenticationService
