package se.lu.nateko.cp.cpauth.core

import scala.util.Try
import spray.http.HttpCookie
import org.apache.commons.codec.binary.Base64
import java.io.ObjectOutputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

object CookieToToken {

	def recoverToken(cookie: HttpCookie): Try[SignedToken] = Try{
		val base64 = cookie.content
		val serialized: Array[Byte] = Base64.decodeBase64(base64)
		val is = new ByteArrayInputStream(serialized)
		val ois = new ObjectInputStream(is)
		
		val expiresOn = ois.readLong()
		val givenName = ois.readObject().asInstanceOf[String]
		val surname = ois.readObject().asInstanceOf[String]
		val mail = ois.readObject().asInstanceOf[String]
		val signatureBytes = ois.readObject().asInstanceOf[Array[Byte]]
		ois.close()

		SignedToken(
			AuthToken(
				UserInfo(givenName, surname, mail),
				expiresOn
			),
			Signature(signatureBytes)
		)
	}

	def constructCookieContent(token: SignedToken): String = {
		val byteStream = new ByteArrayOutputStream()
		val oos = new ObjectOutputStream(byteStream)

		oos.writeLong(token.token.expiresOn)
		oos.writeObject(token.token.userInfo.givenName)
		oos.writeObject(token.token.userInfo.surname)
		oos.writeObject(token.token.userInfo.mail)
		oos.writeObject(token.signature.bytes)
		oos.close()
		val serialized = byteStream.toByteArray
		Base64.encodeBase64String(serialized)
	}
}