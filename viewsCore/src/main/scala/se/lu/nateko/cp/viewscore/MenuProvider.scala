package se.lu.nateko.cp.viewscore

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import scala.util.Success

object MenuProvider:

	private var _cpMenu: Option[Seq[CpMenuItem]] = None
	private var _citiesMenu: Option[Seq[CpMenuItem]] = None

	private val executor =
		val pool = new ScheduledThreadPoolExecutor(0)
		pool.setMaximumPoolSize(1)
		pool.setKeepAliveTime(1, TimeUnit.MINUTES)
		pool

	private val getCpMenuTask = new Runnable:
		def run(): Unit = MenuFetcher.getMenu(CpMenu.cpMenuApi) match
			case Success(newMenu) => _cpMenu = Some(newMenu)
			case _ =>

	private val getCitiesMenuTask = new Runnable:
		def run(): Unit = MenuFetcher.getMenu(CpMenu.citiesMenuApi) match
			case Success(newMenu) => _citiesMenu = Some(newMenu)
			case _ =>


	executor.scheduleAtFixedRate(getCpMenuTask, 0, 12, TimeUnit.HOURS)
	executor.scheduleAtFixedRate(getCitiesMenuTask, 0, 12, TimeUnit.HOURS)

	def cpMenu = _cpMenu
	def citiesMenu = _citiesMenu
