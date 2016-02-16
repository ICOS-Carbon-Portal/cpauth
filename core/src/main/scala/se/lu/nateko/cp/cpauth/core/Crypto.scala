package se.lu.nateko.cp.cpauth.core

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

import scala.util.Failure
import scala.util.Try


case class Signature(bytes: Array[Byte]){
	def base64: String = CoreUtils.encodeToBase64String(bytes)
}

object Crypto{

	def publicKeyFromX509Cert(base64: String): Try[PublicKey] = Try{
		val cf = CertificateFactory.getInstance("X.509")
		val certBytes: Array[Byte] = CoreUtils.decodeBase64(base64)
		val certInputStream = new ByteArrayInputStream(certBytes)
		val cert = cf.generateCertificate(certInputStream).asInstanceOf[X509Certificate]
		cert.getPublicKey
	}

	def rsaPrivateFromDerBytes(keyBytes: Array[Byte]): Try[RSAPrivateKey] = Try{
		val privateKeySpec = new PKCS8EncodedKeySpec(keyBytes)
		KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec).asInstanceOf[RSAPrivateKey]
	}
	
	def rsaPublicFromPemLines(lines: IndexedSeq[String]): Try[RSAPublicKey] =
		keyBytesFromPemLines(lines, "PUBLIC").flatMap(keyBytes => Try{
			val publicKeySpec = new X509EncodedKeySpec(keyBytes)
			KeyFactory.getInstance("RSA").generatePublic(publicKeySpec).asInstanceOf[RSAPublicKey]
		})
	
	def signMessage(msg: String, key: RSAPrivateKey): Signature = {
		val signer = getSigner
		signer.initSign(key)
		signer.update(getMessageBytes(msg))
		val signBytes = signer.sign()
		new Signature(signBytes)
	}
	
	def verifySignature(msg: String, key: RSAPublicKey, signature: Signature): Try[Boolean] = Try{
		val signer = getSigner
		signer.initVerify(key)
		signer.update(getMessageBytes(msg))
		signer.verify(signature.bytes)
	}
	
	private def getSigner = java.security.Signature.getInstance("SHA1withRSA")
	private def getMessageBytes(msg: String): Array[Byte] = msg.getBytes(Charset.forName("UTF-8"))
	
	private def keyBytesFromPemLines(lines: IndexedSeq[String], keyType: String): Try[Array[Byte]] = {
		
		val prologue = s"-----BEGIN $keyType KEY-----"
		val epilogue = s"-----END $keyType KEY-----"

		if(lines.size > 2 && lines.head == prologue && lines.last == epilogue){
			
			val encoded = lines.tail.take(lines.size - 2).mkString("") 
			Try(CoreUtils.decodeBase64(encoded))
			
		}else Failure(new Exception(s"Expected key specification to start with $prologue line, end with $epilogue line, and have body"))
	}

}