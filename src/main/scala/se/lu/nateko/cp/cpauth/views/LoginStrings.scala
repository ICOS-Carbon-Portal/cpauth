package se.lu.nateko.cp.cpauth.views

trait LoginStrings {
	def title: String
	def subheading: String
	def cookieNotice: String
}

object IcosLoginStrings extends LoginStrings {
	val title = "Login to Carbon Portal"
	val subheading = "Please sign in"
	val cookieNotice = "<p>Carbon Portal uses cookies to handle your login session. By using our services, you agree to our use of cookies.</p>"
}

object SitesLoginStrings extends LoginStrings {
	val title = "Sign in"
	val subheading = ""
	val cookieNotice = "<p>SITES uses cookies to handle your login session. By using our services, you agree to our use of cookies.</p>"
}
