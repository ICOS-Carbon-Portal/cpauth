package se.lu.nateko.cp.cpauth.services

import se.lu.nateko.cp.cpauth.PostgresConfig
import se.lu.nateko.cp.cpauth.Envri.Envri

import java.sql.Connection

case class DobjDownload(
	ts: String,
	pid: String,
	ip: String,
	city: String,
	countryCode: String,
	latitude: Float,
	longitude: Float
)

class PostgresClient(conf: PostgresConfig) {

	def getConnection(implicit envri: Envri): Connection = ???

	def logDownload(entry: DobjDownload)(implicit envri: Envri): Unit = {

	}
}
