package se.lu.nateko.cp.cpauth.oauth

case class UserInfo(givenName: String, surname: String, email: String)

/**
 * @param vopersonId the ENVRI-ID persistent user identifier (`voperson_id`, or `sub` which
 *        carries the same value). This, not the email address, is the stable identity --
 *        see ENVRI-ID Architecture and RI Integration Guide, section 8.
 */
case class EnvriIdUserInfo(vopersonId: String, info: UserInfo)

case class OrcidUserInfo(orcidId: String, email: Option[String], givenName: Option[String], surname: Option[String]):
	def toPlainUserInfo: Option[UserInfo] = email.map{mail =>
		UserInfo(givenName.getOrElse(""), surname.getOrElse(""), mail)
	}
