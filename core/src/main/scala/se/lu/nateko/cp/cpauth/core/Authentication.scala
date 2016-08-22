package se.lu.nateko.cp.cpauth.core

import scala.util.Try
import scala.util.Failure
import scala.util.Success
import java.security.interfaces.RSAPublicKey
import java.time.Instant

case class UserId(email: String)

case class AuthToken(userId: UserId, expiresOn: Long)

case class SignedToken(token: AuthToken, signature: Signature)

class Authenticator(key: RSAPublicKey){

	def unwrapUserId(token: SignedToken): Try[UserId] =
		if(tokenIsOld(token.token))
			Exceptions.failure("Authentication token has expired")
		else signatureIsValid(token) match{
			case Success(true) => Success(token.token.userId)
			case Failure(err) => Failure(err)
			case Success(false) => Exceptions.failure("Authentication token's signature is invalid")
		}

	private def signatureIsValid(token: SignedToken): Try[Boolean] = {
		val message = token.token.toString
		Crypto.verifySignature(message, key, token.signature)
	}

	private def tokenIsOld(token: AuthToken): Boolean = Instant.now.toEpochMilli >= token.expiresOn
}

object Authenticator{

	def apply(authConfig: PublicAuthConfig): Try[Authenticator] = {
		val keyLines = CoreUtils.getResourceLines(authConfig.publicKeyPath)
		for(
			key <- Crypto.rsaPublicFromPemLines(keyLines.toIndexedSeq)
		) yield new Authenticator(key)
	}

}
