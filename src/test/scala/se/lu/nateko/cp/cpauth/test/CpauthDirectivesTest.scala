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

class CpauthDirectivesTest extends FunSpec with ScalatestRouteTest with Directives{
	
	def getDirs(pubKeyPath: String, auth: Try[Authenticator]) = new CpauthDirectives{
		val publicAuthConfig = new PublicAuthConfig{ val publicKeyPath = pubKeyPath}
		val authenticator = auth
		implicit val dispatcher = scala.concurrent.ExecutionContext.Implicits.global
	}

	describe("attempt directive"){

		val dirs = getDirs("", Exceptions.failure("dummy"))

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

	describe("user directive when no CPauth cookie is present"){
		val dirs = getDirs("", Exceptions.failure("dummy"))

		it("rejects the request with 'CredentialsMissing' rejection"){
			val route = dirs.user(uinfo => complete(uinfo.givenName))
			
			Get("/any") ~> route ~> check{
				val authRejections = rejections.collect{
					case AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, _) => 1
				}
				assert(authRejections.length === 1)
			}
		}
	}



}