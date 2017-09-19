package se.lu.nateko.cp.cpauth.core

case class PublicAuthConfig(
	authCookieName: String,
	authCookieDomain: String,
	cpauthHost: String,
	publicKeyPath: String
)
