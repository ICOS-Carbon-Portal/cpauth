package se.lu.nateko.cp.viewscore

case class CpMenuItem(title: String, url: String, children: Seq[CpMenuItem])

object CpMenu:

	val cpHome = "https://www.icos-cp.eu"
	val cpMenuApi = "https://www.icos-cp.eu/api/menu/main"
	val citiesHome = "https://www.icos-cities.eu"
	val citiesMenuApi = "https://www.icos-cp.eu/api/menu/cities"
	val sitesHome = "https://www.fieldsites.se"
	val sitesMenuApi = "https://www.fieldsites.se/api/menu/main"

	val fallback = Seq(CpMenuItem("Home", cpHome, Nil))

	def default = MenuProvider.cpMenu.getOrElse(fallback)
	def cities = MenuProvider.citiesMenu.getOrElse(fallback)
	def sites = MenuProvider.sitesMenu.getOrElse(Seq())
