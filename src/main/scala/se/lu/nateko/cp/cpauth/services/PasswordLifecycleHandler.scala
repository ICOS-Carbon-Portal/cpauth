package se.lu.nateko.cp.cpauth.services

import scala.concurrent.Future
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.core.AuthSource
import scala.concurrent.ExecutionContext
import se.lu.nateko.cp.cpauth.HttpConfig
import java.net.URI

class PasswordLifecycleHandler(emailSender: EmailSender, cookieFactory: CookieFactory, config: HttpConfig) {

	def sendResetEmail(uid: UserId)(implicit ctxt: ExecutionContext): Future[Unit] = {

		val tokenTry = cookieFactory.makeTokenBase64(uid, AuthSource.PasswordReset)

		Future.fromTry(tokenTry).map(token => {
			val link = new URI("https", config.serviceHost, "/password/initpassreset/" + token, null)
			val message = views.html.CpauthPassResetEmail(uid.email, link)
			emailSender.send(Seq(uid.email), "Create/reset you Carbon Portal password", message)
		})
	}
}