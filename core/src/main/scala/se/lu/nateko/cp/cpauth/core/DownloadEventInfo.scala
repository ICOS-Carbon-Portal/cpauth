package se.lu.nateko.cp.cpauth.core

import java.time.Instant
import spray.json.*
import DownloadEventInfo.{CsvSelect, CpbSlice, AnonId}


sealed trait DownloadEventInfo{
	def time: Instant
	def ip: String
	def hashId: String
	def cpUser: Option[AnonId]
}

case class CollectionDownloadInfo(time: Instant, ip: String, hashId: String, cpUser: Option[AnonId]) extends DownloadEventInfo
case class DocumentDownloadInfo(time: Instant, ip: String, hashId: String, cpUser: Option[AnonId]) extends DownloadEventInfo
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
	distributor: Option[String],
	endUser: Option[String]
) extends DownloadEventInfo

case class ZipExtractionInfo(
	time: Instant,
	ip: String,
	hashId: String,
	zipEntryPath: String,
	cpUser: Option[AnonId],
	localOrigin: Option[String]
) extends DownloadEventInfo


object DownloadEventInfo extends DefaultJsonProtocol{

	type AnonId = String
	case class CsvSelect(columns: Option[Seq[String]], offset: Option[Long], limit: Option[Int])
	case class CpbSlice(offset: Long, length: Int)

	def anonymizeCpUser(id: UserId, salt: String): AnonId =
		CoreUtils.encodeToBase64String(Crypto.sha256sum(id.email + salt)).take(12)

	given RootJsonFormat[Instant] with {
		def write(instant: Instant) = JsString(instant.toString)
		def read(value: JsValue): Instant = value match{
			case JsString(s) => Instant.parse(s)
			case _ => deserializationError("String representation of a time instant is expected")
		}
	}

	given RootJsonFormat[CollectionDownloadInfo] = jsonFormat4(CollectionDownloadInfo.apply)
	given RootJsonFormat[DocumentDownloadInfo] = jsonFormat4(DocumentDownloadInfo.apply)
	given RootJsonFormat[DataObjDownloadInfo] = jsonFormat6(DataObjDownloadInfo.apply)
	given RootJsonFormat[CsvSelect] = jsonFormat3(CsvSelect.apply)
	given RootJsonFormat[CsvDownloadInfo] = jsonFormat5(CsvDownloadInfo.apply)
	given RootJsonFormat[CpbSlice] = jsonFormat2(CpbSlice.apply)
	given RootJsonFormat[CpbDownloadInfo] = jsonFormat7(CpbDownloadInfo.apply)
	given RootJsonFormat[ZipExtractionInfo] = jsonFormat6(ZipExtractionInfo.apply)

	given RootJsonFormat[DownloadEventInfo] with {

		override def write(obj: DownloadEventInfo): JsValue = {
			def withType[T : JsonWriter](typ: String, e: T) =
				JsObject(e.toJson.asJsObject.fields + ("type" -> JsString(typ)))

			obj match{
				case coll: CollectionDownloadInfo => withType("coll", coll)
				case doc: DocumentDownloadInfo => withType("doc", doc)
				case data: DataObjDownloadInfo => withType("dobj", data)
				case csv: CsvDownloadInfo => withType("csv", csv)
				case cpb: CpbDownloadInfo => withType("cpb", cpb)
				case zip: ZipExtractionInfo => withType("zip", zip)
			}
		}

		override def read(json: JsValue): DownloadEventInfo = {
			val obj = json.asJsObject("Expected DownloadEventInfo to be a JS object, not a plain value")
			obj.fields.get("type").collect{case JsString(typ) => typ} match{
				case Some("cpb") => obj.convertTo[CpbDownloadInfo]
				case Some("dobj") => obj.convertTo[DataObjDownloadInfo]
				case Some("coll") => obj.convertTo[CollectionDownloadInfo]
				case Some("doc") => obj.convertTo[DocumentDownloadInfo]
				case Some("csv") => obj.convertTo[CsvDownloadInfo]
				case Some("zip") => obj.convertTo[ZipExtractionInfo]
				case None =>
					deserializationError("Missing field 'type' on JSON for DownloadEventInfo")
				case Some(other) =>
					deserializationError(s"Unsupported type of DownloadEventInfo: $other")
			}
		}
	}
}
