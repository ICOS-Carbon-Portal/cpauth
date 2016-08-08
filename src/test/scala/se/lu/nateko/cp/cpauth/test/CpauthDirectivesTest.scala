package se.lu.nateko.cp.cpauth.test

import org.scalatest.FunSpec
import se.lu.nateko.cp.cpauth._
import se.lu.nateko.cp.cpauth.core._
import scala.util.Try
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.model.headers.Cookie
import akka.stream.ActorMaterializer

class CpauthDirectivesTest extends FunSpec with ScalatestRouteTest {
	
	def getConfig(privKeyPath: String) = CpauthConfig(
		auth = AuthConfig(
			priv = PrivateAuthConfig(
				authTokenValiditySeconds = 1000,
				privateKeyPath = privKeyPath
			),
			pub = PublicAuthConfig(
				authCookieName = "",
				publicKeyPath = "/public1.pem"
			)
		),
		saml = null,
		http = HttpConfig(
			drupalProxying = null,
			loginPath = null,
			serviceHost = "cpauth.icos-cp.eu",
			servicePrivatePort = 0
		)
	)

	val config = getConfig("/private1.der")

	val dirs = new CpauthDirectives{
		val httpConfig = config.http
		val publicAuthConfig = config.auth.pub
		val authenticator = Authenticator(config.auth.pub)
		val dispatcher = system.dispatcher
		val scheduler = system.scheduler
		val materializer = ActorMaterializer(namePrefix = Some("cpauth_dir_test"))
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

		val route = dirs.user(uinfo => complete(uinfo.givenName))

		describe("when no CPauth cookie is present"){
	
			it("rejects the request with 'CredentialsMissing' rejection"){
				
				Get("/any") ~> route ~> check{
					val authRejections = rejections.collect{
						case AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, _) => 1
					}
					assert(authRejections.length === 1)
				}
			}
		}

		describe("when a properly signed CPauth cookie is present"){
			val user = UserInfo("name", "surname", "mail")
			val cookie = new CookieFactory(config).makeAuthenticationCookie(user).get
			
			it("delegates to the inner route"){
				Get("/any") ~> Cookie(cookie.pair()) ~> route ~> check{
					assert(responseAs[String] === user.givenName)
				}
			}
		}

		describe("when the cookie has been signed with a wrong private key"){
			val user = UserInfo("name", "surname", "mail")
			val wrongConfig = getConfig("/saml/test_private_key.der")
			val cookie = new CookieFactory(wrongConfig).makeAuthenticationCookie(user).get
			
			it("rejects the request with 'CredentialsRejected' rejection"){
				Get("/any") ~> Cookie(cookie.pair()) ~> route ~> check{
					val authRejections = rejections.collect{
						case AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, _) => 1
					}
					assert(authRejections.length === 1)
				}
			}
		}

	}



}