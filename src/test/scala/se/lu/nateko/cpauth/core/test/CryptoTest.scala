package se.lu.nateko.cpauth.core.test

import scala.util.Try
import org.scalatest.FunSuite
import se.lu.nateko.cpauth.core.CoreUtils
import se.lu.nateko.cpauth.core.Crypto
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class CryptoTest extends FunSuite {

	val privateKey: Try[RSAPrivateKey] = {
		val keyBytes = CoreUtils.getResourceBytes("/private1.der")
		Crypto.rsaPrivateFromDerBytes(keyBytes)
	}
	
	val publicKey: Try[RSAPublicKey] = {
		val keyFileLines = CoreUtils.getResourceLines("/public1.pem")
		Crypto.rsaPublicFromPemLines(keyFileLines.toIndexedSeq)
	}
	
	test("Public RSA key creation from file works"){
		assert(publicKey.isSuccess)
	}
	
	test("Private RSA key creation from file works"){
		assert(privateKey.isSuccess)
	}
	
	test("Signing a simple message succeeds"){
		val signature = Crypto.signMessage("blabla", privateKey.get)
		assert(signature.base64.length > 0)
	}
	
	test("Message signature gets verified successfully"){
		val msg = "qweqwe"
		val signature = Crypto.signMessage(msg, privateKey.get)
		val verification = Crypto.verifySignature(msg, publicKey.get, signature)

		assert(verification.isSuccess && verification.get)
	}

	test("Public key extraction from X509 base64-encoded cert string works"){
		val certStr: String = CoreUtils.getResourceAsString("/certX509base64.txt")
		val key = Crypto.publicKeyFromX509Cert(certStr).get
		
		assert(key.getAlgorithm === "RSA")
	}

}
