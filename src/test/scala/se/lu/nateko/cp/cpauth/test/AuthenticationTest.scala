package se.lu.nateko.cp.cpauth.test

import org.scalatest.FunSuite
import se.lu.nateko.cp.cpauth.core._
import se.lu.nateko.cp.cpauth.SignedTokenMaker
import se.lu.nateko.cp.cpauth.PrivateAuthConfig

class AuthenticationTest extends FunSuite{

	val user = UserInfo("Vasja", "Pupkin", "vasja.pupkin@mail.org")
	val pubAuthConfig = PublicAuthConfig(authCookieName = "", publicKeyPath = "/public1.pem")
	
	test("Properly formed fresh token validates successfully"){

		val token = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = 10,
			privateKeyPath = "/private1.der"
		)).get.makeToken(user)
		
		val unwrappedUser = Authenticator(pubAuthConfig).get.unwrapUserInfo(token)

		assert(unwrappedUser.isSuccess)
		assert(unwrappedUser.get === user)
	}

	test("Expired token is rejected"){
		val token = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = -1,
			privateKeyPath = "/private1.der"
		)).get.makeToken(user)
		
		val unwrappedUser = Authenticator(pubAuthConfig).get.unwrapUserInfo(token)

		assert(unwrappedUser.isFailure)

		val errMessage: String = unwrappedUser.failed.get.getMessage
		assert(errMessage.contains("expired"))
	}

	test("Token signed with a wrong key fails to authenticate"){

		val tokenMaker = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = 10,
			privateKeyPath = "/saml/test_private_key.der"
		)).get

		val auth = Authenticator(pubAuthConfig).get

		val unwrappedUser = auth.unwrapUserInfo(tokenMaker.makeToken(user))

		assert(unwrappedUser.isFailure)
		val errMessage: String = unwrappedUser.failed.get.getMessage
		assert(errMessage.contains("not correct"))
	}
}