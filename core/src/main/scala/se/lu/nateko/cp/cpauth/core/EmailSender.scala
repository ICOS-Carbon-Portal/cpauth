package se.lu.nateko.cp.cpauth.core

import play.twirl.api.Html
import javax.mail._
import javax.mail.internet._
import java.util.Date
import java.util.Properties
import org.slf4j.LoggerFactory

case class EmailConfig(
	smtpServer: String,
	username: String,
	password: String,
	fromAddress: String,
	logBccAddress: Option[String]
)

class EmailSender(config: EmailConfig):

	private val log = LoggerFactory.getLogger(getClass)

	def send(to: Seq[String], subject: String, body: Html, cc: Seq[String] = Nil): Unit =
		privateSend(to, subject, body.body, "text/html; charset=utf-8", cc)

	def sendText(to: Seq[String], subject: String, body: String, cc: Seq[String] = Nil): Unit =
		privateSend(to, subject, body, "text/plain; charset=utf-8", cc)

	private def privateSend(to: Seq[String], subject: String, body: String, mimeType: String, cc: Seq[String]): Unit =
		try{
			val message: Message = {
				val properties = new Properties()
				properties.put("mail.smtp.auth", "true")
				properties.put("mail.smtp.starttls.enable", "true")
				properties.put("mail.smtp.host", config.smtpServer)
				properties.put("mail.smtp.port", "587")
				val session = Session.getDefaultInstance(properties, new Authenticator{
					override def getPasswordAuthentication = new PasswordAuthentication(config.username, config.password)
				})
				new MimeMessage(session)
			}

			message.setFrom(new InternetAddress(config.fromAddress))
			message.setReplyTo(Array(new InternetAddress("do_not_reply@icos-cp.eu")))
			message.setSentDate(new Date())
			message.setSubject(subject)
			message.setContent(body, mimeType)

			to.foreach(r => message.addRecipient(Message.RecipientType.TO, new InternetAddress(r)))
			cc.foreach(r => message.addRecipient(Message.RecipientType.CC, new InternetAddress(r)))
			config.logBccAddress.foreach(
				r => message.addRecipient(Message.RecipientType.BCC, new InternetAddress(r))
			)

			Transport.send(message)
		}catch{
			case err: Throwable =>
				log.error("Mail sending failed", err)
				throw err
		}