package se.lu.nateko.cp.viewscore

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import scala.util.Success

object MenuProvider {

	private[this] var _menu: Option[Seq[CpMenuItem]] = None

	val hasInitialized = {

		val task = new Runnable {

			def run(): Unit = MenuFetcher.getMenu match{
				case Success(newMenu) => _menu = Some(newMenu)
				case _ =>
			}
		}

		new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(task, 0, 12, TimeUnit.HOURS)

		true
	}

	def menu = _menu

}