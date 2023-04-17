package se.lu.nateko.cp.viewscore

import java.net.URI

trait IcosStyleConfig{
	def headerImage: URI
	def headerLogo: URI
	def headerHomeLink: URI
}

object CpStyleConfig extends IcosStyleConfig{
	val headerImage = new URI("https://static.icos-cp.eu/images/icos-header.jpg")
	val headerLogo = new URI("https://static.icos-cp.eu/images/ICOS-logo.svg")
	val headerHomeLink = new URI(CpMenu.cpHome + "/")
}
