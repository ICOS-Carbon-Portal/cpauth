package se.lu.nateko.cp.cpauth

import akka.actor.ActorSystem
import java.sql.DriverManager
import se.lu.nateko.cp.cpauth.accounts.JdbcUsers
import se.lu.nateko.cp.cpauth.core.AuthenticationFailedException
import se.lu.nateko.cp.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cp.cpauth.routing.*
import scala.concurrent.duration.DurationInt
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.Http
import akka.stream.Materializer
import se.lu.nateko.cp.cpauth.accounts.RestHeartClient
import se.lu.nateko.cp.cpauth.utils.TargetUrlLookup
import se.lu.nateko.cp.cpauth.utils.MapBasedUrlLookup
import se.lu.nateko.cp.cpauth.services.*
import scala.util.Success
import scala.util.Failure
import utils.Utils.CrasheableTry
import akka.actor.Scheduler

object Main extends App with SamlRouting with PasswordRouting with DrupalRouting
		with StaticRouting with RestHeartRouting with OAuthRouting with PortalLogRouting {

	given system: ActorSystem = ActorSystem("cpauth")
	given dispatcher: ExecutionContext = system.dispatcher
	given scheduler: Scheduler = system.scheduler

	val config: CpauthConfig = ConfigReader.getDefault.getOrCrash("Problem reading/parsing config file")

	val (httpConfig, authConfig, samlConfig, oauthConfig) = (config.http, config.auth, config.saml, config.oauth)
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

	val emailSender = new EmailSender(config.mailing)

	val geoClient = {
		val errorMailer = new ErrorEmailer(config.geoip.emailErrorsTo, "Resolving IP to location failed", emailSender)
		new CpGeoClient(config.geoip, errorMailer)
	}

	val passwordHandler = {
		implicit val exeCtxt = system.dispatchers.lookup("my-blocking-dispatcher")
		new PasswordLifecycleHandler(emailSender, cookieFactory, userDb, config.http, config.auth)
	}
	val targetLookup: TargetUrlLookup = new MapBasedUrlLookup

	val portalLogger = new PortalLogger(geoClient, config.restheart, config.postgres)

	val cpauthExceptionHandler = ExceptionHandler{
		case AuthenticationFailedException =>
			complete((StatusCodes.Forbidden, AuthenticationFailedException.getMessage))
		case ex =>
			val stack = ex.getStackTrace.map(_.toString).mkString("\n")
			complete((StatusCodes.InternalServerError, ex.getMessage + "\n" + stack))
	}

	val route = handleExceptions(cpauthExceptionHandler){
		staticRoute ~
		portalLogRoute ~
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

	restHeart.init.zip(userDb.init()).flatMap{_ =>
		http.newServerAt(httpConfig.serviceInterface, httpConfig.servicePrivatePort).bindFlow(route)
	}.onComplete{
		case Success(binding) =>
			sys.addShutdownHook{
				Await.result(binding.unbind(), 3.seconds)
				println("cpauth has been taken offline successfully")
			}
			system.log.info(s"Started cpauth: $binding")
		case Failure(err) =>
			system.log.error(err, "Could not start 'cpauth' service")
			system.terminate()
	}

	private val host2ToEnvri = httpConfig.serviceHosts.map{
		case (envri, host) => (host2SecondLevel(host), envri)
	}

	def hostToEnvri(host: String) = host2ToEnvri.get(host2SecondLevel(host))

	private def host2SecondLevel(host: String): String = host.count(_ == '.') match{
		case 0 => host
		case x => host.split('.').drop(x - 1).mkString(".")
	}
}
