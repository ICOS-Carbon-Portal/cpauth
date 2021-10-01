package se.lu.nateko.cp.cpauth.views

trait HomeStrings {
	def title: String
	def subheading: String
	def licenceUrl: String
}

object IcosHomeStrings extends HomeStrings {
	val title = "Welcome to Carbon Portal"
	val subheading = "Manage your account"
	val licenceUrl = "https://data.icos-cp.eu/licence"
}

object SitesHomeStrings extends HomeStrings {
	val title = "Account"
	val subheading = ""
	val licenceUrl = "https://data.fieldsites.se/licence"
}
