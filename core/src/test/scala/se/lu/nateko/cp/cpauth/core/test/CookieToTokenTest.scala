package se.lu.nateko.cp.cpauth.core.test

import org.scalatest.FunSuite

import se.lu.nateko.cp.cpauth.core._

class CookieToTokenTest extends FunSuite{

	test("Round trip works"){

		val original = SignedToken(
			AuthToken(
				UserInfo("Вася", "Påpkin", "vasja.pupkin@mail.org"),
				1503889594
			),
			Signature(Array(14, 120, -34))
		)

		val cookieContent = CookieToToken.constructCookieContent(original)

		val roundTripToken = CookieToToken.recoverToken(cookieContent)
		assert(roundTripToken.isSuccess)
		val roundTrip = roundTripToken.get

		assert(original.token === roundTrip.token)
		assert(original.signature.bytes === roundTrip.signature.bytes)
	}

}