package se.lu.nateko.cp.cpauth.services

import akka.actor.Scheduler
import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.AuthConfig
import se.lu.nateko.cp.cpauth.HttpConfig
import se.lu.nateko.cp.cpauth.accounts.UserEntry
import se.lu.nateko.cp.cpauth.accounts.UsersIo
import se.lu.nateko.cp.cpauth.core.AuthSource
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.utils.Utils

import java.net.URI
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import se.lu.nateko.cp.cpauth.core.EmailSender

class PasswordLifecycleHandler(
	emailSender: EmailSender,
	cookieFactory: CookieFactory,
	userDb: UsersIo,
	httpConf: HttpConfig,
	authConf: AuthConfig
)(using ExecutionContext, Scheduler):

	def sendResetEmail(uid: UserId)(using envri: Envri): Future[Unit] =

		val tokenTry = cookieFactory.makeTokenBase64(uid, AuthSource.PasswordReset)

		Future.fromTry(tokenTry).map{token =>
			val link = new URI("https", httpConf.serviceHost, "/password/initpassreset/" + token, null)
			val message = views.html.CpauthPassResetEmail(uid.email, link)
			val name = envri match
				case Envri.ICOS       => "ICOS Carbon Portal"
				case Envri.ICOSCities => "ICOS Carbon Portal"
				case Envri.SITES      => "SITES"
			emailSender.send(Seq(uid.email), "Create/reset your " + name + " password", message)
		}

	def setPassword(uid: UserId, newPassword: String): Future[Unit] = {
		userDb.userExists(uid).flatMap(exists =>
			if(exists)
				userDb.userIsAdmin(uid).flatMap(isAdmin =>
					userDb.updateUser(uid, UserEntry(uid, isAdmin), newPassword)
				)
			else
				userDb.addUser(UserEntry(uid, false), newPassword)
		)
	}

	def changePassword(uid: UserId, oldPassword: String, newPassword: String): Future[Unit] = for(
		userEntry <- authUser(uid, oldPassword);
		_ <- userDb.updateUser(uid, userEntry, newPassword)
	) yield ()

	def authUser(uid: UserId, password: String): Future[UserEntry] =
		val entryFut =
			if authConf.masterAdminUser == uid.email && password == authConf.masterAdminPass
			then Future.successful(UserEntry(uid, true))
			else userDb.authenticateUser(uid, password)
		Utils.slowFailureDown(entryFut, 500.millis)

end PasswordLifecycleHandler
