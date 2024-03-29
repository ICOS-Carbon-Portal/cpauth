package se.lu.nateko.cp.cpauth.core.test

import org.scalatest.funsuite.AnyFunSuite

import se.lu.nateko.cp.cpauth.core.*

class CookieToTokenTest extends AnyFunSuite{

	test("Round trip works"){

		val original = SignedToken(
			AuthToken(
				UserId("vasja.pupkin@mail.org"),
				1503889594,
				AuthSource.Password
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