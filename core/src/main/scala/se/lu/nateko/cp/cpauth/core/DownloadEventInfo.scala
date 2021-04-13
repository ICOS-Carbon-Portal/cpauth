package se.lu.nateko.cp.cpauth.core

import java.time.Instant
import spray.json._


sealed trait DownloadEventInfo{
	def time: Instant
	def ip: String
	def hashId: String
}

case class CollectionDownloadInfo(time: Instant, ip: String, hashId: String, coll: JsObject) extends DownloadEventInfo
case class DocumentDownloadInfo(time: Instant, ip: String, hashId: String, doc: JsObject) extends DownloadEventInfo
case class CsvDownloadInfo(
	time: Instant,
	ip: String,
	hashId: String,
	columns: Option[Seq[String]],
	offset: Option[Long],
	limit: Option[Int]
) extends DownloadEventInfo

case class DataObjDownloadInfo(
	time: Instant,
	ip: String,
	hashId: String,
	dobj: JsObject,
	distributor: Option[String],
	endUser: Option[String]
) extends DownloadEventInfo


object DownloadEventInfo extends DefaultJsonProtocol{

	implicit object javaTimeInstantFormat extends RootJsonFormat[Instant] {

		def write(instant: Instant) = JsString(instant.toString)

		def read(value: JsValue): Instant = value match{
			case JsString(s) => Instant.parse(s)
			case _ => deserializationError("String representation of a time instant is expected")
		}
	}

	implicit val collectionDlInfoFormat = jsonFormat4(CollectionDownloadInfo)
	implicit val docDlInfoFormat = jsonFormat4(DocumentDownloadInfo)
	implicit val dataDlInfoFormat = jsonFormat6(DataObjDownloadInfo)
	implicit val csvDlInfoFormat = jsonFormat6(CsvDownloadInfo)

	implicit object downloadEventInfoFormat extends RootJsonFormat[DownloadEventInfo]{

		override def write(obj: DownloadEventInfo): JsValue = obj match{
			case coll: CollectionDownloadInfo => coll.toJson
			case doc: DocumentDownloadInfo => doc.toJson
			case data: DataObjDownloadInfo => data.toJson
			case csv: CsvDownloadInfo => csv.toJson
		}

		override def read(json: JsValue): DownloadEventInfo = {
			val obj = json.asJsObject("Expected DownloadEventInfo to be a JS object, not a plain value")
			if(obj.fields.contains("coll")) obj.convertTo[CollectionDownloadInfo]
			else if(obj.fields.contains("doc")) obj.convertTo[DocumentDownloadInfo]
			else if(obj.fields.contains("dobj")) obj.convertTo[DataObjDownloadInfo]
			else deserializationError("Expected DownloadEventInfo to contain one of: 'coll', 'doc', 'dobj', but found none")
		}
	}
}
