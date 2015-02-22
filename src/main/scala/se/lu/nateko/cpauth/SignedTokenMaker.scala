package se.lu.nateko.cpauth

import java.security.interfaces.RSAPrivateKey

import scala.util.Try

import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime

import se.lu.nateko.cpauth.core._

class SignedTokenMaker private(key: RSAPrivateKey, validity: Int){

	def makeToken(userInfo: UserInfo): SignedToken = {
		val expiryTime = new DateTime().getMillis + 1000 * validity
		val token = AuthToken(userInfo, expiryTime)
		val signature = Crypto.signMessage(token.toString, key).base64
		SignedToken(token, signature)
	}

}

object SignedTokenMaker {

	def apply(config: PrivateAuthConfig): Try[SignedTokenMaker] = {
		val keyBytes = CoreUtils.getResourceBytes(config.privateKeyPath)
		for(
			key <- Crypto.rsaPrivateFromDerBytes(keyBytes)
		) yield new SignedTokenMaker(key, config.validitySeconds)
	}

}