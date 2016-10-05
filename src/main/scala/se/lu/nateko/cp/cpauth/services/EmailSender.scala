package se.lu.nateko.cp.cpauth.services

import se.lu.nateko.cp.cpauth.EmailConfig
import play.twirl.api.Html
import javax.mail._
import javax.mail.internet._
import java.util.Date
import java.util.Properties
import org.slf4j.LoggerFactory

class EmailSender(config: EmailConfig) {

	private val log = LoggerFactory.getLogger(getClass)

	def send(to: Seq[String], subject: String, body: Html, cc: Seq[String] = Nil): Unit = {
		try{
			val message: Message = {
				val properties = new Properties()
				properties.put("mail.smtp.host", config.smtpServer)
				val session = Session.getDefaultInstance(properties, null)
				new MimeMessage(session)
			}

			message.setFrom(new InternetAddress(config.fromAddress))
			message.setSentDate(new Date())
			message.setSubject(subject)
			message.setContent(body.body, "text/html; charset=utf-8")

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
	}

}