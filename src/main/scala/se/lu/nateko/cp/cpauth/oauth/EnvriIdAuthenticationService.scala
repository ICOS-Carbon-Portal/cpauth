package se.lu.nateko.cp.cpauth.oauth

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import se.lu.nateko.cp.cpauth.OAuthProvider
import se.lu.nateko.cp.cpauth.OAuthProviderConfig
import se.lu.nateko.cp.cpauth.core.Crypto
import se.lu.nateko.cp.cpauth.core.SprayJsonUtils.*
import spray.json.*

import java.time.Instant
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/**
 * OIDC Relying Party for ENVRI-ID (RCIAM/Keycloak), the cross-domain AAI of ENVRI-Hub.
 *
 * Uses the Authorization Code flow with PKCE. Client authentication is
 * `client_secret_basic`, which is the default the ENVRI-ID service registry assigns.
 */
class EnvriIdAuthenticationService(config: OAuthProviderConfig)(using system: ActorSystem, mat: Materializer):
	import system.dispatcher

	private val http = Http(system)
	private val issuer = config.issuerOrCrash(OAuthProvider.envriId)

	def clientId: String = config.clientId
	def redirectUri: String = config.redirectPath
	def authEndpoint: String = s"$issuer/protocol/openid-connect/auth"
	private def tokenEndpoint: String = s"$issuer/protocol/openid-connect/token"
	private def userInfoEndpoint: String = s"$issuer/protocol/openid-connect/userinfo"

	/**
	 * Exchanges the authorization code for tokens, then reads the identity claims.
	 *
	 * The ID token authenticates the user, but ENVRI-ID releases the profile claims from the
	 * UserInfo endpoint rather than putting them in the ID token. That is what OIDC Core 1.0
	 * section 5.4 prescribes for the `profile` and `email` scopes whenever an access token is
	 * issued, and what the ENVRI-ID integration guide describes, so both are consulted.
	 *
	 * @param expectedNonce the nonce sent with the authorization request; the ID token's
	 *        `nonce` claim must match it (guide section 7.2.3).
	 */
	def retrieveUserInfo(singleUseCode: String, codeVerifier: String, expectedNonce: String): Future[EnvriIdUserInfo] =
		for
			tokens <- exchangeCode(singleUseCode, codeVerifier)
			idPayload <- Future.fromTry(
				for
					idToken <- getStringField(tokens, "id_token")
					// The ID token needs no signature check here: it was fetched over TLS
					// directly from the token endpoint by an authenticated client, which
					// OIDC Core 1.0 section 3.1.3.7 accepts. Never trust an id_token that
					// reached us by any other route.
					payload <- Crypto.parseJWTpayload(idToken)
					_ <- validate(payload, expectedNonce)
				yield payload
			)
			accessToken <- Future.fromTry(getStringField(tokens, "access_token"))
			claims <- fetchUserInfo(accessToken)
			info <- Future.fromTry(EnvriIdAuthenticationService.identityFrom(idPayload, claims))
		yield info

	private def exchangeCode(singleUseCode: String, codeVerifier: String): Future[JsObject] =
		val request = HttpRequest(
			uri = tokenEndpoint,
			method = HttpMethods.POST,
			headers =
				Authorization(BasicHttpCredentials(config.clientId, config.clientSecret)) ::
				Accept(MediaTypes.`application/json`) :: Nil,
			entity = FormData(
				"grant_type" -> "authorization_code",
				"redirect_uri" -> config.redirectPath,
				"code" -> singleUseCode,
				"code_verifier" -> codeVerifier
			).toEntity
		)
		singleRequestAsJsObject(request, "token")

	private def fetchUserInfo(accessToken: String): Future[JsObject] =
		val request = HttpRequest(
			uri = userInfoEndpoint,
			headers =
				Authorization(OAuth2BearerToken(accessToken)) ::
				Accept(MediaTypes.`application/json`) :: Nil
		)
		singleRequestAsJsObject(request, "userinfo")

	private def singleRequestAsJsObject(request: HttpRequest, what: String): Future[JsObject] =
		import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsValueUnmarshaller
		http.singleRequest(request).flatMap: resp =>
			Unmarshal(resp.entity).to[JsValue].flatMap: js =>
				if resp.status.isSuccess then Future.fromTry(ensure[JsObject](js))
				else Future.failed(new Exception(
					s"ENVRI-ID $what endpoint responded ${resp.status.value}: ${js.compactPrint}"
				))

	end singleRequestAsJsObject

	private def validate(payload: JsObject, expectedNonce: String): Try[Unit] =
		for
			iss <- getStringField(payload, "iss")
			_ <- if iss == issuer then Success(()) else fail(s"ID token issuer '$iss' does not match '$issuer'")
			_ <- validateAudience(payload)
			_ <- validateExpiry(payload)
			nonce <- getStringField(payload, "nonce")
			_ <-
				if nonce == expectedNonce then Success(())
				else fail("ID token 'nonce' does not match the one sent with the authentication request")
		yield ()

	private def validateAudience(payload: JsObject): Try[Unit] =
		val auds = payload.fields.get("aud") match
			case Some(JsString(single)) => Seq(single)
			case Some(JsArray(many)) => many.collect{case JsString(s) => s}
			case _ => Nil
		if auds.contains(config.clientId) then Success(())
		else fail(s"ID token audience ${auds.mkString("[", ", ", "]")} does not contain '${config.clientId}'")

	private def validateExpiry(payload: JsObject): Try[Unit] =
		payload.fields.get("exp") match
			case Some(JsNumber(exp)) =>
				if Instant.ofEpochSecond(exp.longValue).isAfter(Instant.now) then Success(())
				else fail("ID token has expired")
			case _ => fail("ID token has no numeric 'exp' claim")

	private def fail(msg: String): Failure[Nothing] = Failure(new Exception(msg))

