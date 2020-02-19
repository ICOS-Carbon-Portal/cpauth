package se.lu.nateko.cp.viewscore

case class CpMenuItem(title: String, url: String, children: Seq[CpMenuItem])

object CpMenu {

	val cpHome = "https://www.icos-cp.eu"
	val riHome = "https://www.icos-ri.eu"
	val cpMenuApi = "https://www.icos-cp.eu/api/menu/main"

	val fallback = Seq(CpMenuItem("Home", cpHome, Nil))

	def default = MenuProvider.menu.getOrElse(fallback)

}

