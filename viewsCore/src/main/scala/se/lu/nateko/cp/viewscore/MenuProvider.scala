package se.lu.nateko.cp.viewscore

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import scala.util.Success

object MenuProvider:

	private var _menu: Option[Seq[CpMenuItem]] = None

	private val executor =
		val pool = new ScheduledThreadPoolExecutor(0)
		pool.setMaximumPoolSize(1)
		pool.setKeepAliveTime(1, TimeUnit.MINUTES)
		pool

	private val task = new Runnable:
		def run(): Unit = MenuFetcher.getMenu match
			case Success(newMenu) => _menu = Some(newMenu)
			case _ =>


	executor.scheduleAtFixedRate(task, 0, 12, TimeUnit.HOURS)

	def menu = _menu
