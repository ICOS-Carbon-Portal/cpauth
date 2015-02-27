package se.lu.nateko.cpauth.core

import scala.util.Try
import spray.http.HttpCookie
import spray.json._
import org.apache.commons.codec.binary.Base64
import java.nio.charset.StandardCharsets

object CookieToToken extends DefaultJsonProtocol {

	implicit val userInfoFormat = jsonFormat3(UserInfo)
	implicit val tokenFormat = jsonFormat2(AuthToken)
	implicit val signedTokenFormat = jsonFormat2(SignedToken)

	def recoverToken(cookie: HttpCookie): Try[SignedToken] = Try{
		val base64 = cookie.content
		val json = CoreUtils.decompressFromBase64(base64)
		json.parseJson.convertTo[SignedToken]
	}

	def constructCookieContent(token: SignedToken): String = {
		val json = token.toJson.compactPrint
		CoreUtils.compressAndBase64(json, true)
	}
}