package se.lu.nateko.cpauth.core.test

import org.scalatest.FunSuite
import scala.io.Source
import se.lu.nateko.cpauth.core.Crypto
import se.lu.nateko.cpauth.Utils
import scala.util.Try

class CryptoTest extends FunSuite {

	import Crypto._
	
	test("Signing a simple message succeeds"){
		val keyBytes = Utils.getResourceBytes("/private1.der")
		
		val signature = rsaPrivateFromDerBytes(keyBytes).map(signMessage("blabla", _))
		assert(signature.isSuccess)
	}
	
	test("Public RSA key creation from file works"){
		val keyFileLines = Utils.getResourceLines("/public1.pem")
		
		val key = rsaPublicFromPemLines(keyFileLines.toIndexedSeq)
		assert(key.isSuccess)
	}
	
	test("Message signature gets verified successfully"){
		val msg = "qweqwe"
		val keyBytes = Utils.getResourceBytes("/private1.der")
		val keyFileLines = Utils.getResourceLines("/public1.pem")
		
		val signatureTry = rsaPrivateFromDerBytes(keyBytes).map(signMessage(msg, _))
		val pubKeyTry = rsaPublicFromPemLines(keyFileLines.toIndexedSeq)
		
		val verifyTry: Try[Boolean] = for(
			signature <- signatureTry;
			pubKey <- pubKeyTry
		) yield verifySignature(msg, pubKey, signature)
		
		assert(verifyTry.isSuccess && verifyTry.get)
	}
	
}