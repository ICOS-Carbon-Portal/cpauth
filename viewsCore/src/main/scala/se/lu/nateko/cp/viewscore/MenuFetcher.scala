package se.lu.nateko.cp.viewscore

import scala.util.Try
import spray.json.{enrichString, JsonFormat}

object MenuFetcher:

	import spray.json.DefaultJsonProtocol.{lazyFormat, jsonFormat, immSeqFormat, StringJsonFormat}

	given JsonFormat[CpMenuItem] = lazyFormat(jsonFormat(CpMenuItem.apply, "title", "url", "children"))

	def getMenu(menuApi: String): Try[Seq[CpMenuItem]] = Try {
		scala.io.Source.fromURL(menuApi).mkString.parseJson.convertTo[Seq[CpMenuItem]]
	}
