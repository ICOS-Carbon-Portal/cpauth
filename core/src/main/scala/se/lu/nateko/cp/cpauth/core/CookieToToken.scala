package se.lu.nateko.cp.cpauth.core

import scala.util.Try
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import spray.json.*
import java.nio.charset.StandardCharsets
import scala.util.control.NoStackTrace


object CookieToToken:

	val OpenBracket:  Byte = 91 // [ character
	val CloseBracket: Byte = 93 // ] character
	val RecordSep:    Byte = 30 // (record separator) character

	def recoverToken(base64: String): Try[SignedToken] = Try{
		val bytes: Array[Byte] = CoreUtils.decodeBase64(base64)
		if bytes(0) == OpenBracket then
			val jsLength = bytes.indices.find{i =>
				i > 0 && bytes(i - 1) == CloseBracket && bytes(i) == RecordSep
			}.getOrElse(
				throw cpauthException(s"Bad token, cannot read JSON array")
			)
			val js = new String(bytes.take(jsLength), StandardCharsets.UTF_8).parseJson
			js match
				case JsArray(elems) => elems.toList match
					case JsNumber(validTo) :: JsString(email) :: JsString(source) :: Nil =>
						val authToken = AuthToken(UserId(email), validTo.longValue, AuthSource.valueOf(source))
						SignedToken(authToken, Signature(bytes.drop(jsLength + 1)))
					case _ =>
						throw cpauthException("Bad token, invalid JSON array")
				case _ =>
					throw cpauthException("Bad token, the JSON was not an array")
		else
			readOldToken(bytes)
	}.recoverWith{
		case err: Throwable => Exceptions.failure(s"Failed to parse authentication token: ${err.getMessage()}")
	}

	private def readOldToken(serialized: Array[Byte]): SignedToken =
		val is = new ByteArrayInputStream(serialized)
		val ois = new ObjectInputStream(is)
		
		val expiresOn = ois.readLong()
		val email = ois.readObject().asInstanceOf[String]
		val authSource = AuthSource.valueOf(ois.readObject().asInstanceOf[String])
		val signatureBytes = ois.readObject().asInstanceOf[Array[Byte]]
		ois.close()

		SignedToken(
			AuthToken(
				UserId(email),
				expiresOn,
				authSource
			),
			Signature(signatureBytes)
		)

	def constructCookieContent(token: SignedToken): String =
		val t = token.token
		val tokenJs = JsArray(JsNumber(t.expiresOn), JsString(t.userId.email), JsString(t.source.toString))

		val tokenBytes = Array.concat(
			tokenJs.compactPrint.getBytes(StandardCharsets.UTF_8),
			Array(RecordSep),
			token.signature.bytes
		)

		CoreUtils.encodeToBase64String(tokenBytes)

end CookieToToken
