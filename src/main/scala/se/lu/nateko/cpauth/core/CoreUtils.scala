package se.lu.nateko.cpauth.core

import scala.io.Source
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.Inflater
import java.util.zip.InflaterOutputStream

object CoreUtils {

	private[this] val utf8 = java.nio.charset.StandardCharsets.UTF_8

	def decode64(in: String) = new String(Base64.decodeBase64(in), utf8)

	def getResourceBytes(resourcePath: String): Array[Byte] = {
		val stream = getClass.getResourceAsStream(resourcePath)
		if(stream == null) Array()
		else IOUtils.toByteArray(stream)
	}

	def getResourceLines(resourcePath: String): Iterator[String] = {
		val stream = getClass.getResourceAsStream(resourcePath)
		if(stream == null) Iterator()
		else Source.fromInputStream(stream, utf8.displayName).getLines
	}

	def getResourceAsString(resourcePath: String): String = {
		val stream = getClass.getResourceAsStream(resourcePath)
		if(stream == null) ""
		else IOUtils.toString(stream, utf8)
	}

	def compressAndBase64(s: String): String = compressAndBase64(s, false)

	def compressAndBase64(s: String, hard: Boolean): String = {
		val level = if(hard) Deflater.BEST_COMPRESSION else Deflater.DEFAULT_COMPRESSION
		val deflater = new Deflater(level, true);
		val byteStream = new ByteArrayOutputStream()

		val defstr = new DeflaterOutputStream(byteStream, deflater)
		defstr.write(s.getBytes(utf8))
		defstr.close()
		byteStream.close()

		new String(new Base64().encode(byteStream.toByteArray), utf8)
	}

	def decompressFromBase64(s: String): String = {
		val compressedBytes: Array[Byte] = Base64.decodeBase64(s)

		val byteStream = new ByteArrayOutputStream()

		val inflStream = new InflaterOutputStream(byteStream, new Inflater(true))
		inflStream.write(compressedBytes)
		inflStream.close()

		new String(byteStream.toByteArray, utf8)
	}

}