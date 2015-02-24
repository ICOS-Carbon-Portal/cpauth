package se.lu.nateko.cpauth.core.test

import org.scalatest.FunSuite
import se.lu.nateko.cpauth.core.CookieToToken.{recoverToken, constructCookieContent}
import se.lu.nateko.cpauth.core.SignedToken
import se.lu.nateko.cpauth.core.AuthToken
import se.lu.nateko.cpauth.core.UserInfo
import spray.http.HttpCookie

class CookieToTokenTest extends FunSuite{

	test("Round trip works"){

		val token = SignedToken(
			AuthToken(
				UserInfo("Вася", "Påpkin", "vasja.pupkin@mail.org"),
				1503889594
			),
			"signature_blabla"
		)

		val cookieContent = constructCookieContent(token)

		val cookie = HttpCookie("cpauthToken", cookieContent)

		val roundTripToken = recoverToken(cookie)
		assert(roundTripToken.isSuccess)
		assert(token === roundTripToken.get)
	}
}