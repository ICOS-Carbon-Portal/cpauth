package se.lu.nateko.cp.cpauth.oauth

case class UserInfo(givenName: String, surname: String, email: String)

case class OrcidUserInfo(orcidId: String, email: Option[String], givenName: Option[String], surname: Option[String])
