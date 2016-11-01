package views

import java.net.URI

object CpMenu {

	def default = Seq(
		item("Home", "/node/1"),
		group("Services", "/node/2")(
			item("Search, find, download", "/node/45"),
			item("Station Labelling", "https://meta.icos-cp.eu/labeling/"),
			item("Data upload", "/node/44"),
			item("ICOS Stations map", "/node/82"),
			item("ICOS Stations table", "/node/83"),
			group("Visualisations", "/node/9")(
				group("Spatial data", "/node/50")(
					item("Carbon Portal Service", "/node/52"),
					item("THREDDS service", "/node/51")
				),
				item("Time series data", "/node/49"),
				item("Flux estimates", "/node/48"),
				item("Footprint tool", "https://data.icos-cp.eu/stilt/")
			),
			group("Background info", "/node/10")(
				item("ICOS Data Flow", "/node/84"),
				item("Prior flux maps", "/node/54"),
				item("Inverse modelling", "/node/53")
			)
		),
		item("News & Events", "/node/3"),
		item("Documents", "/node/4"),
		group("About", "/node/6")(
			item("Carbon Portal", "/node/93"),
			item("Feedback form", "/node/46"),
			item("People", "/node/19"),
			item("Structure", "/node/20"),
			item("FAQ", "/node/18"),
			item("Visit Carbon Portal", "/node/40")
		)
	)

	def item(label: String, url: String) = CpMenuItem(label, new URI(url))

	def group(label: String, url: String)(subItems: CpMenuItem*) =
		CpMenuItem(label, new URI(url), subItems)
}

