package se.lu.nateko.cpauth.core

import scala.io.Source
import org.apache.commons.codec.binary.Base64

object CoreUtils {

	def decode64(in: String) = new String(Base64.decodeBase64(in), "UTF-8")
  
	def getResourceBytes(resourcePath: String): Array[Byte] = {
		import java.nio.file.{Files, Paths}
		Files.readAllBytes(Paths.get(getClass.getResource(resourcePath).toURI))
	}

	def getResourceLines(resourcePath: String): Iterator[String] = {
		val stream = getClass.getResourceAsStream(resourcePath)
		Source.fromInputStream(stream, "UTF-8").getLines
	}

}