package se.lu.nateko.cp.cpauth

import akka.actor.ActorSystem
import java.sql.DriverManager
import se.lu.nateko.cp.cpauth.accounts.JdbcUsers
import se.lu.nateko.cp.cpauth.core.AuthenticationFailedException
import se.lu.nateko.cp.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cp.cpauth.routing._
import scala.concurrent.duration.DurationInt
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import se.lu.nateko.cp.cpauth.accounts.RestHeartClient
import se.lu.nateko.cp.cpauth.utils.TargetUrlLookup
import se.lu.nateko.cp.cpauth.utils.MapBasedUrlLookup
import se.lu.nateko.cp.cpauth.services._
import scala.util.Success
import scala.util.Failure
import utils.Utils.CrasheableTry


object Main extends App with SamlRouting with PasswordRouting with DrupalRouting
		with StaticRouting with RestHeartRouting with OAuthRouting{

	implicit val system = ActorSystem("cpauth")
	implicit val dispatcher = system.dispatcher
	implicit val scheduler = system.scheduler
	implicit val materializer = ActorMaterializer(namePrefix = Some("cpauth_mat"))

	val config: CpauthConfig = ConfigReader.getDefault.getOrCrash("Problem reading/parsing config file")

	val (httpConfig, publicAuthConfigs, samlConfig, oauthConfig) = (config.http, config.auth.pub, config.saml, config.oauth)

	val hostToEnvri = httpConfig.serviceHosts.map(_.swap)
	val http = Http()
	val restHeart = new RestHeartClient(config.restheart, http)

	val idpLib: IdpLibrary = IdpLibrary.fromConfig(samlConfig).getOrCrash("Try running 'fetchIdpList' in SBT.")
	val cookieFactory = new CookieFactory(config)

	Class.forName(config.database.driver)
	val userDb = new JdbcUsers( () => {
		DriverManager.getConnection(
			config.database.url,
			config.database.user,
			config.database.password)
	})

	val passwordHandler = {
		val emailSender = new EmailSender(config.mailing)
		implicit val exeCtxt = system.dispatchers.lookup("my-blocking-dispatcher")
		new PasswordLifecycleHandler(emailSender, cookieFactory, userDb, config.http)
	}
	val targetLookup: TargetUrlLookup = new MapBasedUrlLookup

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
		facebookRoute ~
		orcidRoute ~
		get{
			path("logout")(logout) ~
			path("whoami"){whoami} ~
			path("cpauthcookie"){cpauthCookie} ~
			pathEndOrSingleSlash{
				token(_ => redirect("/home/", StatusCodes.Found)) ~
				redirect("/login/", StatusCodes.Found)
			} ~
			path("buildInfo"){complete(BuildInfo.toString)}
		}
	}

	restHeart.init.flatMap{_ =>
		http.bindAndHandle(route, "127.0.0.1", httpConfig.servicePrivatePort)
	}.onComplete{
		case Success(binding) =>
			sys.addShutdownHook{
				val doneFuture = binding.unbind()
					.flatMap(_ => system.terminate())(ExecutionContext.Implicits.global)
				Await.result(doneFuture, 3.seconds)
			}
			system.log.info(s"Started cpauth: $binding")
		case Failure(err) =>
			system.log.error(err, "Could not start 'cpauth' service")
			system.terminate()
	}

}