end EnvriIdAuthenticationService

object EnvriIdAuthenticationService:

	private def fail(msg: String): Failure[Nothing] = Failure(new Exception(msg))

	/**
	 * Combines the validated ID token with the UserInfo response into the identity cpauth needs.
	 *
	 * Claims are taken from UserInfo first and from the ID token as a fallback, since a
	 * provider may put them in either place (OIDC Core 1.0 section 5.4).
	 */
	def identityFrom(idPayload: JsObject, userInfo: JsObject): Try[EnvriIdUserInfo] =
		def claim(name: String): Option[String] =
			getStringFieldOpt(userInfo, name).orElse(getStringFieldOpt(idPayload, name))

		def required(name: String): Try[String] = claim(name).fold(
			fail(s"ENVRI-ID released neither an ID token nor a UserInfo '$name' claim")
		)(Success.apply)

		for
			_ <- ensureSameSubject(idPayload, userInfo)
			// voperson_id is the persistent identifier; sub carries the same value in ENVRI-ID
			vopersonId <- claim("voperson_id").fold(required("sub"))(Success.apply)
			email <- required("email")
			givenName <- required("given_name")
			familyName <- required("family_name")
		yield EnvriIdUserInfo(vopersonId, UserInfo(givenName, familyName, email))

	/**
	 * OIDC Core 1.0 section 5.3.2 requires the UserInfo `sub` to match the ID token's, so that
	 * claims fetched with the access token cannot be attributed to a different user.
	 */
	private def ensureSameSubject(idPayload: JsObject, userInfo: JsObject): Try[Unit] =
		(getStringFieldOpt(idPayload, "sub"), getStringFieldOpt(userInfo, "sub")) match
			case (Some(idSub), Some(uiSub)) if idSub == uiSub => Success(())
			case (Some(idSub), Some(uiSub)) =>
				fail(s"UserInfo subject '$uiSub' does not match the ID token subject '$idSub'")
			case (None, _) => fail("ID token has no 'sub' claim")
			case (_, None) => fail("UserInfo response has no 'sub' claim")

end EnvriIdAuthenticationService
