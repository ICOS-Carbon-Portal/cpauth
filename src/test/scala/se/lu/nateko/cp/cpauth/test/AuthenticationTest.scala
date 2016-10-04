package se.lu.nateko.cp.cpauth.test

import org.scalatest.FunSuite
import se.lu.nateko.cp.cpauth.core._
import se.lu.nateko.cp.cpauth.SignedTokenMaker
import se.lu.nateko.cp.cpauth.PrivateAuthConfig

class AuthenticationTest extends FunSuite{

	val user = UserId("vasja.pupkin@mail.org")
	val pubAuthConfig = PublicAuthConfig(authCookieName = "", publicKeyPath = "/public1.pem")
	
	test("Properly formed fresh token validates successfully"){

		val token = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = 10,
			privateKeyPath = "/private1.der"
		)).get.makeToken(user, AuthSource.Password)
		
		val unwrappedToken = Authenticator(pubAuthConfig).get.unwrapToken(token)

		assert(unwrappedToken.isSuccess)
		assert(unwrappedToken.get.userId === user)
	}

	test("Expired token is rejected"){
		val token = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = -1,
			privateKeyPath = "/private1.der"
		)).get.makeToken(user, AuthSource.Password)
		
		val unwrappedToken = Authenticator(pubAuthConfig).get.unwrapToken(token)

		assert(unwrappedToken.isFailure)

		val errMessage: String = unwrappedToken.failed.get.getMessage
		assert(errMessage.contains("expired"))
	}

	test("Token originating from an untrusted source is rejected"){
		val token = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = 10,
			privateKeyPath = "/private1.der"
		)).get.makeToken(user, AuthSource.Saml)

		val unwrappedToken = Authenticator(pubAuthConfig).get.unwrapTrustedToken(token, AuthSource.ValueSet(AuthSource.Password))

		assert(unwrappedToken.isFailure)

		val errMessage: String = unwrappedToken.failed.get.getMessage
		assert(errMessage.contains("not trusted"))
	}

	test("Token signed with a wrong key fails to authenticate"){

		val tokenMaker = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = 10,
			privateKeyPath = "/saml/test_private_key.der"
		)).get

		val auth = Authenticator(pubAuthConfig).get

		val unwrappedToken = auth.unwrapToken(tokenMaker.makeToken(user, AuthSource.Password))

		assert(unwrappedToken.isFailure)
		val errMessage: String = unwrappedToken.failed.get.getMessage
		assert(errMessage.contains("not correct"))
	}
}