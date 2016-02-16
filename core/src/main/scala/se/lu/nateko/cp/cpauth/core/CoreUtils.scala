package se.lu.nateko.cp.cpauth.core

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterOutputStream

import scala.Iterator
import scala.io.Source

object CoreUtils {

	private[this] val utf8 = java.nio.charset.StandardCharsets.UTF_8
	private[this] val base64Decoder = Base64.getDecoder
	private[this] val base64Encoder = Base64.getEncoder
	private[this] val whiteRegex = "\\s+".r

	def decodeBase64ToString(in: String): String =
		new String(base64Decoder.decode(noWhite(in)), utf8)

	def encodeToBase64String(in: Array[Byte]): String =
		base64Encoder.encodeToString(in)

	def decodeBase64(in: String): Array[Byte] =
		base64Decoder.decode(noWhite(in))

	def noWhite(s: String): String = {
		whiteRegex.replaceAllIn(s, "")
	}

	def getResourceBytes(resourcePath: String): Array[Byte] = {
		val res = getClass.getResource(resourcePath)
		if(res == null) Array.empty else{
			val path = Paths.get(res.toURI)
			Files.readAllBytes(path)
		}
	}

	def getResourceAsString(resourcePath: String): String = {
		val bytes = getResourceBytes(resourcePath)
		new String(bytes, utf8)
	}

	def getResourceLines(resourcePath: String): Iterator[String] = {
		val stream = getClass.getResourceAsStream(resourcePath)
		if(stream == null) Iterator()
		else Source.fromInputStream(stream, utf8.displayName).getLines
	}

	def compress(data: Array[Byte]): Array[Byte] = {
		val deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
		val byteStream = new ByteArrayOutputStream()

		val defstr = new DeflaterOutputStream(byteStream, deflater)
		defstr.write(data)
		defstr.close()
		byteStream.close()

		byteStream.toByteArray
	}

	def decompress(data: Array[Byte]): Array[Byte] = {
		val byteStream = new ByteArrayOutputStream()

		val inflStream = new InflaterOutputStream(byteStream, new Inflater(true))
		inflStream.write(data)
		inflStream.close()

		byteStream.toByteArray
	}

}