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

	/**
	 * Exchanges the authorization code for tokens and extracts the identity claims.
	 *
	 * @param expectedNonce the nonce sent with the authorization request; the ID token's
	 *        `nonce` claim must match it (guide section 7.2.3).
	 */
	def retrieveUserInfo(singleUseCode: String, codeVerifier: String, expectedNonce: String): Future[EnvriIdUserInfo] =
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

		import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsValueUnmarshaller
		http.singleRequest(request)
			.flatMap(resp => Unmarshal(resp.entity).to[JsValue])
			.flatMap: js =>
				val infoTry = for
					jso <- ensure[JsObject](js)
					idToken <- getStringField(jso, "id_token")
					// The ID token needs no signature check here: it was fetched over TLS
					// directly from the token endpoint by an authenticated client, which
					// OIDC Core 1.0 section 3.1.3.7 accepts. Never trust an id_token that
					// reached us by any other route.
					payload <- Crypto.parseJWTpayload(idToken)
					_ <- validate(payload, expectedNonce)
					vopersonId <- persistentId(payload)
					email <- getStringField(payload, "email")
					givenName <- getStringField(payload, "given_name")
					familyName <- getStringField(payload, "family_name")
				yield EnvriIdUserInfo(vopersonId, UserInfo(givenName, familyName, email))
				Future.fromTry(infoTry)

	end retrieveUserInfo

	/** `voperson_id`, falling back to `sub` (ENVRI-ID gives both the same value). */
	private def persistentId(payload: JsObject): Try[String] =
		getStringFieldOpt(payload, "voperson_id")
			.orElse(getStringFieldOpt(payload, "sub"))
			.fold(fail("ID token has neither 'voperson_id' nor 'sub' claim"))(Success.apply)

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
