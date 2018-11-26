package se.lu.nateko.cp.viewscore

import java.net.URI

trait IcosStyleConfig{
	def headerImage: URI
	def headerLogo: URI
	def headerHomeLink: URI
}

object CpStyleConfig extends IcosStyleConfig{
	private val prefix = "https://www.icos-cp.eu/themes/cp_theme_d8/"

	val headerImage = new URI("https://static.icos-cp.eu/images/cp-header-background.jpg")
	val headerLogo = new URI(prefix + "logo.svg")
	val headerHomeLink = new URI("https://www.icos-cp.eu/")
}
