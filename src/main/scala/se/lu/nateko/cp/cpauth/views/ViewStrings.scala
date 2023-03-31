package se.lu.nateko.cp.cpauth.views

import eu.icoscp.envri.Envri

object ViewStrings:
	def loginStrings(implicit envri: Envri): LoginStrings = envri match
		case Envri.ICOS       => IcosLoginStrings
		case Envri.ICOSCities => IcosLoginStrings
		case Envri.SITES      => SitesLoginStrings

	def homeStrings(implicit envri: Envri): HomeStrings = envri match
		case Envri.ICOS       => IcosHomeStrings
		case Envri.ICOSCities => IcosHomeStrings
		case Envri.SITES      => SitesHomeStrings
