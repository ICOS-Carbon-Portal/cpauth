package se.lu.nateko.cp.cpauth.core

import scala.util.Try
import scala.util.Success
import java.security.PublicKey
import java.time.Instant
import se.lu.nateko.cp.cpauth.core.PublicAuthConfig

case class UserId(email: String)

enum AuthSource:
	case Password, PasswordReset, Saml, Orcid, Facebook, AtmoAccess

case class AuthToken(userId: UserId, expiresOn: Long, source: AuthSource)

case class SignedToken(token: AuthToken, signature: Signature)

class Authenticator(key: PublicKey){

	def unwrapTrustedToken(token: SignedToken, trustedSources: Set[AuthSource]): Try[AuthToken] =
		if(!trustedSources.contains(token.token.source))
			Exceptions.failure(s"Authentication tokens originating from ${token.token.source} are not trusted by this application")
		else unwrapToken(token)

	def unwrapToken(token: SignedToken): Try[AuthToken] = {
		val message = token.token.toString

		Crypto.verifySignature(message, key, token.signature).flatMap(valid =>
			if(!valid)
				Exceptions.failure("Authentication token's signature is invalid")
			else if(Instant.now.toEpochMilli >= token.token.expiresOn)
				Exceptions.failure("Authentication token has expired")
			else Success(token.token)
		)
	}

}

object Authenticator{

	def apply(authConfig: PublicAuthConfig): Try[Authenticator] = {
		val keyLines = CoreUtils.getResourceLines(authConfig.publicKeyPath)
		for(
			key <- Crypto.publicFromPemLines(keyLines.toIndexedSeq, "EC")
		) yield new Authenticator(key)
	}

}
