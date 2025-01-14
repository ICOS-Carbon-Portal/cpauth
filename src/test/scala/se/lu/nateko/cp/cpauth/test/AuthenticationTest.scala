package se.lu.nateko.cp.cpauth.test

import eu.icoscp.envri.Envri
import org.scalatest.funsuite.AnyFunSuite
import se.lu.nateko.cp.cpauth.PrivateAuthConfig
import se.lu.nateko.cp.cpauth.core.*
import se.lu.nateko.cp.cpauth.utils.SignedTokenMaker

import java.security.SignatureException

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
	val private2 = "src/test/resources/private2.der"

	test("Properly formed fresh token validates successfully"){

		val token = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = 10,
			privateKeyPaths = Map(ICOS -> private1)
		), "EC").get.makeToken(user, AuthSource.Password)
		
		val unwrappedToken = Authenticator("EC", pubAuthConfig).get.unwrapToken(token)

		assert(unwrappedToken.isSuccess)
		assert(unwrappedToken.get.userId === user)
	}

	test("Expired token is rejected"){
		val token = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = -1,
			privateKeyPaths = Map(ICOS -> private1)
		), "EC").get.makeToken(user, AuthSource.Password)
		
		val unwrappedToken = Authenticator("EC", pubAuthConfig).get.unwrapToken(token)

		assert(unwrappedToken.isFailure)

		val errMessage: String = unwrappedToken.failed.get.getMessage
		assert(errMessage.contains("expired"))
	}

	test("Token originating from an untrusted source is rejected"):
		val token = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = 10,
			privateKeyPaths = Map(ICOS -> private1)
		), "EC").get.makeToken(user, AuthSource.Saml)

		val err = intercept[CpauthException]:
			Authenticator("EC", pubAuthConfig).get
				.unwrapTrustedToken(token, Set(AuthSource.Password)).get

		assert(err.getMessage.contains("not trusted"))


	test("Token signed with a wrong key fails to authenticate"):

		val wrongTokenMaker = SignedTokenMaker(PrivateAuthConfig(
			authTokenValiditySeconds = 10,
			privateKeyPaths = Map(ICOS -> private2)
		), "EC").get

		val wrongToken = wrongTokenMaker.makeToken(user, AuthSource.Password)
		val unwrapped = Authenticator("EC", pubAuthConfig).get.unwrapToken(wrongToken)
		assert(unwrapped.isFailure)
		val errMessage = unwrapped.failed.get.getMessage
		assert(errMessage.contains("signature is invalid"))

}