package se.lu.nateko.cpauth

import spray.http.HttpCookie

class CookieSetter {

	val cookie = HttpCookie(
		name = "testcookie",
		content = "success",
//		secure = true,
		domain = Some(".icos-cp.eu"),
		path = Some("/"),
//		maxAge = Some(10.minutes.toSeconds)
		httpOnly = true
	)
}