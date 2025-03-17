package se.lu.nateko.cp.cpauth.core

case class PublicAuthConfig(
	authCookieName: String,
	authCookieDomain: String,
	authHost: String,
	publicKeyPath: String
)

case class EmailConfig(
	smtpServer: String,
	username: String,
	password: String,
	fromAddress: String,
	replyToAddress: String,
	logBccAddress: Option[String]
)
