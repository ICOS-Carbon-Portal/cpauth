package se.lu.nateko.cpauth.core

import scala.util.{Try, Success, Failure}
import org.apache.commons.codec.binary.Base64
import java.security.spec.X509EncodedKeySpec
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.Signature
import java.nio.charset.Charset


class PKCS8EncodedKey(val bytes: Array[Byte]) extends AnyVal
class PEMPrivateRsaKey(val bytes: Array[Byte]) extends AnyVal
class PEMPublicRsaKey(val bytes: Array[Byte]) extends AnyVal

object Crypto{

	def decode64(in: String) = new String(Base64.decodeBase64(in), "UTF-8")
	
	def rsaPrivateFromPemLines(lines: IndexedSeq[String]): Try[PEMPrivateRsaKey] =
		keyFromPemLines(lines, "RSA PRIVATE").map(new PEMPrivateRsaKey(_))
		
	def rsaPublicFromPemLines(lines: IndexedSeq[String]): Try[PEMPublicRsaKey] =
		keyFromPemLines(lines, "RSA PUBLIC").map(new PEMPublicRsaKey(_))
	
	private def keyFromPemLines(lines: IndexedSeq[String], keyType: String): Try[Array[Byte]] = {
		
		val prologue = s"-----BEGIN $keyType KEY-----"
		val epilogue = s"-----END $keyType KEY-----"

		if(lines.size > 2 &&	lines.head == prologue &&	lines.last == epilogue){
			
			val encoded = lines.tail.take(lines.size - 2).mkString("") 
			Try(Base64.decodeBase64(encoded))
			
		}else Failure(new Exception(s"Expected key specification to start with $prologue line, end with $epilogue line, and have body"))
	}
	
	def signMessage(msg: String, key: PEMPrivateRsaKey): String = {
		val privateKeySpec = new X509EncodedKeySpec(key.bytes)
		val privateKey = KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec).asInstanceOf[RSAPrivateKey]
		
		val signature = Signature.getInstance("SHA1withRSA")
		signature.initSign(privateKey)
		val msgBytes = msg.getBytes(Charset.forName("UTF-8"))
		signature.update(msgBytes)
		val signBytes = signature.sign()
		Base64.encodeBase64String(signBytes)
	}

}