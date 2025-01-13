package se.lu.nateko.cp.cpauth.core.test

import scala.util.Try
import org.scalatest.funsuite.AnyFunSuite
import se.lu.nateko.cp.cpauth.core.CoreUtils
import se.lu.nateko.cp.cpauth.core.Crypto
import java.security.PrivateKey
import java.security.PublicKey

class CryptoTest extends AnyFunSuite {

	val privateKey: Try[PrivateKey] = {
		val keyBytes = CoreUtils.getResourceBytes("/private1.der")
		Crypto.privateFromDerBytes(keyBytes, "EC")
	}
	
	val publicKey: Try[PublicKey] = {
		val keyFileLines = CoreUtils.getResourceLines("/public1.pem")
		Crypto.publicFromPemLines(keyFileLines.toIndexedSeq, "EC")
	}
	
	test("Public EC key creation from file works"){
		assert(publicKey.isSuccess)
	}
	
	test("Private EC key creation from file works"){
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
