package se.lu.nateko.cp.cpauth.core

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.{UTF_8 => utf8}
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterOutputStream
import scala.Iterator
import scala.io.Source
import java.nio.file.FileSystems
import java.net.URI

object CoreUtils {

	private val whiteRegex = "\\s+".r

	def decodeBase64ToString(in: String) = new String(decodeBase64(in), utf8)
	def decodeBase64UrlToString(in: String) = new String(decodeBase64Url(in), utf8)

	def encodeToBase64String(in: Array[Byte]): String = Base64.getEncoder.encodeToString(in)

	def decodeBase64(in: String): Array[Byte] = Base64.getDecoder.decode(noWhite(in))
	def decodeBase64Url(in: String): Array[Byte] = Base64.getUrlDecoder.decode(noWhite(in))

	def noWhite(s: String): String = whiteRegex.replaceAllIn(s, "")

	def getResourceBytes(resourcePath: String): Array[Byte] = {
		val res = getClass.getResource(resourcePath)
		if(res == null) Array.empty else{
			val uri = res.toURI
			if(uri.toString.contains("!")){
				val uriParts = uri.toString.split("!")
				val fs = FileSystems.newFileSystem(
					URI.create(uriParts(0)),
					new java.util.HashMap[String, String]
				)
				val path = fs.getPath(uriParts(1))
				val res = Files.readAllBytes(path)
				fs.close()
				res
			}
			else Files.readAllBytes(Paths.get(uri))
		}
	}

	def getResourceAsString(resourcePath: String): String = {
		val bytes = getResourceBytes(resourcePath)
		new String(bytes, utf8)
	}

	def getResourceLines(resourcePath: String): Iterator[String] = {
		val stream = getClass.getResourceAsStream(resourcePath)
		if(stream == null) Iterator()
		else Source.fromInputStream(stream, utf8.displayName).getLines()
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