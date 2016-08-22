package se.lu.nateko.cp.cpauth

import java.security.interfaces.RSAPrivateKey
import scala.util.Try
import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import se.lu.nateko.cp.cpauth.core._

class SignedTokenMaker private(key: RSAPrivateKey, validity: Int){

	def makeToken(userId: UserId): SignedToken = {
		val expiryTime = new DateTime().getMillis + 1000 * validity
		val token = AuthToken(userId, expiryTime)
		val signature = Crypto.signMessage(token.toString, key)
		SignedToken(token, signature)
	}

}

object SignedTokenMaker {

	def apply(config: PrivateAuthConfig): Try[SignedTokenMaker] = {
		val keyBytes = CoreUtils.getResourceBytes(config.privateKeyPath)
		for(
			key <- Crypto.rsaPrivateFromDerBytes(keyBytes)
		) yield new SignedTokenMaker(key, config.authTokenValiditySeconds)
	}

}