package se.lu.nateko.cp.cpauth.views

trait HomeStrings {
	def title: String
	def subheading: String
	def loginUrl: String
	def licenceUrl: String
}

object IcosHomeStrings extends HomeStrings {
	val title = "Welcome to Carbon Portal"
	val subheading = "Manage your account here"
	val loginUrl = "https://cpauth.icos-cp.eu/login/"
	val licenceUrl = "https://data.icos-cp.eu/licence"
}

object SitesHomeStrings extends HomeStrings {
	val title = "Account"
	val subheading = ""
	val loginUrl = "https://auth.fieldsites.se/login"
	val licenceUrl = "https://data.fieldsites.se/licence"
}
