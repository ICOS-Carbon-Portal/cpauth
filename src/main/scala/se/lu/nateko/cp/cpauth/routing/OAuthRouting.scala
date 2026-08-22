package se.lu.nateko.cp.cpauth.routing

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import se.lu.nateko.cp.cpauth.CpauthConfig
import se.lu.nateko.cp.cpauth.OAuthProvider
import se.lu.nateko.cp.cpauth.accounts.RestHeartClient
import se.lu.nateko.cp.cpauth.core.AuthSource
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.oauth.*
import se.lu.nateko.cp.cpauth.services.CookieFactory
import se.lu.nateko.cp.cpauth.utils.Oidc
import se.lu.nateko.cp.cpauth.utils.OidcLoginStore
import se.lu.nateko.cp.cpauth.utils.PendingOidcLogin
import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.CpauthConfig
import akka.actor.ActorSystem
import se.lu.nateko.cp.cpauth.OAuthProvider

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

trait OAuthRouting extends CpauthDirectives:

	def oauthConfig: CpauthConfig.OAuthConfig
	def cookieFactory: CookieFactory
	given system: ActorSystem
	def restHeart: RestHeartClient
	def oidcLoginStore: OidcLoginStore

	val oauthRoute: Route = (pathPrefix("oauth") & extractEnvri) { implicit envri =>
		pathPrefix("facebook"){
			oauthRoute(facebookAuth.retrieveUserInfo(_).map(uinfoToToken), AuthSource.Facebook)
		} ~
		pathPrefix("orcidid"){
			oauthRoute(cpauthTokenFromOrcidId, AuthSource.Orcid)
		} ~
		pathPrefix("atmoAccess"){
			atmoAccessAuth match
				case None => complete(StatusCodes.InternalServerError -> s"ATMO ACCESS authentication is not configured for $envri")
				case Some(atmoService) =>
					oauthRoute(atmoService.retrieveUserInfo(_).map(uinfoToToken), AuthSource.AtmoAccess)
		} ~
		pathPrefix("envriId"){
			envriIdAuth match
				case None => complete(StatusCodes.InternalServerError -> s"ENVRI-ID authentication is not configured for $envri")
				case Some(envriIdService) =>
					path("login"){envriIdLoginRoute(envriIdService)} ~
					pathEnd{envriIdCallbackRoute(envriIdService)}
		} ~
		complete(StatusCodes.NotFound)
	}

	/**
	 * Starts an ENVRI-ID login. Unlike the other providers, whose authorization URLs are
	 * assembled in the browser, this is built server-side so that the PKCE code verifier and
	 * the nonce can be kept out of the user agent and remembered until the callback.
	 */
	private def envriIdLoginRoute(service: EnvriIdAuthenticationService)(using Envri): Route =
		parameter("targetUrl".?){targetUrlStr =>
			val state = Oidc.randomToken()
			val nonce = Oidc.randomToken()
			val codeVerifier = Oidc.randomToken()

			oidcLoginStore.memorize(
				state,
				PendingOidcLogin(
					targetUrl = targetUrlStr.flatMap(validatedTargetUrl),
					nonce = nonce,
					codeVerifier = codeVerifier,
					created = Instant.now
				)
			)

			val authUri = Uri(service.authEndpoint).withQuery(Uri.Query(
				"client_id" -> service.clientId,
				"response_type" -> "code",
				"scope" -> OAuthRouting.EnvriIdScopes.mkString(" "),
				"redirect_uri" -> service.redirectUri,
				"state" -> state,
				"nonce" -> nonce,
				"code_challenge" -> Oidc.codeChallengeS256(codeVerifier),
				"code_challenge_method" -> "S256"
			))
			redirect(authUri, StatusCodes.Found)
		}

	private def envriIdCallbackRoute(service: EnvriIdAuthenticationService)(using Envri): Route =
		parameters("code", "state"){(code, state) =>
			// A 'state' that is not in the store was never issued by us, or has already been
			// redeemed, or has expired. Rejecting it binds the response to our request.
			oidcLoginStore.getAndForget(state) match
				case None =>
					complete(StatusCodes.BadRequest -> "Unknown, expired or already used login state")
				case Some(pending) =>
					val cookieFut = for
						uinfo <- service.retrieveUserInfo(code, pending.codeVerifier, pending.nonce)
						uid <- envriIdUserId(uinfo)
						token <- Future.fromTry(cookieFactory.makeTokenBase64(uid, AuthSource.EnvriId))
					yield cookieFactory.makeAuthCookie(token)

					onSuccess(cookieFut){cookie =>
						setCookie(cookie){
							redirect(pending.targetUrl.getOrElse(Uri("/home/")), StatusCodes.Found)
						}
					}
		}

	/**
	 * Resolves the cpauth user for an ENVRI-ID login, preferring the persistent identifier
	 * over the email address so that a changed home-organisation email keeps the account.
	 */
	private def envriIdUserId(uinfo: EnvriIdUserInfo)(using Envri): Future[UserId] =
		restHeart.findUserByVopersonId(uinfo.vopersonId).flatMap{
			case Some(uid) => Future.successful(uid)
			case None =>
				val uid = UserId(uinfo.info.email)
				// Unlike the fire-and-forget calls elsewhere in this trait, these are chained
				// in: if we cannot record the identifier, the next login would silently fall
				// back to email matching, so the failure must be visible.
				for
					_ <- restHeart.createUserIfNew(uid, uinfo.info.givenName, uinfo.info.surname)
					_ <- restHeart.setVopersonId(uid, uinfo.vopersonId)
				yield uid
		}

	/**
	 * Accepts a post-login redirect target only if it is relative, or absolute within the
	 * auth cookie's domain. Hosts outside that domain cannot use the cookie anyway, so
	 * honouring them would just make cpauth an open redirector.
	 */
	private def validatedTargetUrl(target: String)(using Envri): Option[Uri] =
		// NOTE: a protocol-relative URI such as "//example.com/x" has an empty scheme, so akka
		// reports it as relative, yet browsers follow it to another host. Hence the authority
		// must be checked even for relative URIs.
		Try(Uri(target)).toOption.filter{uri =>
			if uri.authority.isEmpty then uri.isRelative
			else
				// authCookieDomain is dot-prefixed by convention (".icos-cp.eu"), as relied on
				// by the CORS origin check in CpauthDirectives
				val bareDomain = publicAuthConfig.authCookieDomain.stripPrefix(".")
				val host = uri.authority.host.address
				host == bareDomain || host.endsWith("." + bareDomain)
		}

	private def uinfoToToken(userInfo: UserInfo)(using Envri): UserId =
		val uid = UserId(userInfo.email)
		//Silent side effect: creating user profile if it does not already exist
		restHeart.createUserIfNew(uid, userInfo.givenName, userInfo.surname)
		uid

	private def cpauthTokenFromOrcidId(code: String)(using Envri): Future[UserId] = orcidAuth
		.retrieveUserInfo(code)
		.flatMap(userInfo => userInfo.toPlainUserInfo match
			case Some(uinfo) =>
				Future.successful(uinfoToToken(uinfo))
			case None =>
				restHeart.findUsers(Map("profile.orcid" -> userInfo.orcidId))
					.map(_.headOption.getOrElse(throw new Exception(
						"You need to either make your (verified!) email public in your OrcidID account, " +
						"or log in to CP by other means first, and specify your OrcidId in your CP user profile"
					)))
		)

	private def oauthRoute(uidProvider: String => Future[UserId], source: AuthSource)(using Envri): Route = {
		parameters("code", "state".?){(code, targetUrl) =>
			val tokenFut: Future[String] = uidProvider(code).flatMap{uid =>
				Future.fromTry(
					cookieFactory.makeTokenBase64(uid, source)
				)
			}
			onSuccess(tokenFut){token =>
				setCookie(cookieFactory.makeAuthCookie(token)){

					targetUrl match
						case Some(target) =>
							//getting rid of Facebook's appended #_=_
							val uri = if(Uri(target).fragment.isDefined) target else target + "#"
							redirect(uri, StatusCodes.Found)

						case None => redirect("/#", StatusCodes.Found)
				}
			}
		}
	}

	private def facebookAuth(using envri: Envri) = new FacebookAuthenticationService(
		oauthConfig(envri)(OAuthProvider.facebook)
	)

	private def orcidAuth(using envri: Envri) = new OrcidAuthenticationService(
		oauthConfig(envri)(OAuthProvider.orcidid)
	)

	private def atmoAccessAuth(using envri: Envri): Option[AtmoAccessAuthenticationService] =
		for
			envriConf <- oauthConfig.get(envri)
			conf <- envriConf.get(OAuthProvider.atmoAccess)
		yield new AtmoAccessAuthenticationService(conf)

	private def envriIdAuth(using envri: Envri): Option[EnvriIdAuthenticationService] =
		for
			envriConf <- oauthConfig.get(envri)
			conf <- envriConf.get(OAuthProvider.envriId)
		yield new EnvriIdAuthenticationService(conf)

end OAuthRouting

object OAuthRouting:
	/**
	 * Only what cpauth actually consumes. The ENVRI-ID guide (sections 7.2.7 and 8) requires
	 * services to request the minimum; adding a scope here also needs a reconfiguration
	 * request in the ENVRI-ID service registry.
	 */
	val EnvriIdScopes = Seq("openid", "voperson_id", "email", "profile")
