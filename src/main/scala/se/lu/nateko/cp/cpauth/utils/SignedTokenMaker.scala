package se.lu.nateko.cp.cpauth.utils

import java.nio.file.Files
import java.nio.file.Paths
import java.security.PrivateKey

import scala.util.Try

import org.joda.time.DateTime

import eu.icoscp.envri.Envri
import se.lu.nateko.cp.cpauth.PrivateAuthConfig
import se.lu.nateko.cp.cpauth.core.*

class SignedTokenMaker private(key: PrivateKey, validity: Int):
	def makeToken(userId: UserId, source: AuthSource): SignedToken = {
		val expiryTime = new DateTime().getMillis + 1000 * validity
		val token = AuthToken(userId, expiryTime, source)
		val signature = Crypto.signMessage(token.toString, key)
		SignedToken(token, signature)
	}


object SignedTokenMaker:

	def apply(config: PrivateAuthConfig, ktype: Crypto.KeyType)(using Envri): Try[SignedTokenMaker] =
		val keyBytes = Files.readAllBytes(Paths.get(config.privateKeyPath))
		for(
			key <- Crypto.privateFromDerBytes(keyBytes, ktype)
		) yield new SignedTokenMaker(key, config.authTokenValiditySeconds)
