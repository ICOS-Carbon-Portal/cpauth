package se.lu.nateko.cp.cpauth.test

import org.scalatest.FunSuite

import se.lu.nateko.cp.cpauth.core._
import se.lu.nateko.cp.cpauth.SignedTokenMaker

class AuthenticationTest extends FunSuite{

	val user = UserInfo("Vasja", "Pupkin", "vasja.pupkin@mail.org")
	
	test("Properly formed fresh token validates successfully"){

		val token = SignedTokenMaker(new PrivateAuthConfig{
			val authTokenValiditySeconds = 10
			val privateKeyPath = "/private1.der"
		}).get.makeToken(user)
		
		val unwrappedUser = Authenticator(new PublicAuthConfig{
			val publicKeyPath = "/public1.pem"
		}).get.unwrapUserInfo(token)

		assert(unwrappedUser.isSuccess)
		assert(unwrappedUser.get === user)
	}

	test("Expired token is rejected"){
		val token = SignedTokenMaker(new PrivateAuthConfig{
			val authTokenValiditySeconds = -1
			val privateKeyPath = "/private1.der"
		}).get.makeToken(user)
		
		val unwrappedUser = Authenticator(new PublicAuthConfig{
			val publicKeyPath = "/public1.pem"
		}).get.unwrapUserInfo(token)

		assert(unwrappedUser.isFailure)

		val errMessage: String = unwrappedUser.failed.get.getMessage
		assert(errMessage.contains("expired"))
	}

	test("Token signed with a wrong key fails to authenticate"){

		val tokenMaker = SignedTokenMaker(new PrivateAuthConfig{
			val authTokenValiditySeconds: Int = 10
			val privateKeyPath: String = "/saml/test_private_key.der"
		}).get

		val auth = Authenticator(new PublicAuthConfig{
			val publicKeyPath: String = "/public1.pem"
		}).get

		val unwrappedUser = auth.unwrapUserInfo(tokenMaker.makeToken(user))

		assert(unwrappedUser.isFailure)
		val errMessage: String = unwrappedUser.failed.get.getMessage
		assert(errMessage.contains("not correct"))
	}
}