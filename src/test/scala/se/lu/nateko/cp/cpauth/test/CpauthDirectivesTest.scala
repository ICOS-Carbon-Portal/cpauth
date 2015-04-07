package se.lu.nateko.cp.cpauth.test

import org.scalatest.FunSpec
import scala.util.Try
import se.lu.nateko.cp.cpauth.core.Authenticator
import scala.concurrent.ExecutionContext
import se.lu.nateko.cp.cpauth.CpauthDirectives
import se.lu.nateko.cp.cpauth.core.PublicAuthConfig
import spray.testkit.ScalatestRouteTest
import se.lu.nateko.cp.cpauth.core.Exceptions
import spray.http.StatusCodes
import spray.routing.Directives
import spray.routing.AuthenticationFailedRejection
import se.lu.nateko.cp.cpauth.core.UrlsConfig
import se.lu.nateko.cp.cpauth.core.SamlConfig
import se.lu.nateko.cp.cpauth.core.AuthConfig
import se.lu.nateko.cp.cpauth.CookieFactory
import se.lu.nateko.cp.cpauth.core.UserInfo
import spray.http.HttpHeaders.Cookie

class CpauthDirectivesTest extends FunSpec with ScalatestRouteTest with Directives{
	
	def getConfig(privKeyPath: String) = new UrlsConfig with SamlConfig with AuthConfig {
		def authTokenValiditySeconds: Int = 1000
		def privateKeyPath: String = privKeyPath
		
		// Members declared in se.lu.nateko.cp.cpauth.core.PublicAuthConfig
		def publicKeyPath: String = "/public1.pem"
		
		// Members declared in se.lu.nateko.cp.cpauth.core.SamlConfig
		def givenNameAttr: String = ???
		def idpMetadataFilePath: String = ???
		def mailAttr: String = ???
		def samlSpXmlPath: String = ???
		def spConfig = ???
		def surnameAttr: String = ???
		
		// Members declared in se.lu.nateko.cp.cpauth.core.UrlsConfig
		def drupalProxying = ???
		def loginPath: String = ???
		def serviceHost: String = "cpauth.icos-cp.eu"
		def servicePrivatePort: Int = ???
	}
	val config = getConfig("/private1.der")
	val dirs = new CpauthDirectives{
		val publicAuthConfig = config
		val authenticator = Authenticator(publicAuthConfig)
		implicit val dispatcher = scala.concurrent.ExecutionContext.Implicits.global
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
				Get("/any") ~> Cookie(cookie) ~> route ~> check{
					assert(responseAs[String] === user.givenName)
				}
			}
		}

		describe("when the cookie has been signed with a wrong private key"){
			val user = UserInfo("name", "surname", "mail")
			val wrongConfig = getConfig("/saml/test_private_key.der")
			val cookie = new CookieFactory(wrongConfig).makeAuthenticationCookie(user).get
			
			it("rejects the request with 'CredentialsRejected' rejection"){
				Get("/any") ~> Cookie(cookie) ~> route ~> check{
					val authRejections = rejections.collect{
						case AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, _) => 1
					}
					assert(authRejections.length === 1)
				}
			}
		}

	}



}