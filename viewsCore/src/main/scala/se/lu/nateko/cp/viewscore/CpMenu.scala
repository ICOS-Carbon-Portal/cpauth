package se.lu.nateko.cp.viewscore

import java.net.URI

sealed trait CpMenuItem
case class CpMenuGroup(label: String, children: Seq[CpMenuItem]) extends CpMenuItem
case class CpMenuLeaf(label: String, ref: URI) extends CpMenuItem

object CpMenu {

	val cpHome = "https://www.icos-cp.eu"
	val riHome = "https://www.icos-ri.eu"

	val landingPage = Seq(
		group("Home")(
			item("Carbon Portal", cpHome),
			item("ICOS", riHome)
		),
		item("License", "https://data.icos-cp.eu/licence")
	)

	val fallback = Seq("Home", "Services", "News & Events", "Documents", "About").map(item(_, cpHome))

	def default = MenuProvider.menu.getOrElse(fallback)

	def item(label: String, url: String): CpMenuItem = CpMenuLeaf(label, new URI(url))

	def group(label: String)(first: CpMenuItem, rest: CpMenuItem*): CpMenuItem =
		CpMenuGroup(label, first +: rest)
}

