package se.lu.nateko.cpauth.core

import scala.io.Source
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

object CoreUtils {

	def decode64(in: String) = new String(Base64.decodeBase64(in), "UTF-8")

	def getResourceBytes(resourcePath: String): Array[Byte] = {
		val stream = getClass.getResourceAsStream(resourcePath)
		IOUtils.toByteArray(stream)
	}

	def getResourceLines(resourcePath: String): Iterator[String] = {
		val stream = getClass.getResourceAsStream(resourcePath)
		Source.fromInputStream(stream, "UTF-8").getLines
	}

	def compressAndBase64(s: String): String = {

		val utf8 = java.nio.charset.StandardCharsets.UTF_8

		val byteStream = new ByteArrayOutputStream();
		val deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);

		val defstr = new DeflaterOutputStream(byteStream, deflater);
		defstr.write(s.getBytes(utf8))
		defstr.close()
		byteStream.close()

		val res = new String(new Base64().encode(byteStream.toByteArray), utf8);

		res.trim
	}

}