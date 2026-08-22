package se.lu.nateko.cp.cpauth.test

import akka.actor.ActorSystem
import akka.actor.Scheduler
import akka.event.NoLogging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.`Set-Cookie`
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import eu.icoscp.envri.Envri
import org.scalatest.funspec.AnyFunSpec
import se.lu.nateko.cp.cpauth.*
import se.lu.nateko.cp.cpauth.core.*
import se.lu.nateko.cp.cpauth.routing.OAuthRouting
import se.lu.nateko.cp.cpauth.services.CookieFactory
import se.lu.nateko.cp.cpauth.utils.MapBasedOidcLoginStore
import se.lu.nateko.cp.cpauth.utils.Oidc
import se.lu.nateko.cp.cpauth.utils.PendingOidcLogin

import java.net.URI
import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class EnvriIdLoginTest extends AnyFunSpec with ScalatestRouteTest:
	import Envri.ICOS
	given Envri = ICOS

	private val envriIdConf = OAuthProviderConfig(
		clientId = "cpauth-icos",
		clientSecret = "secret",
		redirectPath = "https://cpauth.icos-cp.eu/oauth/envriId",
		issuer = Some("https://login.staging.envri.eu/auth/realms/envri")
	)

	private val config = CpauthConfig(
		auth = AuthConfig(
			priv = PrivateAuthConfig(
				authTokenValiditySeconds = 1000,
				privateKeyPaths = Map(ICOS -> "src/test/resources/private1.der")
			),
			pub = Map(ICOS -> PublicAuthConfig(
				authCookieName = "cpauthToken",
				authCookieDomain = ".icos-cp.eu",
				authHost = "cpauth.icos-cp.eu",
				publicKeyPath = "/public1.pem"
			)),
			secretUserSalt = "bla",
			masterAdminUser = "",
			masterAdminPass = "",
		),
		saml = null,
		database = null,
		http = HttpConfig(
			drupalProxying = null,
			loginPath = null,
			serviceHosts = Map(ICOS -> "cpauth.icos-cp.eu"),
			servicePrivatePort = 0,
			serviceInterface = "localhost"
		),
		restheart = null,
		mailing = null,
		oauth = Map(ICOS -> Map(OAuthProvider.envriId -> envriIdConf)),
	)

	private val store = new MapBasedOidcLoginStore

	private val routing = new OAuthRouting:
		val oauthConfig = config.oauth
		val cookieFactory = new CookieFactory(config, NoLogging)
		given system: ActorSystem = EnvriIdLoginTest.this.system
		val oidcLoginStore = store
		val httpConfig = config.http
		val authConfig = config.auth
		given dispatcher: ExecutionContext = system.dispatcher
		given scheduler: Scheduler = system.scheduler
		def hostToEnvri(host: String) = config.http.serviceHosts.map(_.swap).get(host)
		val userDb = null
		val restHeart = null

	private val route = routing.oauthRoute

	private def loginRedirectQuery(targetUrl: Option[String]): Uri.Query =
		val suffix = targetUrl.fold("")(t => "?targetUrl=" + java.net.URLEncoder.encode(t, "UTF-8"))
		Get(s"https://cpauth.icos-cp.eu/oauth/envriId/login$suffix") ~> route ~> check:
			assert(status === StatusCodes.Found)
			Uri(header("Location").get.value).query()

	describe("ENVRI-ID authorization request"){

		it("redirects to the configured issuer's authorization endpoint"){
			Get("https://cpauth.icos-cp.eu/oauth/envriId/login") ~> route ~> check:
				assert(status === StatusCodes.Found)
				val location = Uri(header("Location").get.value)
				assert(location.withQuery(Uri.Query.Empty).toString ===
					"https://login.staging.envri.eu/auth/realms/envri/protocol/openid-connect/auth")
		}

		it("uses the authorization code flow with PKCE S256 and requests only the needed scopes"){
			val q = loginRedirectQuery(None)
			assert(q.get("response_type") === Some("code"))
			assert(q.get("client_id") === Some("cpauth-icos"))
			assert(q.get("code_challenge_method") === Some("S256"))
			assert(q.get("scope") === Some("openid voperson_id email profile"))
			assert(q.get("redirect_uri") === Some("https://cpauth.icos-cp.eu/oauth/envriId"))
		}

		it("sends state, nonce and a code challenge, and remembers the login under the state"){
			val q = loginRedirectQuery(None)
			val state = q.get("state").get
			val nonce = q.get("nonce").get
			assert(state.nonEmpty && nonce.nonEmpty && state != nonce)

			val pending = store.getAndForget(state)
			assert(pending.isDefined)
			assert(pending.get.nonce === nonce)
			// the verifier stays server-side; only its S256 challenge is sent out
			assert(q.get("code_challenge") === Some(Oidc.codeChallengeS256(pending.get.codeVerifier)))
			assert(q.get("code_challenge").get != pending.get.codeVerifier)
		}

		it("keeps a target URL inside the auth cookie domain"){
			val q = loginRedirectQuery(Some("https://data.icos-cp.eu/portal/"))
			val pending = store.getAndForget(q.get("state").get).get
			assert(pending.targetUrl === Some(Uri("https://data.icos-cp.eu/portal/")))
		}

		it("keeps a relative target URL"){
			val q = loginRedirectQuery(Some("/home/"))
			val pending = store.getAndForget(q.get("state").get).get
			assert(pending.targetUrl === Some(Uri("/home/")))
		}

		it("drops a target URL outside the auth cookie domain"){
			val q = loginRedirectQuery(Some("https://evil.example.com/steal"))
			val pending = store.getAndForget(q.get("state").get).get
			assert(pending.targetUrl === None)
		}

		it("drops a protocol-relative target URL pointing at another host"){
			// empty scheme makes akka call this relative, but a browser would follow it away
			val q = loginRedirectQuery(Some("//evil.example.com/steal"))
			val pending = store.getAndForget(q.get("state").get).get
			assert(pending.targetUrl === None)
		}

		it("drops a look-alike domain that merely ends with the cookie domain's letters"){
			val q = loginRedirectQuery(Some("https://evil-icos-cp.eu/steal"))
			val pending = store.getAndForget(q.get("state").get).get
			assert(pending.targetUrl === None)
		}

		it("keeps the bare cookie domain itself"){
			val q = loginRedirectQuery(Some("https://icos-cp.eu/"))
			val pending = store.getAndForget(q.get("state").get).get
			assert(pending.targetUrl === Some(Uri("https://icos-cp.eu/")))
		}
	}

	describe("ENVRI-ID callback"){

		it("rejects a state that was never issued"){
			Get("https://cpauth.icos-cp.eu/oauth/envriId?code=whatever&state=forged") ~> route ~> check:
				assert(status === StatusCodes.BadRequest)
				assert(responseAs[String].contains("Unknown, expired or already used login state"))
		}

		it("does not log anyone in when the callback carries no state at all"){
			Get("https://cpauth.icos-cp.eu/oauth/envriId?code=whatever") ~> route ~> check:
				assert(status != StatusCodes.Found)
				assert(headers.collect{case `Set-Cookie`(c) => c}.isEmpty)
		}
	}

	describe("pending-login store"){

		it("redeems a state only once, so a replayed callback fails"){
			val store = new MapBasedOidcLoginStore
			val pending = PendingOidcLogin(None, "nonce", "verifier", Instant.now)
			store.memorize("st", pending)
			assert(store.getAndForget("st") === Some(pending))
			assert(store.getAndForget("st") === None)
		}

		it("does not return an entry older than the TTL"){
			val store = new MapBasedOidcLoginStore(ttl = 1.second)
			store.memorize("st", PendingOidcLogin(None, "nonce", "verifier", Instant.now.minusSeconds(60)))
			assert(store.getAndForget("st") === None)
		}

		it("evicts expired entries"){
			val store = new MapBasedOidcLoginStore(ttl = 1.second)
			store.memorize("stale", PendingOidcLogin(None, "n", "v", Instant.now.minusSeconds(60)))
			store.memorize("fresh", PendingOidcLogin(None, "n", "v", Instant.now))
			store.evictExpired()
			assert(store.getAndForget("stale") === None)
			assert(store.getAndForget("fresh").isDefined)
		}
	}

	describe("OAuth provider config"){
		import spray.json.*
		import ConfigReader.given

		// Guards the jsonFormat arity: adding a field to OAuthProviderConfig without bumping
		// jsonFormatN in ConfigReader compiles, then fails only when config is read.
		it("reads a provider that declares an issuer"){
			val conf = """{
				"clientId": "cpauth-icos",
				"clientSecret": "s3cret",
				"redirectPath": "https://cpauth.icos-cp.eu/oauth/envriId",
				"issuer": "https://login.envri.eu/auth/realms/envri"
			}""".parseJson.convertTo[OAuthProviderConfig]
			assert(conf.issuer === Some("https://login.envri.eu/auth/realms/envri"))
		}

		it("reads a provider that omits the issuer, as the older providers do"){
			val conf = """{
				"clientId": "id",
				"clientSecret": "s3cret",
				"redirectPath": "https://cpauth.icos-cp.eu/oauth/orcidid"
			}""".parseJson.convertTo[OAuthProviderConfig]
			assert(conf.issuer === None)
		}

		it("never exposes a client secret to the login page"){
			val json = CpauthConfig
				.oauthJson(Map(OAuthProvider.envriId -> envriIdConf))
				.parseJson.asJsObject
			val emitted = json.fields("envriId").asJsObject.fields
			assert(emitted("clientSecret") === JsString(""))
			// the issuer is public, and the login page has no use for it either way
			assert(emitted("clientId") === JsString("cpauth-icos"))
		}
	}

	describe("shipped configuration"){

		// Depends on the git-ignored application.conf in the project root, which supplies the
		// client ids the bundled resource config leaves commented out. Absent on a fresh clone.
		it("parses, and gives the ENVRI-ID provider an issuer"){
			assume(new java.io.File("application.conf").getAbsoluteFile.exists)
			val parsed = ConfigReader.getDefault
			assert(parsed.isSuccess, parsed.failed.map(_.getMessage).getOrElse(""))
			val icosOauth = parsed.get.oauth(ICOS)
			assert(icosOauth.contains(OAuthProvider.envriId))
			assert(icosOauth(OAuthProvider.envriId).issuer.isDefined)
		}
	}

	describe("PKCE helpers"){

		it("generates code verifiers within the length RFC 7636 allows"){
			val verifier = Oidc.randomToken()
			assert(verifier.length >= 43 && verifier.length <= 128)
			assert(verifier.forall(c => c.isLetterOrDigit || c == '-' || c == '_'))
		}

		it("generates a distinct token each time"){
			assert(Oidc.randomToken() != Oidc.randomToken())
		}
	}

end EnvriIdLoginTest
