package se.lu.nateko.cp.viewscore

case class CpMenuItem(title: String, url: String, children: Seq[CpMenuItem])

object CpMenu:

	val cpHome = "https://www.icos-cp.eu"
	val cpMenuApi = "" // temporarily disabled
	val citiesHome = "https://www.icos-cities.eu"
	val citiesMenuApi = "https://www.icos-cp.eu/api/menu/cities"
	val sitesHome = "https://www.fieldsites.se"
	val sitesMenuApi = "https://www.fieldsites.se/api/menu/main"

	val fallback = Seq(
		CpMenuItem("Home", cpHome, Nil), 
		CpMenuItem("Data & Services", cpHome + "/data-services", Seq(
			CpMenuItem("Data Portal", cpHome + "/data-services/data-portal", Seq(
				CpMenuItem("Access ICOS data", "https://data.icos-cp.eu/", Nil),
				CpMenuItem("Main data products", cpHome + "/data-products", Nil),
				CpMenuItem("Elaborated Products", cpHome + "/data-services/data-portal/elaborated-products", Nil),
				CpMenuItem("How to use the ICOS Data Portal", cpHome + "/data-services/data-portal/how-to-use", Nil),
				CpMenuItem("ICOS Data Licence", cpHome + "/data-services/data-portal/data-licence", Nil),
				CpMenuItem("User account", "https://cpauth.icos-cp.eu/", Nil),
				CpMenuItem("Help pages", cpHome + "/data-services/data-portal/help", Nil)
			)),
			CpMenuItem("ICOS Services", cpHome + "/data-services/services", Seq(
				CpMenuItem("STILT Footprint", cpHome + "/data-services/services/stilt-footprint", Nil),
				CpMenuItem("STILT results visualisation", "https://stilt.icos-cp.eu/", Nil),
				CpMenuItem("STILT on demand calculator", "https://stilt.icos-cp.eu/worker/", Nil),
				CpMenuItem("Jupyter Notebook", cpHome + "/data-services/services/jupyter-notebook", Nil),
				CpMenuItem("DOI minting", "https://doi.icos-cp.eu/", Nil),
				CpMenuItem("Download statistics", "https://data.icos-cp.eu/stats", Nil),
				CpMenuItem("SPARQL endpoint", "https://meta.icos-cp.eu/sparqlclient/", Nil),
				CpMenuItem("Upload to Data Portal", cpHome + "/data-services/services/upload-data", Nil),
				CpMenuItem("Forecast of backtrajectories", cpHome + "/forecast", Nil),
				CpMenuItem("Python package", "https://pypi.org/project/icoscp/", Nil),
				CpMenuItem("Service status", "https://uptime.icos-cp.eu/status/core", Nil)
			)),
			CpMenuItem("How to Cite and Acknowledge ICOS data", cpHome + "/how-to-cite", Nil)
		)),
		CpMenuItem("Measurements", cpHome + "/measurements", Seq(
			CpMenuItem("Station network", cpHome + "/measurements/station-network", Nil),
			CpMenuItem("Atmosphere measurements", cpHome + "/measurements/atmosphere", Nil),
			CpMenuItem("Ecosystem measurements", cpHome + "/measurements/ecosystem", Nil),
			CpMenuItem("Ocean measurements", cpHome + "/measurements/ocean", Nil),
			CpMenuItem("Central Analytical Laboratories", cpHome + "/measurements/central-analytic-laboratories", Nil),
			CpMenuItem("Data collection process", cpHome + "/measurements/data-collection", Seq(
				CpMenuItem("Data flow", cpHome + "/measurements/data-collection/data-flow", Nil),
				CpMenuItem("Data levels and quality", cpHome + "/measurements/data-collection/data-levels-quality", Nil),
				CpMenuItem("Station labelling status", "https://meta.icos-cp.eu/labeling/", Nil),
				CpMenuItem("Raw data submission heatmap", cpHome + "/measurements/network-data-submission-overview", Nil)
			)),
			CpMenuItem("ICOS Countries", cpHome + "/measurements/icos-countries", Seq(
				CpMenuItem("Belgium", cpHome + "/measurements/icos-countries/belgium", Nil),
				CpMenuItem("Czech Republic", cpHome + "/measurements/icos-countries/czech-republic", Nil),
				CpMenuItem("Denmark", cpHome + "/measurements/icos-countries/denmark", Nil),
				CpMenuItem("Finland", cpHome + "/measurements/icos-countries/finland", Nil),
				CpMenuItem("France", cpHome + "/measurements/icos-countries/france", Nil),
				CpMenuItem("Germany", cpHome + "/measurements/icos-countries/germany", Nil),
				CpMenuItem("Greece", cpHome + "/measurements/icos-countries/greece", Nil),
				CpMenuItem("Hungary", cpHome + "/measurements/icos-countries/hungary", Nil),
				CpMenuItem("Ireland", cpHome + "/measurements/icos-countries/ireland", Nil),
				CpMenuItem("Italy", cpHome + "/measurements/icos-countries/italy", Nil),
				CpMenuItem("Netherlands", cpHome + "/measurements/icos-countries/netherlands", Nil),
				CpMenuItem("Norway", cpHome + "/measurements/icos-countries/norway", Nil),
				CpMenuItem("Spain", cpHome + "/measurements/icos-countries/spain", Nil),
				CpMenuItem("Sweden", cpHome + "/measurements/icos-countries/sweden", Nil),
				CpMenuItem("Switzerland", cpHome + "/measurements/icos-countries/switzerland", Nil),
				CpMenuItem("United Kingdom", cpHome + "/measurements/icos-countries/united-kingdom", Nil)
			))
		)),
		CpMenuItem("Impact", cpHome + "/impact", Seq(
			CpMenuItem("Climate change", cpHome + "/impact/climate-change", Seq(
				CpMenuItem("Greenhouse gases", cpHome + "/impact/climate-change/ghgs", Nil),
				CpMenuItem("Emission reduction", cpHome + "/impact/climate-change/emission-reduction", Nil)
			)),
			CpMenuItem("ICOS contribution to science", cpHome + "/impact/science", Seq(
				CpMenuItem("Science done with ICOS data", cpHome + "/impact/science/science-done", Nil),
				CpMenuItem("Scientific impact", cpHome + "/impact/science/scientific", Nil),
				CpMenuItem("Global Carbon Budget", cpHome + "/impact/science/global-carbon-budget", Nil),
				CpMenuItem("ICOS and remote sensing", cpHome + "/science-and-impact/science-contribution/remote-sensing", Nil)
			)),
			CpMenuItem("ICOS Curve", cpHome + "/impact/icos-curve", Seq(
				CpMenuItem("Questions and Answers about the ICOS Curve", cpHome + "/impact/icos-curve/q-and-a", Nil)
			)),
			CpMenuItem("ICOS impact to society", cpHome + "/impact/society", Seq(
				CpMenuItem("FLUXES, The European Greenhouse Gas Bulletin", cpHome + "/fluxes", Nil),
				CpMenuItem("Socioeconomic impact", cpHome + "/impact/society/socioeconomic", Nil),
				CpMenuItem("Technology & innovation", cpHome + "/impact/society/technology-innovation", Nil),
				CpMenuItem("ICOS in scientific publications", cpHome + "/impact/society/references", Nil)
			)),
			CpMenuItem("Projects", cpHome + "/impact/projects", Seq(
				CpMenuItem("ICOS Cities", cpHome + "/projects/icos-cities", Nil),
				CpMenuItem("NUBICOS", cpHome + "/projects/nubicos", Nil)
			))
		)),
		CpMenuItem("Materials", cpHome + "/materials", Seq(
			CpMenuItem("Reports and documents", cpHome + "/materials/reports-and-documents", Nil),
			CpMenuItem("Education related to ICOS", cpHome + "/materials/education", Nil),
			CpMenuItem("Photo galleries", cpHome + "/materials/photo-gallery", Nil),
			CpMenuItem("Videos", cpHome + "/materials/videos", Nil),
			CpMenuItem("Logos and templates", cpHome + "/materials/logos-templates", Nil)
		)),
		CpMenuItem("News & Events", cpHome + "/news-and-events", Seq(
			CpMenuItem("News", cpHome + "/news-and-events/news", Nil),
			CpMenuItem("Events", cpHome + "/news-and-events/events", Seq(
				CpMenuItem("ICOS Talks", cpHome + "/news-and-events/talks", Nil)
			)),
			CpMenuItem("Newsletters", cpHome + "/news-and-events/newsletters", Nil),
			CpMenuItem("ICOS Science Conference 2026", cpHome + "/news-and-events/science-conference/icos2026sc", Seq(
				CpMenuItem("Past Science Conferences", cpHome + "/news-and-events/science-conference", Nil)
			))
		)),
		CpMenuItem("About & Contacts", cpHome + "/about", Seq(
			CpMenuItem("ICOS in a nutshell", cpHome + "/about/icos-in-nutshell", Seq(
				CpMenuItem("Mission", cpHome + "/about/icos-in-nutshell/mission", Nil),
				CpMenuItem("Strategy", cpHome + "/about/icos-in-nutshell/strategy", Nil),
				CpMenuItem("ICOS abbreviations", cpHome + "/about/icos-in-nutshell/abbreviations", Nil)
			)),
			CpMenuItem("Organisation and governance", cpHome + "/about/organisation-governance", Seq(
				CpMenuItem("Structure of ICOS", cpHome + "/about/organisation-governance/structure", Nil),
				CpMenuItem("ICOS ERIC", cpHome + "/about/organisation-governance/icos-eric", Nil),
				CpMenuItem("ICOS member countries", cpHome + "/about/organisation-governance/member-countries", Nil),
				CpMenuItem("International cooperation", cpHome + "/about/organisation-governance/international-cooperation", Nil),
				CpMenuItem("Terms of Use", cpHome + "/terms-of-use", Nil)
			)),
			CpMenuItem("Join ICOS network", cpHome + "/about/join-icos", Seq(
				CpMenuItem("Benefits of being in ICOS", cpHome + "/about/join-icos/benefits", Nil),
				CpMenuItem("How countries join ICOS", cpHome + "/about/join-icos/process-countries", Nil),
				CpMenuItem("How stations join ICOS", cpHome + "/about/join-icos/process-stations", Nil)
			)),
			CpMenuItem("Contact us", cpHome + "/about/contact", Seq(
				CpMenuItem("Head Office & Central Facilities", cpHome + "/about/contact/head-office-central-facilities", Nil),
				CpMenuItem("National Networks", cpHome + "/about/contact/national-networks", Nil),
				CpMenuItem("For media", cpHome + "/about/contact/media", Nil),
				CpMenuItem("FAQ", cpHome + "/about/contact/faq", Nil),
				CpMenuItem("Give us feedback", cpHome + "/about/contact/feedback", Nil),
				CpMenuItem("User survey", cpHome + "/about/contact/services-feedback", Nil),
			)),
			CpMenuItem("Opportunities", cpHome + "/about/opportunities", Seq(
				CpMenuItem("Careers", cpHome + "/about/opportunities/careers", Nil),
				CpMenuItem("ICOS Summer School", cpHome + "/about/opportunities/summer-school", Nil),
				CpMenuItem("Ingeborg Levin Early Career Scientist Award", cpHome + "/about/opportunities/ingeborg-award", Nil)
			))
		))
	)

	def default = MenuProvider.cpMenu.getOrElse(fallback)
	def cities = MenuProvider.citiesMenu.getOrElse(fallback)
	def sites = MenuProvider.sitesMenu.getOrElse(Seq())
