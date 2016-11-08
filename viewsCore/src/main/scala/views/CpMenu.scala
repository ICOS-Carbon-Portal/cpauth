package views

import java.net.URI
import se.lu.nateko.cp.viewscore.MenuProvider

object CpMenu {

	val cpHome = "https://www.icos-cp.eu"
	val riHome = "https://www.icos-ri.eu"

	val landingPage = Seq(
		group("Home", cpHome)(
			item("Carbon Portal", cpHome),
			item("ICOS", riHome)
		),
		item("License", cpHome + "/license")
	)

	val fallback = Seq("Home", "Services", "News & Events", "Documents", "About").map(item(_, cpHome))

	def default = MenuProvider.menu.getOrElse(fallback)

	def item(label: String, url: String) = CpMenuItem(label, new URI(url))

	def group(label: String, url: String)(subItems: CpMenuItem*) =
		CpMenuItem(label, new URI(url), subItems)
}

