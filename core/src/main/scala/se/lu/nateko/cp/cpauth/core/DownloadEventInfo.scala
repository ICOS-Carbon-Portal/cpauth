package se.lu.nateko.cp.cpauth.core

import java.time.Instant
import spray.json.*
import DownloadEventInfo.{CsvSelect, CpbSlice, AnonId}
import se.lu.nateko.cp.cpauth.core.CoreUtils
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.core.Crypto


trait DownloadEventInfo:
	def time: Instant
	def ip: String
	def hashId: String
	def cpUser: Option[AnonId]


sealed trait DlEventForMongo extends DownloadEventInfo:
	def userAgent: Option[String]

case class CsvDownloadInfo(
	time: Instant,
	ip: String,
	hashId: String,
	cpUser: Option[AnonId],
	userAgent: Option[String],
	select: DownloadEventInfo.CsvSelect
) extends DlEventForMongo

case class CpbDownloadInfo(
	time: Instant,
	ip: String,
	hashId: String,
	cpUser: Option[AnonId],
	colNums: Seq[Int],
	slice: Option[DownloadEventInfo.CpbSlice],
	localOrigin: Option[String],
	userAgent: Option[String]
) extends DlEventForMongo


case class ZipExtractionInfo(
	time: Instant,
	ip: String,
	hashId: String,
	zipEntryPath: String,
	cpUser: Option[AnonId],
	localOrigin: Option[String],
	userAgent: Option[String]
) extends DlEventForMongo


object DownloadEventInfo extends DefaultJsonProtocol:

	type AnonId = String
	case class CsvSelect(columns: Option[Seq[String]], offset: Option[Long], limit: Option[Int])
	case class CpbSlice(offset: Long, length: Int)

	def anonymizeCpUser(id: UserId, salt: String): AnonId =
		CoreUtils.encodeToBase64String(Crypto.sha256sum(id.email + salt)).take(12)

	given RootJsonFormat[Instant] with {
		def write(instant: Instant) = JsString(instant.toString)
		def read(value: JsValue): Instant = value match
			case JsString(s) => Instant.parse(s)
			case _ => deserializationError("String representation of a time instant is expected")
	}

	given RootJsonFormat[CsvSelect] = jsonFormat3(CsvSelect.apply)
	given RootJsonFormat[CsvDownloadInfo] = jsonFormat6(CsvDownloadInfo.apply)
	given RootJsonFormat[CpbSlice] = jsonFormat2(CpbSlice.apply)
	given RootJsonFormat[CpbDownloadInfo] = jsonFormat8(CpbDownloadInfo.apply)
	given RootJsonFormat[ZipExtractionInfo] = jsonFormat7(ZipExtractionInfo.apply)

	given RootJsonFormat[DownloadEventInfo] with {

		override def write(obj: DownloadEventInfo): JsValue =
			def withType[T : JsonWriter](typ: String, e: T) =
				JsObject(e.toJson.asJsObject.fields + ("type" -> JsString(typ)))

			obj match
				case csv: CsvDownloadInfo => withType("csv", csv)
				case cpb: CpbDownloadInfo => withType("cpb", cpb)
				case zip: ZipExtractionInfo => withType("zip", zip)

		override def read(json: JsValue): DownloadEventInfo =
			val obj = json.asJsObject("Expected DownloadEventInfo to be a JS object, not a plain value")
			obj.fields.get("type").collect{case JsString(typ) => typ} match
				case Some("cpb") => obj.convertTo[CpbDownloadInfo]
				case Some("csv") => obj.convertTo[CsvDownloadInfo]
				case Some("zip") => obj.convertTo[ZipExtractionInfo]
				case None =>
					deserializationError("Missing field 'type' on JSON for DownloadEventInfo")
				case Some(other) =>
					deserializationError(s"Unsupported type of DownloadEventInfo: $other")
	}

end DownloadEventInfo
