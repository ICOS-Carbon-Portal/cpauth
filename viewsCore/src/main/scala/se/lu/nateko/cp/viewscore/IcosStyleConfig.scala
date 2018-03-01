package se.lu.nateko.cp.viewscore

import java.net.URI

trait IcosStyleConfig{
	def headerImage: URI
	def headerImageMedium: URI
	def headerImageSmall: URI
	def headerLogo: URI
	def headerHomeLink: URI
}

object CpStyleConfig extends IcosStyleConfig{
	private val prefix = "https://www.icos-cp.eu/themes/cp_theme_d8/"

	val headerImage = new URI(prefix + "images/icos-cp_header.gif")
	val headerImageMedium = new URI(prefix + "images/icos-cp_header_medium.gif")
	val headerImageSmall = new URI(prefix + "images/icos-cp_header_small.gif")
	val headerLogo = new URI(prefix + "logo.svg")
	val headerHomeLink = new URI("https://www.icos-cp.eu/")
}
