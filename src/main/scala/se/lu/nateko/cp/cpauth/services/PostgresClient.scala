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
import se.lu.nateko.cp.cpauth.core.DownloadEventInfo
import se.lu.nateko.cp.cpauth.core.DataObjDownloadInfo
import se.lu.nateko.cp.cpauth.core.DocumentDownloadInfo
import se.lu.nateko.cp.cpauth.core.CollectionDownloadInfo

class PostgresClient(conf: PostgresConfig) extends AutoCloseable {

	def logDownload(dlInfo: DownloadEventInfo, ip: Either[String, GeoIpInfo])(implicit envri: Envri): Future[Done] = withTransaction(conf.writer){
		ip.fold(
			ip => """INSERT INTO downloads(item_type, ts, hash_id, ip, distributor, endUser)
				|VALUES (?, ?::timestamptz at time zone 'utc', ?, ?, ?, ?)""".stripMargin,
			geo =>
				"""INSERT INTO downloads(item_type, ts, hash_id, ip, city, country_code, pos, distributor, endUser)
				|VALUES (?, ?::timestamptz at time zone 'utc', ?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326), ?, ?)""".stripMargin
		)
	}{st =>
		val Seq(item_type, ts, hash_id, ip_idx, city, country_code, lon, lat, distributor_idx, endUser_idx) = 1 to 10

		val itemType = dlInfo match{
			case _: DataObjDownloadInfo => "data"
			case _: DocumentDownloadInfo => "document"
			case _: CollectionDownloadInfo => "collection"
		}
		st.setString(item_type, itemType)
		st.setString(ts, dlInfo.time.toString) //TODO investigate .setTimestamp or similar, to avoid .toString
		st.setString(hash_id, dlInfo.hashId)
		st.setString(ip_idx, ip.fold(identity, _.ip))

		ip.foreach{geo =>

			geo.city match {
				case Some(c) => st.setString(city, c)
				case None => st.setNull(city, Types.VARCHAR)
			}

			geo.country_code match {
				case Some(cc) => st.setString(country_code, cc)
				case None => st.setNull(country_code, Types.VARCHAR)
			}

			st.setDouble(lon, geo.longitude)
			st.setDouble(lat, geo.latitude)

		}

		val dobjOpt = Option(dlInfo).collect{ case d: DataObjDownloadInfo => d }

		dobjOpt.flatMap(_.distributor) match {
			case Some(distributor) => st.setString(distributor_idx, distributor)
			case _                 => st.setNull(distributor_idx, Types.VARCHAR)
		}

		dobjOpt.flatMap(_.endUser) match {
			case Some(endUser) => st.setString(endUser_idx, endUser)
			case _             => st.setNull(endUser_idx, Types.VARCHAR)
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
