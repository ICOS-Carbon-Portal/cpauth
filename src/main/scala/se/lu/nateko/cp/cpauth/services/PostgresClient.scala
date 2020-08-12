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

object DownloadItemType extends Enumeration{
	val Data = Value("data")
	val Doc = Value("document")
	val Coll = Value("collection")
	type ItemType = Value
}

case class DownloadEvent(
	itemType: DownloadItemType.ItemType,
	ts: Instant,
	hashId: String,
	ip: String,
	city: Option[String],
	countryCode: Option[String],
	longitude: Option[Double],
	latitude: Option[Double]
)

class PostgresClient(conf: PostgresConfig) extends AutoCloseable {

	def logDownload(entry: DownloadEvent)(implicit envri: Envri): Future[Done] = withTransaction(conf.writer){
		entry.latitude.zip(entry.longitude) match{
			case Some((lat, lon)) =>
				"""INSERT INTO downloads(item_type, ts, hash_id, ip, city, country_code, pos)
      			|VALUES (?, ?::timestamptz at time zone 'utc', ?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326))""".stripMargin
			case None =>
				"""INSERT INTO downloads(item_type, ts, hash_id, ip, city, country_code)
      			|VALUES (?, ?::timestamptz at time zone 'utc', ?, ?, ?, ?)""".stripMargin
		}
	}{st =>
		val Seq(item_type, ts, hash_id, ip, city, country_code, lon, lat) = 1 to 8

		st.setString(item_type, entry.itemType.toString)
		st.setString(ts, entry.ts.toString)
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

		entry.latitude.zip(entry.longitude) match{
			case Some((latitude, longitude)) =>
				st.setDouble(lon, longitude)
				st.setDouble(lat, latitude)
			case None => ;
		}

		st.executeUpdate()
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
		ds.setDefaultMinIdle(1)
		ds.setDefaultMaxIdle(2)
		ds.setConnectionPoolDataSource(pgDs)
		ds.setDefaultAutoCommit(false)
		ds
	}.toMap

	override def close(): Unit = {
		executor.shutdown()
		dataSources.valuesIterator.foreach{_.close()}
	}

	private def withConnection[T](creds: CredentialsConfig)(act: Connection => T)(implicit envri: Envri): Future[T] = Future{
		val conn = dataSources(envri).getConnection(creds.username, creds.password)
		conn.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT)
		try {
			act(conn)
		} finally{
			conn.close()
		}
	}

	private def withTransaction(creds: CredentialsConfig)(query: String)(act: PreparedStatement => Unit)(implicit envri: Envri): Future[Done] = {
		withConnection(creds){conn =>
			val st = conn.prepareStatement(query)
			try{
				act(st)
				conn.commit()
				Done
			}finally{
				st.close()
			}
		}
	}
}
