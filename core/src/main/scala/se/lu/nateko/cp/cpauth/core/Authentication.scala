package se.lu.nateko.cp.cpauth.core

import scala.util.Try
import scala.util.Failure
import scala.util.Success
import java.security.interfaces.RSAPublicKey
import java.time.Instant

case class UserId(email: String)

object AuthSource extends Enumeration{
	type AuthSource = Value
	val Password, Saml, Orcid = Value
}

case class AuthToken(userId: UserId, expiresOn: Long, source: AuthSource.Value)

case class SignedToken(token: AuthToken, signature: Signature)

class Authenticator(key: RSAPublicKey){

	def unwrapUserId(token: SignedToken, trustedSources: AuthSource.ValueSet = AuthSource.values): Try[UserId] =
		if(Instant.now.toEpochMilli >= token.token.expiresOn)
			Exceptions.failure("Authentication token has expired")
		else if(!trustedSources.contains(token.token.source))
			Exceptions.failure(s"Authentication tokens originating from ${token.token.source} are not trusted by this application")
		else unwrapToken(token).map(_.userId)

	def unwrapToken(token: SignedToken): Try[AuthToken] = {
		val message = token.token.toString

		Crypto.verifySignature(message, key, token.signature).flatMap(valid => {
			if(valid) Success(token.token)
			else Exceptions.failure("Authentication token's signature is invalid")
		})
	}

}

object Authenticator{

	def apply(authConfig: PublicAuthConfig): Try[Authenticator] = {
		val keyLines = CoreUtils.getResourceLines(authConfig.publicKeyPath)
		for(
			key <- Crypto.rsaPublicFromPemLines(keyLines.toIndexedSeq)
		) yield new Authenticator(key)
	}

}
