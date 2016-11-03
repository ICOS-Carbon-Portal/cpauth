package views

import java.net.URI
import se.lu.nateko.cp.viewscore.MenuProvider

object CpMenu {

	val cpHome = "https://www.icos-cp.eu"

	val fallback = Seq(
		item("Home", "/node/1"),
		group("Services", "/node/2")(
			item("ICOS email lists", "https://lists.icos-cp.eu/lists/"),
			item("ICOS comunity fora", "https://fora.icos-cp.eu/bb/"),
			item("ICOS References", "/node/101"),
			item("Search, find, download", "/node/45"),
			item("Data upload", "/node/44"),
			item("Station Labelling", "https://meta.icos-cp.eu/labeling/"),
			item("ICOS Stations map", "/node/82"),
			item("ICOS Stations table", "/node/83"),
			group("Visualisations", "/node/9")(
				item("Footprint tool", "https://data.icos-cp.eu/stilt/"),
				group("Spatial data", "/node/50")(
					item("Carbon Portal Service", "https://data.icos-cp.eu/netcdf/"),
					item("THREDDS service", "/node/51")
				),
				item("Time series data", "/node/49"),
				item("Flux estimates", "/node/48")
			),
			group("Background info", "/node/10")(
				item("ICOS Data Flow", "/node/84"),
				item("Prior flux maps", "/node/54"),
				item("Inverse modelling", "/node/53")
			)
		),
		group("News & Events", "/node/3")(
			item("Archive of events and news", "/node/120")
		),
		group("Documents", "/node/4")(
			item("Carbon Portal Newsletters", "/node/113"),
			item("ICOS Science Conference 2016 Presentations", "/node/112"),
			item("All Carbon Portal documents", "/node/4")
		),
		group("About", "/node/6")(
			item("Carbon Portal", "/node/93"),
			item("Feedback form", "/node/46"),
			item("People", "/node/19"),
			item("Structure", "/node/20"),
			item("FAQ", "/node/18"),
			item("Visit Carbon Portal", "/node/40"),
			item("My CP", "https://cpauth.icos-cp.eu/home/")
		)
	)

	def default = MenuProvider.menu.getOrElse(fallback)

	def item(label: String, url: String) = CpMenuItem(label, new URI(url))

	def group(label: String, url: String)(subItems: CpMenuItem*) =
		CpMenuItem(label, new URI(url), subItems)
}

