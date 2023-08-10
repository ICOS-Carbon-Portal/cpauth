package se.lu.nateko.cp.viewscore

import java.net.URI

trait IcosStyleConfig{
	def headerImage: URI
	def headerLogo: URI
	def headerHomeLink: URI
	def headerLogoName: String
	def matomoId: Int
}

object CpStyleConfig extends IcosStyleConfig{
	val headerImage = new URI("https://static.icos-cp.eu/images/icos-header.jpg")
	val headerLogo = new URI("https://static.icos-cp.eu/images/ICOS-logo.svg")
	val headerHomeLink = new URI(CpMenu.cpHome)
	val headerLogoName = "ICOS Carbon Portal"
	val matomoId = 3
}

object CitiesStyleConfig extends IcosStyleConfig{
	val headerImage = new URI("https://static.icos-cp.eu/images/icos-cities-banner.png")
	val headerLogo = new URI("https://static.icos-cp.eu/images/ICOS-Cities-logo-nega.svg")
	val headerHomeLink = new URI(CpMenu.citiesHome)
	val headerLogoName = "ICOS Cities"
	val matomoId = 4
}
