package se.lu.nateko.cp.cpauth

import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown
import akka.actor.Scheduler
import akka.Done
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.Materializer
import se.lu.nateko.cp.cpauth.accounts.JdbcUsers
import se.lu.nateko.cp.cpauth.accounts.RestHeartClient
import se.lu.nateko.cp.cpauth.core.AuthenticationFailedException
import se.lu.nateko.cp.cpauth.core.EmailSender
import se.lu.nateko.cp.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cp.cpauth.routing.*
import se.lu.nateko.cp.cpauth.services.*
import se.lu.nateko.cp.cpauth.utils.MapBasedUrlLookup
import se.lu.nateko.cp.cpauth.utils.TargetUrlLookup
import eu.icoscp.geoipclient.CpGeoClient
import eu.icoscp.geoipclient.ErrorEmailer
import se.lu.nateko.cp.cpauth.routing.PortalLogRouting

import java.sql.DriverManager

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.control.NonFatal
import scala.util.Success

import utils.Utils.getOrCrash

object Main extends App with SamlRouting with PasswordRouting with DrupalRouting
		with StaticRouting with RestHeartRouting with OAuthRouting with PortalLogRouting:

	given system: ActorSystem = ActorSystem("cpauth")
	private val log = system.log
	given dispatcher: ExecutionContext = system.dispatcher
	given scheduler: Scheduler = system.scheduler

	val config: CpauthConfig = ConfigReader.getDefault.getOrCrash("Problem reading/parsing config file")
	val (httpConfig, authConfig, samlConfig, oauthConfig) = (config.http, config.auth, config.saml, config.oauth)
	val http = Http()

	val idpLib: IdpLibrary = IdpLibrary.fromConfig(samlConfig).getOrCrash("Try running 'fetchIdpList' in SBT.")
	val cookieFactory = new CookieFactory(config, log)

	Class.forName(config.database.driver)

	val userDb = new JdbcUsers( () => {
		DriverManager.getConnection(
			config.database.url,
			config.database.user,
			config.database.password)
	})

	val emailSender = EmailSender(config.mailing)

	val restHeart =
		val geoClient = CpGeoClient(emailSender)
		new RestHeartClient(config.restheart, geoClient, http)

	val proxyConnPoolSettings = restHeart.connPoolSetts

	val passwordHandler =
		given ExecutionContext = system.dispatchers.lookup("my-blocking-dispatcher")
		PasswordLifecycleHandler(emailSender, cookieFactory, userDb, config.http, config.auth)

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
		portalLogRoute ~
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
			} ~
			path("buildInfo"){complete(BuildInfo.toString)}
		}
	}

	restHeart.init.zip(userDb.init()).flatMap{_ =>
		http.newServerAt(httpConfig.serviceInterface, httpConfig.servicePrivatePort).bindFlow(route)
	}.onComplete{
		case Success(binding) =>
			val shutdown = CoordinatedShutdown(system)
			shutdown.addTask(CoordinatedShutdown.PhaseServiceUnbind, "unbind-http-server"){() =>
				binding.unbind().map(_ => Done)
			}
			shutdown.addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate, "shutdown-user-db"){() =>
				userDb.shutdown()
					.map(_ => Done)
					.recover{
						case NonFatal(err) =>
							log.warning(s"Could not shut down embedded DB cleanly: ${err.getMessage}")
							Done
					}
			}
			shutdown.addTask(CoordinatedShutdown.PhaseActorSystemTerminate, "log-shutdown-complete"){() =>
				Future.successful{
					println("cpauth has been taken offline successfully")
					Done
				}
			}
			log.info(s"Started cpauth: $binding")
		case Failure(err) =>
			log.error(err, "Could not start 'cpauth' service")
			system.terminate()
	}

	private val host2Envri = httpConfig.serviceHosts.map{
		case (envri, host) => (host, envri)
	}

	def hostToEnvri(host: String) = host2Envri.get(host)

end Main
