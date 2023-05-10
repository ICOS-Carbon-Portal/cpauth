package eu.icoscp.utils.akkauri

import akka.http.scaladsl.model.Uri
import java.net.URI

given uriJavaToAkka: Conversion[URI, Uri] = uri => Uri(uri.toString)

extension (uri: Uri)
	def appendPathSegment(segment: String): Uri = uri.withPath(uri.path / segment)
