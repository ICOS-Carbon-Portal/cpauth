package se.lu.nateko.cpauth.core

import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

import scala.util.Failure
import scala.util.Try

import org.apache.commons.codec.binary.Base64


class PKCS8EncodedKey(val bytes: Array[Byte]) extends AnyVal

object Crypto{

	def decode64(in: String) = new String(Base64.decodeBase64(in), "UTF-8")
	
	def rsaPrivateFromDerBytes(keyBytes: Array[Byte]): Try[RSAPrivateKey] = Try{
		val privateKeySpec = new PKCS8EncodedKeySpec(keyBytes)
		KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec).asInstanceOf[RSAPrivateKey]
	}
	
	def rsaPublicFromPemLines(lines: IndexedSeq[String]): Try[RSAPublicKey] =
		keyFromPemLines(lines, "PUBLIC").flatMap(keyBytes => Try{
			val publicKeySpec = new X509EncodedKeySpec(keyBytes)
			KeyFactory.getInstance("RSA").generatePublic(publicKeySpec).asInstanceOf[RSAPublicKey]
		})
	
	private def keyFromPemLines(lines: IndexedSeq[String], keyType: String): Try[Array[Byte]] = {
		
		val prologue = s"-----BEGIN $keyType KEY-----"
		val epilogue = s"-----END $keyType KEY-----"

		if(lines.size > 2 &&	lines.head == prologue &&	lines.last == epilogue){
			
			val encoded = lines.tail.take(lines.size - 2).mkString("") 
			Try(Base64.decodeBase64(encoded))
			
		}else Failure(new Exception(s"Expected key specification to start with $prologue line, end with $epilogue line, and have body"))
	}
	
	private def getSignature: Signature = Signature.getInstance("SHA1withRSA")
	private def getMessageBytes(msg: String): Array[Byte] = msg.getBytes(Charset.forName("UTF-8"))
	
	def signMessage(msg: String, key: RSAPrivateKey): String = {
		val signature = getSignature
		signature.initSign(key)
		signature.update(getMessageBytes(msg))
		val signBytes = signature.sign()
		Base64.encodeBase64String(signBytes)
	}
	
	def verifySignature(msg: String, key: RSAPublicKey, signature: String): Boolean = {
		val signer = getSignature
		signer.initVerify(key)
		signer.update(getMessageBytes(msg))
		val signatureBytes = Base64.decodeBase64(signature)
		signer.verify(signatureBytes)
	}

}