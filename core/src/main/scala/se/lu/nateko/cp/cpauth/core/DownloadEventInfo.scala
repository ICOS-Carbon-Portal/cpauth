package se.lu.nateko.cp.cpauth.core

import java.time.Instant
import spray.json._
import DownloadEventInfo.{CsvSelect, CpbSlice, AnonId}


sealed trait DownloadEventInfo{
	def time: Instant
	def ip: String
	def hashId: String
	def cpUser: Option[AnonId]
}

case class CollectionDownloadInfo(time: Instant, ip: String, hashId: String, cpUser: Option[AnonId], coll: JsObject) extends DownloadEventInfo
case class DocumentDownloadInfo(time: Instant, ip: String, hashId: String, cpUser: Option[AnonId], doc: JsObject) extends DownloadEventInfo
case class CsvDownloadInfo(
	time: Instant,
	ip: String,
	hashId: String,
	cpUser: Option[AnonId],
	select: DownloadEventInfo.CsvSelect
) extends DownloadEventInfo

case class CpbDownloadInfo(
	time: Instant,
	ip: String,
	hashId: String,
	cpUser: Option[AnonId],
	colNums: Seq[Int],
	slice: Option[DownloadEventInfo.CpbSlice],
	localOrigin: Option[String]
) extends DownloadEventInfo

case class DataObjDownloadInfo(
	time: Instant,
	ip: String,
	hashId: String,
	cpUser: Option[AnonId],
	dobj: JsObject,
	distributor: Option[String],
	endUser: Option[String]
) extends DownloadEventInfo


object DownloadEventInfo extends DefaultJsonProtocol{

	type AnonId = String
	case class CsvSelect(columns: Option[Seq[String]], offset: Option[Long], limit: Option[Int])
	case class CpbSlice(offset: Long, length: Int)

	def anonymizeCpUser(id: UserId, salt: String): AnonId =
		CoreUtils.encodeToBase64String(Crypto.sha256sum(id.email + salt)).take(12)

	implicit object javaTimeInstantFormat extends RootJsonFormat[Instant] {
		def write(instant: Instant) = JsString(instant.toString)
		def read(value: JsValue): Instant = value match{
			case JsString(s) => Instant.parse(s)
			case _ => deserializationError("String representation of a time instant is expected")
		}
	}

	implicit val collectionDlInfoFormat = jsonFormat5(CollectionDownloadInfo)
	implicit val docDlInfoFormat = jsonFormat5(DocumentDownloadInfo)
	implicit val dataDlInfoFormat = jsonFormat7(DataObjDownloadInfo)
	implicit val csvSelectFormat = jsonFormat3(CsvSelect)
	implicit val csvDlInfoFormat = jsonFormat5(CsvDownloadInfo)
	implicit val cbpSliceFormat = jsonFormat2(CpbSlice)
	implicit val cpbDlInfoFormat = jsonFormat7(CpbDownloadInfo)

	implicit object downloadEventInfoFormat extends RootJsonFormat[DownloadEventInfo]{

		override def write(obj: DownloadEventInfo): JsValue = {
			def withType[T : JsonWriter](typ: String, e: T) =
				JsObject(e.toJson.asJsObject.fields + ("type" -> JsString(typ)))

			obj match{
				case coll: CollectionDownloadInfo => withType("coll", coll)
				case doc: DocumentDownloadInfo => withType("doc", doc)
				case data: DataObjDownloadInfo => withType("dobj", data)
				case csv: CsvDownloadInfo => withType("csv", csv)
				case cpb: CpbDownloadInfo => withType("cpb", cpb)
			}
		}

		override def read(json: JsValue): DownloadEventInfo = {
			val obj = json.asJsObject("Expected DownloadEventInfo to be a JS object, not a plain value")
			//TODO Switch to using field 'type' for determining the type
			if(obj.fields.contains("coll")) obj.convertTo[CollectionDownloadInfo]
			else if(obj.fields.contains("doc")) obj.convertTo[DocumentDownloadInfo]
			else if(obj.fields.contains("dobj")) obj.convertTo[DataObjDownloadInfo]
			else if(obj.fields.contains("select")) obj.convertTo[CsvDownloadInfo]
			else if(obj.fields.contains("colNums")) obj.convertTo[CpbDownloadInfo]
			else deserializationError("Expected DownloadEventInfo to contain one of: 'coll', 'doc', 'dobj', 'select', 'colNums', but found none")
		}
	}
}
