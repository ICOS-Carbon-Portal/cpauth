package se.lu.nateko.cp.viewscore

import scala.util.Try
import spray.json._

object MenuFetcher {

	import spray.json.DefaultJsonProtocol._



	implicit val menuInfoFormat: JsonFormat[CpMenuItem] = lazyFormat(jsonFormat(CpMenuItem, "title", "url", "children"))

	def getMenu: Try[Seq[CpMenuItem]] = Try {
		scala.io.Source.fromURL(CpMenu.cpMenuApi).mkString.parseJson.convertTo[Seq[CpMenuItem]]
	}

}
