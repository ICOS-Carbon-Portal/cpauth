package se.lu.nateko.cp.viewscore

case class CpMenuItem(title: String, url: String, children: Seq[CpMenuItem])

object CpMenu:

	val cpHome = "https://www.icos-cp.eu"
	val cpMenuApi = "https://www.icos-cp.eu/api/menu/main"
	val citiesHome = "https://www.icos-cities.eu"
	val citiesMenuApi = "https://www.icos-cp.eu/api/menu/cities"
	val sitesHome = "https://www.fieldsites.se"
	val sitesMenuApi = "https://www.fieldsites.se/api/menu/main"

	val fallback = Seq(
		CpMenuItem("Home", cpHome, Nil), 
		CpMenuItem("Data & Services", "/data-services", Seq(
			CpMenuItem("Data Portal", "/data-services/data-portal", Seq(
				CpMenuItem("Access ICOS data", "https://data.icos-cp.eu/", Nil),
				CpMenuItem("Main data products", "/data-products", Nil),
				CpMenuItem("Elaborated Products", "/data-services/data-portal/elaborated-products", Nil),
				CpMenuItem("How to use the ICOS Data Portal", "/data-services/data-portal/how-to-use", Nil),
				CpMenuItem("ICOS Data Licence", "/data-services/data-portal/data-licence", Nil),
				CpMenuItem("User account", "https://cpauth.icos-cp.eu/", Nil),
				CpMenuItem("Help pages", "/data-services/data-portal/help", Nil)
			)),
			CpMenuItem("ICOS Services", "/data-services/services", Seq(
				CpMenuItem("STILT Footprint", "/data-services/services/stilt-footprint", Nil),
				CpMenuItem("STILT results visualisation", "https://stilt.icos-cp.eu/", Nil),
				CpMenuItem("STILT on demand calculator", "https://stilt.icos-cp.eu/worker/", Nil),
				CpMenuItem("Jupyter Notebook", "/data-services/services/jupyter-notebook", Nil),
				CpMenuItem("DOI minting", "https://doi.icos-cp.eu/", Nil),
				CpMenuItem("Download statistics", "https://data.icos-cp.eu/stats", Nil),
				CpMenuItem("SPARQL endpoint", "https://meta.icos-cp.eu/sparqlclient/", Nil),
				CpMenuItem("Upload to Data Portal", "/data-services/services/upload-data", Nil),
				CpMenuItem("Forecast of backtrajectories", "/forecast", Nil),
				CpMenuItem("Python package", "https://pypi.org/project/icoscp/", Nil),
				CpMenuItem("Service status", "https://uptime.icos-cp.eu/status/core", Nil)
			)),
			CpMenuItem("How to Cite and Acknowledge ICOS data", "/how-to-cite", Nil)
		)),
		CpMenuItem("Measurements", "/measurements", Seq(
			CpMenuItem("Station network", "/measurements/station-network", Nil),
			CpMenuItem("Atmosphere measurements", "/measurements/atmosphere", Nil),
			CpMenuItem("Ecosystem measurements", "/measurements/ecosystem", Nil),
			CpMenuItem("Ocean measurements", "/measurements/ocean", Nil),
			CpMenuItem("Central Analytical Laboratories", "/measurements/central-analytic-laboratories", Nil),
			CpMenuItem("Data collection process", "/measurements/data-collection", Seq(
				CpMenuItem("Data flow", "/measurements/data-collection/data-flow", Nil),
				CpMenuItem("Data levels and quality", "/measurements/data-collection/data-levels-quality", Nil),
				CpMenuItem("Station labelling status", "https://meta.icos-cp.eu/labeling/", Nil),
				CpMenuItem("Raw data submission heatmap", "/measurements/network-data-submission-overview", Nil)
			)),
			CpMenuItem("ICOS Countries", "/measurements/icos-countries", Seq(
				CpMenuItem("Belgium", "/measurements/icos-countries/belgium", Nil),
				CpMenuItem("Czech Republic", "/measurements/icos-countries/czech-republic", Nil),
				CpMenuItem("Denmark", "/measurements/icos-countries/denmark", Nil),
				CpMenuItem("Finland", "/measurements/icos-countries/finland", Nil),
				CpMenuItem("France", "/measurements/icos-countries/france", Nil),
				CpMenuItem("Germany", "/measurements/icos-countries/germany", Nil),
				CpMenuItem("Greece", "/measurements/icos-countries/greece", Nil),
				CpMenuItem("Hungary", "/measurements/icos-countries/hungary", Nil),
				CpMenuItem("Ireland", "/measurements/icos-countries/ireland", Nil),
				CpMenuItem("Italy", "/measurements/icos-countries/italy", Nil),
				CpMenuItem("Netherlands", "/measurements/icos-countries/netherlands", Nil),
				CpMenuItem("Norway", "/measurements/icos-countries/norway", Nil),
				CpMenuItem("Spain", "/measurements/icos-countries/spain", Nil),
				CpMenuItem("Sweden", "/measurements/icos-countries/sweden", Nil),
				CpMenuItem("Switzerland", "/measurements/icos-countries/switzerland", Nil),
				CpMenuItem("United Kingdom", "/measurements/icos-countries/united-kingdom", Nil)
			))
		)),
		CpMenuItem("Impact", "/impact", Seq(
			CpMenuItem("Climate change", "/impact/climate-change", Seq(
				CpMenuItem("Greenhouse gases", "/impact/climate-change/ghgs", Nil),
				CpMenuItem("Emission reduction", "/impact/climate-change/emission-reduction", Nil)
			)),
			CpMenuItem("ICOS contribution to science", "/impact/science", Seq(
				CpMenuItem("Science done with ICOS data", "/impact/science/science-done", Nil),
				CpMenuItem("Scientific impact", "/impact/science/scientific", Nil),
				CpMenuItem("Global Carbon Budget", "/impact/science/global-carbon-budget", Nil),
				CpMenuItem("ICOS and remote sensing", "/science-and-impact/science-contribution/remote-sensing", Nil)
			)),
			CpMenuItem("ICOS Curve", "/impact/icos-curve", Seq(
				CpMenuItem("Questions and Answers about the ICOS Curve", "/impact/icos-curve/q-and-a", Nil)
			)),
			CpMenuItem("ICOS impact to society", "/impact/society", Seq(
				CpMenuItem("FLUXES, The European Greenhouse Gas Bulletin", "/fluxes", Nil),
				CpMenuItem("Socioeconomic impact", "/impact/society/socioeconomic", Nil),
				CpMenuItem("Technology & innovation", "/impact/society/technology-innovation", Nil),
				CpMenuItem("ICOS in scientific publications", "/impact/society/references", Nil)
			)),
			CpMenuItem("Projects", "/impact/projects", Seq(
				CpMenuItem("ICOS Cities", "/projects/icos-cities", Nil),
				CpMenuItem("NUBICOS", "/projects/nubicos", Nil)
			))
		)),
		CpMenuItem("Materials", "/materials", Seq(
			CpMenuItem("Reports and documents", "/materials/reports-and-documents", Nil),
			CpMenuItem("Education related to ICOS", "/materials/education", Nil),
			CpMenuItem("Photo galleries", "/materials/photo-gallery", Nil),
			CpMenuItem("Videos", "/materials/videos", Nil),
			CpMenuItem("Logos and templates", "/materials/logos-templates", Nil)
		)),
		CpMenuItem("News & Events", "/news-and-events", Seq(
			CpMenuItem("News", "/news-and-events/news", Nil),
			CpMenuItem("Events", "/news-and-events/events", Seq(
				CpMenuItem("ICOS Talks", "/news-and-events/talks", Nil)
			)),
			CpMenuItem("Newsletters", "/news-and-events/newsletters", Nil),
			CpMenuItem("ICOS Science Conference 2026", "/news-and-events/science-conference/icos2026sc", Seq(
				CpMenuItem("Past Science Conferences", "/news-and-events/science-conference", Nil)
			))
		)),
		CpMenuItem("About & Contacts", "/about", Seq(
			CpMenuItem("ICOS in a nutshell", "/about/icos-in-nutshell", Seq(
				CpMenuItem("Mission", "/about/icos-in-nutshell/mission", Nil),
				CpMenuItem("Strategy", "/about/icos-in-nutshell/strategy", Nil),
				CpMenuItem("ICOS abbreviations", "/about/icos-in-nutshell/abbreviations", Nil)
			)),
			CpMenuItem("Organisation and governance", "/about/organisation-governance", Seq(
				CpMenuItem("Structure of ICOS", "/about/organisation-governance/structure", Nil),
				CpMenuItem("ICOS ERIC", "/about/organisation-governance/icos-eric", Nil),
				CpMenuItem("ICOS member countries", "/about/organisation-governance/member-countries", Nil),
				CpMenuItem("International cooperation", "/about/organisation-governance/international-cooperation", Nil),
				CpMenuItem("Terms of Use", "/terms-of-use", Nil)
			)),
			CpMenuItem("Join ICOS network", "/about/join-icos", Seq(
				CpMenuItem("Benefits of being in ICOS", "/about/join-icos/benefits", Nil),
				CpMenuItem("How countries join ICOS", "/about/join-icos/process-countries", Nil),
				CpMenuItem("How stations join ICOS", "/about/join-icos/process-stations", Nil)
			)),
			CpMenuItem("Contact us", "/about/contact", Seq(
				CpMenuItem("Head Office & Central Facilities", "/about/contact/head-office-central-facilities", Nil),
				CpMenuItem("National Networks", "/about/contact/national-networks", Nil),
				CpMenuItem("For media", "/about/contact/media", Nil),
				CpMenuItem("FAQ", "/about/contact/faq", Nil),
				CpMenuItem("Give us feedback", "/about/contact/feedback", Nil),
				CpMenuItem("User survey", "/about/contact/services-feedback", Nil),
			)),
			CpMenuItem("Opportunities", "/about/opportunities", Seq(
				CpMenuItem("Careers", "/about/opportunities/careers", Nil),
				CpMenuItem("ICOS Summer School", "/about/opportunities/summer-school", Nil),
				CpMenuItem("Ingeborg Levin Early Career Scientist Award", "/about/opportunities/ingeborg-award", Nil)
			))
		))
	)

	def default = MenuProvider.cpMenu.getOrElse(fallback)
	def cities = MenuProvider.citiesMenu.getOrElse(fallback)
	def sites = MenuProvider.sitesMenu.getOrElse(Seq())
