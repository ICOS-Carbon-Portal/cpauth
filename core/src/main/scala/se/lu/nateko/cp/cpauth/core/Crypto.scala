package se.lu.nateko.cp.cpauth.core

import spray.json.JsObject
import spray.json.enrichString

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import scala.util.Failure
import scala.util.Success
import scala.util.Try


case class Signature(bytes: Array[Byte]){
	def base64: String = CoreUtils.encodeToBase64String(bytes)
}

object Crypto:

	type KeyType = "EC" | "RSA"

	def sha256sum(s: String): Array[Byte] = MessageDigest
		.getInstance("SHA-256")
		.digest(getMessageBytes(s))

	def publicKeyFromX509Cert(base64: String): Try[PublicKey] = Try{
		val cf = CertificateFactory.getInstance("X.509")
		val certBytes: Array[Byte] = CoreUtils.decodeBase64(base64)
		val certInputStream = new ByteArrayInputStream(certBytes)
		val cert = cf.generateCertificate(certInputStream).asInstanceOf[X509Certificate]
		cert.getPublicKey
	}

	def privateFromDerBytes(keyBytes: Array[Byte], ktype: KeyType): Try[PrivateKey] = Try{
		val privateKeySpec = new PKCS8EncodedKeySpec(keyBytes)
		KeyFactory.getInstance(ktype).generatePrivate(privateKeySpec)
	}
	
	def publicFromPemLines(lines: IndexedSeq[String], ktype: KeyType): Try[PublicKey] =
		keyBytesFromPemLines(lines, "PUBLIC").flatMap(keyBytes => Try{
			val publicKeySpec = new X509EncodedKeySpec(keyBytes)
			KeyFactory.getInstance(ktype).generatePublic(publicKeySpec)
		})
	
	def signMessage(msg: String, key: PrivateKey): Signature = {
		val signer = getSigner
		signer.initSign(key)
		signer.update(getMessageBytes(msg))
		val signBytes = signer.sign()
		new Signature(signBytes)
	}
	
	def verifySignature(msg: String, key: PublicKey, signature: Signature): Try[Boolean] = Try{
		val signer = getSigner
		signer.initVerify(key)
		signer.update(getMessageBytes(msg))
		signer.verify(signature.bytes)
	}
	
	private def getSigner = java.security.Signature.getInstance("SHA256withECDSA")
	private def getMessageBytes(msg: String): Array[Byte] = msg.getBytes(StandardCharsets.UTF_8)
	
	private def keyBytesFromPemLines(lines: IndexedSeq[String], keyType: String): Try[Array[Byte]] = {
		
		val prologue = s"-----BEGIN $keyType KEY-----"
		val epilogue = s"-----END $keyType KEY-----"

		if(lines.size > 2 && lines.head == prologue && lines.last == epilogue){
			
			val encoded = lines.tail.take(lines.size - 2).mkString("") 
			Try(CoreUtils.decodeBase64(encoded))
			
		}else Failure(new Exception(s"Expected key specification to start with $prologue line, end with $epilogue line, and have body"))
	}

	def parseJWTpayload(allBase64Url: String): Try[JsObject] =
		val parts = allBase64Url.split('.')
		if parts.length != 3 then Failure(Exception("JWT must have 3 parts"))
		else Try(CoreUtils.decodeBase64UrlToString(parts(1))).flatMap(
			_.parseJson match
				case obj: JsObject => Success(obj)
				case _ => Failure(Exception("JWT payload must be a JSON object"))
		)

end Crypto
