package eu.icoscp.envri

enum Envri derives CanEqual:
	case ICOS, SITES, ICOSCities

case class EnvriConfig(
	shortName: String,
	longName: String
)

val Config = Map(
	Envri.ICOS -> EnvriConfig(
		shortName = "ICOS",
		longName = "Integrated Carbon Observation System"
	),
	Envri.SITES -> EnvriConfig(
		shortName = "SITES",
		longName = "Swedish Infrastructure for Ecosystem Science"
	),
	Envri.ICOSCities -> EnvriConfig(
		shortName = "ICOS Cities",
		longName = "Pilot Applications in Urban Landscapes"
	)
)
