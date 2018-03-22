package se.lu.nateko.cp.cpauth.views

import se.lu.nateko.cp.cpauth.Envri
import se.lu.nateko.cp.cpauth.Envri.Envri

object ViewStrings {
	def loginStrings(implicit envri: Envri): LoginStrings = envri match {
		case Envri.ICOS => IcosLoginStrings
		case Envri.SITES => SitesLoginStrings
	}

	def homeStrings(implicit envri: Envri): HomeStrings = envri match {
		case Envri.ICOS => IcosHomeStrings
		case Envri.SITES => SitesHomeStrings
	}
}
