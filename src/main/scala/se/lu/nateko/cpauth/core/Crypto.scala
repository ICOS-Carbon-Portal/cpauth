package se.lu.nateko.cpauth.core

class PKCS8EncodedKey(val bytes: Array[Byte]) extends AnyVal

object Crypto{

	def decode64(in: String): String = {
		import org.apache.commons.codec.binary.Base64
		val decoder = new Base64()
		new String(decoder.decode(in), "UTF-8")
	}

}