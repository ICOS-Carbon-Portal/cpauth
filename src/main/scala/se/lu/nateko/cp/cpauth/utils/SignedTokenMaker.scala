package se.lu.nateko.cp.cpauth.utils

import java.nio.file.Files
import java.nio.file.Paths
import java.security.interfaces.RSAPrivateKey

import scala.util.Try

import org.joda.time.DateTime

import se.lu.nateko.cp.cpauth.Envri.Envri
import se.lu.nateko.cp.cpauth.PrivateAuthConfig
import se.lu.nateko.cp.cpauth.core._

class SignedTokenMaker private(key: RSAPrivateKey, validity: Int){

	def makeToken(userId: UserId, source: AuthSource.Value): SignedToken = {
		val expiryTime = new DateTime().getMillis + 1000 * validity
		val token = AuthToken(userId, expiryTime, source)
		val signature = Crypto.signMessage(token.toString, key)
		SignedToken(token, signature)
	}

}

object SignedTokenMaker {

	def apply(config: PrivateAuthConfig)(implicit envri: Envri): Try[SignedTokenMaker] = {
		val keyBytes = Files.readAllBytes(Paths.get(config.privateKeyPath))
		for(
			key <- Crypto.rsaPrivateFromDerBytes(keyBytes)
		) yield new SignedTokenMaker(key, config.authTokenValiditySeconds)
	}

}