package se.lu.nateko.cp.cpauth.test

import org.scalatest.funsuite.AnyFunSuite
import se.lu.nateko.cp.cpauth.core.*
import se.lu.nateko.cp.cpauth.utils.SignedTokenMaker
import se.lu.nateko.cp.cpauth.PrivateAuthConfig
import eu.icoscp.envri.Envri

class AuthenticationTest extends AnyFunSuite{
	import Envri.ICOS
	given Envri = ICOS

	val user = UserId("vasja.pupkin@mail.org")
	val pubAuthConfig = PublicAuthConfig(
		authCookieName = "",
		authCookieDomain = ".icos-cp.eu",
		authHost = "cpauth.icos-cp.eu",
		publicKeyPath = "/public1.pem"
	)

	val private1 = "src/test/resources/private1.der"
	val samlPrivate = "src/test/resources/saml/test_private_key.der"

	test("Properly formed fresh token validates successfully"){

		val token = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = 10,
			privateKeyPaths = Map(ICOS -> private1)
		)).get.makeToken(user, AuthSource.Password)
		
		val unwrappedToken = Authenticator(pubAuthConfig).get.unwrapToken(token)

		assert(unwrappedToken.isSuccess)
		assert(unwrappedToken.get.userId === user)
	}

	test("Expired token is rejected"){
		val token = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = -1,
			privateKeyPaths = Map(ICOS -> private1)
		)).get.makeToken(user, AuthSource.Password)
		
		val unwrappedToken = Authenticator(pubAuthConfig).get.unwrapToken(token)

		assert(unwrappedToken.isFailure)

		val errMessage: String = unwrappedToken.failed.get.getMessage
		assert(errMessage.contains("expired"))
	}

	test("Token originating from an untrusted source is rejected"){
		val token = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = 10,
			privateKeyPaths = Map(ICOS -> private1)
		)).get.makeToken(user, AuthSource.Saml)

		val unwrappedToken = Authenticator(pubAuthConfig).get.unwrapTrustedToken(token, Set(AuthSource.Password))

		assert(unwrappedToken.isFailure)

		val errMessage: String = unwrappedToken.failed.get.getMessage
		assert(errMessage.contains("not trusted"))
	}

	test("Token signed with a wrong key fails to authenticate"){

		val tokenMaker = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = 10,
			privateKeyPaths = Map(ICOS -> samlPrivate)
		)).get

		val auth = Authenticator(pubAuthConfig).get

		val unwrappedToken = auth.unwrapToken(tokenMaker.makeToken(user, AuthSource.Password))

		assert(unwrappedToken.isFailure)
		val errMessage: String = unwrappedToken.failed.get.getMessage
		assert(errMessage.contains("not correct"))
	}
}