package se.lu.nateko.cp.cpauth.test

import org.scalatest.funspec.AnyFunSpec

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import se.lu.nateko.cp.cpauth._
import se.lu.nateko.cp.cpauth.core._
import se.lu.nateko.cp.cpauth.routing.BadCpauthCookieRejection
import se.lu.nateko.cp.cpauth.routing.CpauthCookieMissingRejection
import se.lu.nateko.cp.cpauth.routing.CpauthDirectives
import se.lu.nateko.cp.cpauth.services.CookieFactory
import eu.icoscp.envri.Envri
import java.net.URI

class CpauthDirectivesTest extends AnyFunSpec with ScalatestRouteTest {
	import Envri.ICOS
	given Envri = ICOS

	def getConfig(privKeyPath: String) = CpauthConfig(
		auth = AuthConfig(
			priv = PrivateAuthConfig(
				authTokenValiditySeconds = 1000,
				privateKeyPaths = Map(ICOS -> privKeyPath)
			),
			pub = Map(ICOS -> PublicAuthConfig(
				authCookieName = "",
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
		restheart = RestHeartConfig(
			db = Map(ICOS -> RestHeartDBConfig(
				uri = new URI("http://127.0.0.1:8088/db/"),
				username = "",
				password = ""
			)),
			portalUsageCollection = "",
			usersCollection = "",
			ipsToIgnore = Nil,
			skipInit = true
		),
		mailing = null,
		oauth = null,
	)

	val config = getConfig("src/test/resources/private1.der")

	val dirs = new CpauthDirectives{
		val httpConfig = config.http
		val authConfig = config.auth
		val dispatcher = system.dispatcher
		val scheduler = system.scheduler
		val materializer = Materializer(system)
		def hostToEnvri(host: String) = config.http.serviceHosts.map(_.swap).get(host)
		val userDb = null
		val restHeart = null
	}


	describe("attempt directive"){

		it("computes inner route if the attempt is successful"){
			val route = dirs.attempt("blabla"){ s =>
				complete((StatusCodes.OK, s))
			}

			Get("/any") ~> route ~> check {
				assert(responseAs[String] === "blabla")
			}
		}

		it("returns a 'Bad Request' HTTP response with exception's message as body, in case attempt fails"){
			val route = dirs.attempt(Exceptions.failure[String]("error message")){ s =>
				complete((StatusCodes.OK, s))
			}

			Get("/any") ~> route ~> check {
				assert(status === StatusCodes.BadRequest)
				assert(responseAs[String] === "error message")
			}
		}
	}

	describe("user directive"){

		val route = dirs.user(uid => complete(uid.email))

		describe("when no CPauth cookie is present"){

			it("rejects the request with 'CredentialsMissing' rejection"){

				Get("https://cpauth.icos-cp.eu/any") ~> route ~> check{
					val authRejections = rejections.collect{
						case CpauthCookieMissingRejection => 1
					}
					assert(authRejections.length === 1)
				}
			}
		}

		def makeCookie(uid: String, config: CpauthConfig): HttpCookie = {
			val factory = new CookieFactory(config)
			val token = factory.makeTokenBase64(UserId(uid), AuthSource.Password).get
			factory.makeAuthCookie(token)
		}

		describe("when a properly signed CPauth cookie is present"){
			val cookie = makeCookie("test1", config)

			it("delegates to the inner route"){
				Get("https://cpauth.icos-cp.eu/any") ~> Cookie(cookie.pair()) ~> route ~> check{
					assert(responseAs[String] === "test1")
				}
			}
		}

		describe("when the cookie has been signed with a wrong private key"){
			val wrongConfig = getConfig("src/test/resources/saml/test_private_key.der")
			val cookie = makeCookie("test2", wrongConfig)

			it("rejects the request with 'CredentialsRejected' rejection"){
				Get("https://cpauth.icos-cp.eu/any") ~> Cookie(cookie.pair()) ~> route ~> check{
					val authRejections = rejections.collect{
						case _: BadCpauthCookieRejection => 1
					}
					assert(authRejections.length === 1)
				}
			}
		}

	}



}
