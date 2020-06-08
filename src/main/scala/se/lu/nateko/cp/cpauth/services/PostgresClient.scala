package se.lu.nateko.cp.cpauth.services

import se.lu.nateko.cp.cpauth.PostgresConfig
import se.lu.nateko.cp.cpauth.Envri.Envri

import java.sql.Connection

case class DobjDownload(
	hash: String,
	ip: String,
	ts: String,
	countryCode: String
	//TODO More fields
)

class PostgresClient(conf: PostgresConfig) {

	def getConnection(implicit envri: Envri): Connection = ???

	def logDownload(entry: DobjDownload)(implicit envri: Envri): Unit = {

	}
}
