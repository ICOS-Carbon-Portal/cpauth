package se.lu.nateko.cpauth.core

import org.joda.time.DateTime
import scala.util.Try
import org.apache.commons.codec.binary.Base64
import scala.util.Failure
import scala.util.control.NoStackTrace
import scala.util.Success
import java.security.interfaces.RSAPublicKey

case class UserInfo(givenName: String, surname: String, mail: String)

case class AuthToken(userInfo: UserInfo, expiresOn: Long)

case class SignedToken(token: AuthToken, signature: Signature)

class Authenticator private(key: RSAPublicKey){

	def unwrapUserInfo(token: SignedToken): Try[UserInfo] = {
		if(tokenIsOld(token.token))
			Failure(new Exception("Authentication token has expired") with NoStackTrace)

		else signatureIsValid(token) match{
			case Success(true) => Success(token.token.userInfo)
			case Failure(err) => Failure(err)
			case Success(false) => Failure(new Exception("Authentication token's signature is invalid") with NoStackTrace)
		}
	}

	private def signatureIsValid(token: SignedToken): Try[Boolean] = {
		val message = token.token.toString
		Crypto.verifySignature(message, key, token.signature)
	}

	private def tokenIsOld(token: AuthToken): Boolean = new DateTime().getMillis >= token.expiresOn
}

object Authenticator{

	def apply(config: PublicAuthConfig): Try[Authenticator] = {
		val keyLines = CoreUtils.getResourceLines(config.publicKeyPath)
		for(
			key <- Crypto.rsaPublicFromPemLines(keyLines.toIndexedSeq)
		) yield new Authenticator(key)
	}

}
