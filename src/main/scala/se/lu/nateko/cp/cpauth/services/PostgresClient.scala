package se.lu.nateko.cp.cpauth.services

import se.lu.nateko.cp.cpauth.PostgresConfig
import se.lu.nateko.cp.cpauth.Envri.Envri

import java.sql.Connection
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.ArrayBlockingQueue
import scala.concurrent.ExecutionContext
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource
import org.postgresql.ds.PGConnectionPoolDataSource
import se.lu.nateko.cp.cpauth.CredentialsConfig
import scala.concurrent.Future
import java.sql.ResultSet
import akka.Done
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.Instant
import java.sql.Types

case class DobjDownload(
	objType: String,	// document, data or collection
	ts: String,
	hashId: String,
	ip: String,
	city: Option[String],
	countryCode: Option[String],
	longitude: Option[Float],
	latitude: Option[Float]
)

class PostgresClient(conf: PostgresConfig) extends AutoCloseable {

	def logDownload(entry: DobjDownload)(implicit envri: Envri): Future[Done] = {
		execute(conf.writer)(conn => {
			val  query = """
    			|INSERT INTO downloads(obj_type, ts, hash_id, ip, city, country_code, pos)
				|VALUES (?, ?, ?, ?, ?, ?, ?)""".stripMargin

			val st = conn.prepareStatement(query)
			
			try {
				val Seq(obj_type, ts, hash_id, ip, city, country_code, pos) = 1 to 7

				st.setString(obj_type, entry.objType)
				st.setTimestamp(ts, Timestamp.from(Instant.parse(entry.ts)))
				st.setString(hash_id, entry.hashId)
				st.setString(ip, entry.ip)
				entry.city match {
					case Some(c) => st.setString(city, c)
					case None => st.setNull(city, Types.VARCHAR)
				}
				entry.countryCode match {
					case Some(cc) => st.setString(country_code, cc)
					case None => st.setNull(country_code, Types.VARCHAR)
				}
				if (entry.longitude.isDefined && entry.latitude.isDefined){
					st.setObject(pos, s"ST_SetSRID(POINT(${entry.longitude}, ${entry.latitude}), 4326)", Types.OTHER)
				} else {
					st.setNull(pos, Types.OTHER)
				}

				st.executeUpdate()
			} finally {
				st.close()
			}
		})
	}

	private[this] val executor = {
		val maxThreads = conf.dbAccessPoolSize * conf.dbNames.size
		new ThreadPoolExecutor(
			1, maxThreads, 30, TimeUnit.SECONDS, new ArrayBlockingQueue[Runnable](maxThreads)
		)
	}

	private[this] implicit val exeCtxt = ExecutionContext.fromExecutor(executor)

	private[this] val dataSources: Map[Envri, SharedPoolDataSource] = conf.dbNames.view.mapValues{ dbName =>
		val pgDs = new PGConnectionPoolDataSource()
		pgDs.setServerNames(Array(conf.hostname))
		pgDs.setDatabaseName(dbName)
		pgDs.setPortNumbers(Array(conf.port))
		val ds = new SharedPoolDataSource()
		ds.setMaxTotal(conf.dbAccessPoolSize)
		ds.setConnectionPoolDataSource(pgDs)
		ds.setDefaultAutoCommit(false)
		ds
	}.toMap

	override def close(): Unit = {
		executor.shutdown()
		dataSources.valuesIterator.foreach{_.close()}
	}

	private def getConnection(creds: CredentialsConfig)(implicit envri: Envri): Future[Connection] = Future{
		dataSources(envri).getConnection(creds.username, creds.password)
	}

	private def withConnection[T](creds: CredentialsConfig)(act: Connection => T)(implicit envri: Envri): Future[T] =
		getConnection(creds).map{conn =>
			conn.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT)
			try {
				act(conn)
			} finally{
				conn.close()
			}
		}

	private def execute(credentials: CredentialsConfig)(action: Connection => Unit)(implicit envri: Envri): Future[Done] = {
		withConnection(credentials){conn =>
			try {
				action(conn)
				conn.commit()
				Done
			} catch {
				case ex: Throwable =>
					conn.rollback()
					throw ex
			}
		}
	}

	private def withTransaction(creds: CredentialsConfig)(query: String)(act: PreparedStatement => Unit)(implicit envri: Envri): Unit = {
		withConnection(creds){conn =>
			val st = conn.prepareStatement(query)
			try{
				act(st)
				conn.commit()
			}finally{
				st.close()
			}
		}
	}
}
