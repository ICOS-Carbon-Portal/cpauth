package eu.icoscp.envri

enum Envri(
	val shortName: String,
	val longName: String
) derives CanEqual:
	case ICOS extends Envri("ICOS", "Integrated Carbon Observation System")
	case SITES extends Envri("SITES", "Swedish Infrastructure for Ecosystem Science")
	case ICOSCities extends Envri("ICOS Cities", "Pilot Applications in Urban Landscapes")
