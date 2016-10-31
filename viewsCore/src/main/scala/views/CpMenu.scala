package views

import java.net.URI

object CpMenu {

	def default = Seq(
		item("Home", "/node/1"),
		group("Services", "/node/2")(
			item("Search, find, download", "/node/45"),
			item("Station Labelling", "https://meta.icos-cp.eu/labeling/"),
			group("Visualisations", "/node/9")(
				group("Spatial data", "/node/50")(
					item("Carbon Portal Service", "/node/52"),
					item("THREDDS service", "/node/51")
				),
				item("Time series data", "/node/49")
			)
		)
	)

	def item(label: String, url: String) = CpMenuItem(label, new URI(url))

	def group(label: String, url: String)(subItems: CpMenuItem*) =
		CpMenuItem(label, new URI(url), subItems)
}