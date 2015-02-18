package se.lu.nateko.cpauth.core.test

import scala.util.Try

import org.scalatest.FunSuite

import se.lu.nateko.cpauth.core.CoreUtils
import se.lu.nateko.cpauth.core.Crypto
import Crypto._

class CryptoTest extends FunSuite {

	test("Public key extraction from X509 base64-encoded cert string works"){
		val certStr: String = CoreUtils.getResourceLines("/certX509base64.txt").map(_.trim).mkString("")
		val key = publicKeyFromX509Cert(certStr).get
		
		assert(key.getAlgorithm === "RSA")
	}
	
	test("Signing a simple message succeeds"){
		val keyBytes = CoreUtils.getResourceBytes("/private1.der")
		
		val signature = rsaPrivateFromDerBytes(keyBytes).map(signMessage("blabla", _))
		assert(signature.isSuccess)
	}
	
	test("Public RSA key creation from file works"){
		val keyFileLines = CoreUtils.getResourceLines("/public1.pem")
		
		val key = rsaPublicFromPemLines(keyFileLines.toIndexedSeq)
		assert(key.isSuccess)
	}
	
	test("Message signature gets verified successfully"){
		val msg = "qweqwe"
		val keyBytes = CoreUtils.getResourceBytes("/private1.der")
		val keyFileLines = CoreUtils.getResourceLines("/public1.pem")
		
		val signatureTry = rsaPrivateFromDerBytes(keyBytes).map(signMessage(msg, _))
		val pubKeyTry = rsaPublicFromPemLines(keyFileLines.toIndexedSeq)
		
		val verifyTry: Try[Boolean] = for(
			signature <- signatureTry;
			pubKey <- pubKeyTry
		) yield verifySignature(msg, pubKey, signature)
		
		assert(verifyTry.isSuccess && verifyTry.get)
	}
	
}