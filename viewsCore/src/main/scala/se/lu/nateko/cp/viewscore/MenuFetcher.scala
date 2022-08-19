package se.lu.nateko.cp.viewscore

import scala.util.Try
import spray.json.*

object MenuFetcher {

	import spray.json.DefaultJsonProtocol.*

	given JsonFormat[CpMenuItem] = lazyFormat(jsonFormat(CpMenuItem.apply, "title", "url", "children"))

	def getMenu: Try[Seq[CpMenuItem]] = Try {
		scala.io.Source.fromURL(CpMenu.cpMenuApi).mkString.parseJson.convertTo[Seq[CpMenuItem]]
	}

}
