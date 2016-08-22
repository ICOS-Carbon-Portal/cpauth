package se.lu.nateko.cp.cpauth

import akka.actor.ActorSystem
import se.lu.nateko.cp.cpauth.CpauthJsonProtocol._
import se.lu.nateko.cp.cpauth.accounts.Users
import se.lu.nateko.cp.cpauth.core.AuthenticationFailedException
import se.lu.nateko.cp.cpauth.core.Authenticator
import se.lu.nateko.cp.cpauth.core.CoreUtils
import se.lu.nateko.cp.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cp.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cp.cpauth.routing._
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._


object Main extends App with SamlRouting with PasswordRouting with DrupalRouting with StaticRouting with RestHeartRouting{

	val config: CpauthConfig = ConfigReader.getDefault
	val (httpConfig, publicAuthConfig, samlConfig, restheartConfig) = (config.http, config.auth.pub, config.saml, config.restheart)

	implicit val system = ActorSystem("cpauth")
	implicit val dispatcher = system.dispatcher
	implicit val scheduler = system.scheduler
	implicit val materializer = ActorMaterializer(namePrefix = Some("cpauth_mat"))

	val http = Http()

	val assExtractorTry = AssertionExtractor(samlConfig)
	val idpLib: IdpLibrary = IdpLibrary.fromConfig(samlConfig)
	val cookieFactory = new CookieFactory(config)
	val targetLookup: TargetUrlLookup = new MapBasedUrlLookup
	val authenticator = Authenticator(publicAuthConfig)

	val userDb = Users
	system.registerOnTermination(Users.closeDb())

	val cpauthExceptionHandler = ExceptionHandler{
		case AuthenticationFailedException =>
			complete((StatusCodes.Forbidden, AuthenticationFailedException.getMessage))
		case ex =>
			val stack = ex.getStackTrace.map(_.toString).mkString("\n")
			complete((StatusCodes.InternalServerError, ex.getMessage + "\n" + stack))
	}

	val route = handleExceptions(cpauthExceptionHandler){
		staticRoute ~
		samlRoute ~
		passwordRoute ~
		drupalRoute ~
		restheartRoute ~
		get{
			path("logout")(logout) ~
			path("whoami"){
				user(uinfo => complete(uinfo)) ~ complete(StatusCodes.Unauthorized)
			} ~
			path("cpauthcookie"){
				cpauthCookie
			} ~
			pathEndOrSingleSlash{
				user(_ => redirect("/home/", StatusCodes.Found)) ~
				redirect("/login/", StatusCodes.Found)
			}
		}
	}

	http.bindAndHandle(route, "127.0.0.1", httpConfig.servicePrivatePort)
		.onSuccess{
			case binding =>
				sys.addShutdownHook{
					val doneFuture = binding.unbind()
						.flatMap(_ => system.terminate())(ExecutionContext.Implicits.global)
					Await.result(doneFuture, 3 seconds)
				}
				println(binding)
		}

}
