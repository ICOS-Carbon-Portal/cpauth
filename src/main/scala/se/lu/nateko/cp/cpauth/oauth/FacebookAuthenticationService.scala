package se.lu.nateko.cp.cpauth.oauth

import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.unmarshalling.Unmarshal
import se.lu.nateko.cp.cpauth.OAuthProviderConfig
import se.lu.nateko.cp.cpauth.core.SprayJsonUtils.*
import spray.json.*


class FacebookAuthenticationService(config: OAuthProviderConfig)(using system: ActorSystem) {

	import system.dispatcher

	private val http = Http(system)

	def retrieveUserInfo(singleUseCode: String): Future[UserInfo] = {
		for(
			accessToken <- getAccessToken(singleUseCode);
			userInfo <- getUserInfo(accessToken)
		) yield userInfo
	}

	private def getAccessToken(singleUseCode: String): Future[String] = {

		val request = HttpRequest(
			uri = "https://graph.facebook.com/v2.9/oauth/access_token",
			method = HttpMethods.POST,
			headers = Accept(MediaTypes.`application/json`) :: Nil,
			entity = FormData(
				"client_secret" -> config.clientSecret,
				"client_id" -> config.clientId,
				"redirect_uri" -> config.redirectPath,
				"code" -> singleUseCode
			).toEntity
		)

		http.singleRequest(request)
			.flatMap(resp => Unmarshal(resp.entity).to[JsValue])
			.flatMap{jsv =>

				Future.fromTry(
					for(
						jso <- ensure[JsObject](jsv);
						accessToken <- getStringField(jso, "access_token")
					) yield accessToken
				)
			}
	}

	private def getUserInfo(accessToken: String): Future[UserInfo] = {

		val request = HttpRequest(
			uri = Uri("https://graph.facebook.com/me").withQuery(
				Uri.Query(
					"fields" -> "first_name,last_name,email",
					"access_token" -> accessToken
				)
			),
			headers = Accept(MediaTypes.`application/json`) :: Nil
		)

		http.singleRequest(request)
			.flatMap(resp => Unmarshal(resp.entity).to[JsValue])
			.flatMap{jsv =>

				Future.fromTry(
					for(
						jso <- ensure[JsObject](jsv);
						firstName <- getStringField(jso, "first_name");
						lastName <- getStringField(jso, "last_name");
						email <- getStringField(jso, "email")
					) yield new UserInfo(firstName, lastName, email)
				)
			}
	}

}

