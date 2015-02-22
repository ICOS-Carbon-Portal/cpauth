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

case class SignedToken(token: AuthToken, signature: String)

class Authenticator private(key: RSAPublicKey){

	def unwrapUserInfo(token: SignedToken): Try[UserInfo] = {
		if(!tokenIsFresh(token.token))
			Failure(new Exception("Authentication token has expired") with NoStackTrace)

		else signatureIsValid(token) match{
			case Success(true) => Success(token.token.userInfo)
			case Failure(err) => Failure(err)
			case Success(false) => Failure(new Exception("Authentication token's signature is wrong") with NoStackTrace)
		}
	}

	private def signatureIsValid(token: SignedToken): Try[Boolean] = {
		val message = token.token.toString
		val signature = new Signature(token.signature)
		Crypto.verifySignature(message, key, signature)
	}

	private def tokenIsFresh(token: AuthToken): Boolean = new DateTime().getMillis < token.expiresOn
}

object Authenticator{

	def apply(config: PublicAuthConfig): Try[Authenticator] = {
		val keyLines = CoreUtils.getResourceLines(config.publicKeyPath)
		for(
			key <- Crypto.rsaPublicFromPemLines(keyLines.toIndexedSeq)
		) yield new Authenticator(key)
	}

}
