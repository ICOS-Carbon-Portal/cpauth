package se.lu.nateko.cp.cpauth

import akka.actor.ActorSystem
import java.sql.DriverManager
import se.lu.nateko.cp.cpauth.accounts.JdbcUsers
import se.lu.nateko.cp.cpauth.core.AuthenticationFailedException
import se.lu.nateko.cp.cpauth.core.Authenticator
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
import se.lu.nateko.cp.cpauth.accounts.RestHeartClient
import se.lu.nateko.cp.cpauth.utils.TargetUrlLookup
import se.lu.nateko.cp.cpauth.utils.MapBasedUrlLookup
import se.lu.nateko.cp.cpauth.services._
import se.lu.nateko.cp.cpauth.oauth.FacebookAuthenticationService


object Main extends App with SamlRouting with PasswordRouting with DrupalRouting
		with StaticRouting with RestHeartRouting with OAuthRouting{
	val config: CpauthConfig = ConfigReader.getDefault
	val (httpConfig, publicAuthConfig, samlConfig, oauthConfig) = (config.http, config.auth.pub, config.saml, config.oauth)

	implicit val system = ActorSystem("cpauth")
	implicit val dispatcher = system.dispatcher
	val blockingExeContext  = system.dispatchers.lookup("my-blocking-dispatcher")
	implicit val scheduler = system.scheduler
	implicit val materializer = ActorMaterializer(namePrefix = Some("cpauth_mat"))

	val http = Http()
	val restHeart = new RestHeartClient(config.restheart, http)
	val facebookAuth = new FacebookAuthenticationService(oauthConfig.facebook, httpConfig.serviceHost)

	val assExtractorTry = AssertionExtractor(samlConfig)
	val idpLib: IdpLibrary = IdpLibrary.fromConfig(samlConfig)
	val cookieFactory = new CookieFactory(config)

	val userDb = new JdbcUsers( () => {
		Class.forName("org.hsqldb.jdbc.JDBCDataSource")
		DriverManager.getConnection("jdbc:hsqldb:mem:test")
	})

	val passwordHandler = {
		val emailSender = new EmailSender(config.mailing)
		implicit val exeCtxt = blockingExeContext
		new PasswordLifecycleHandler(emailSender, cookieFactory, userDb, config.http)
	}
	val targetLookup: TargetUrlLookup = new MapBasedUrlLookup
	val authenticator = Authenticator(publicAuthConfig)

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
		oauthRoute ~
		get{
			path("logout")(logout) ~
			path("whoami"){whoami} ~
			path("cpauthcookie"){cpauthCookie} ~
			pathEndOrSingleSlash{
				token(_ => redirect("/home/", StatusCodes.Found)) ~
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
			system.log.info(s"Started cpauth: $binding")
	}

}
