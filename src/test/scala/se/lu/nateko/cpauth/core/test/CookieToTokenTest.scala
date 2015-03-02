package se.lu.nateko.cpauth.core.test

import org.scalatest.FunSuite

import se.lu.nateko.cpauth.core.AuthToken
import se.lu.nateko.cpauth.core.CookieToToken.{recoverToken, constructCookieContent}
import se.lu.nateko.cpauth.core.Signature
import se.lu.nateko.cpauth.core.SignedToken
import se.lu.nateko.cpauth.core.UserInfo

import spray.http.HttpCookie

class CookieToTokenTest extends FunSuite{

	test("Round trip works"){

		val original = SignedToken(
			AuthToken(
				UserInfo("Вася", "Påpkin", "vasja.pupkin@mail.org"),
				1503889594
			),
			Signature(Array(14, 120, -34))
		)

		val cookieContent = constructCookieContent(original)

		val cookie = HttpCookie("cpauthToken", cookieContent)

		val roundTripToken = recoverToken(cookie)
		assert(roundTripToken.isSuccess)
		val roundTrip = roundTripToken.get

		assert(original.token === roundTrip.token)
		assert(original.signature.bytes === roundTrip.signature.bytes)
	}

}