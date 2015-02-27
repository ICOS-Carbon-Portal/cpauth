package se.lu.nateko.cpauth.core.test

import org.scalatest.FunSpec
import se.lu.nateko.cpauth.core.CoreUtils

class CoreUtilsTest extends FunSpec {

	describe("getResourceBytes"){
		it("gives an empty array from non-existent file"){
			val array: Array[Byte] = CoreUtils.getResourceBytes("/anything/nonexistent.will.do")
			assert(array != null)
			assert(array.length === 0)
		}
	}

	describe("compression/decompression utility methods"){
		it("round trip works"){
			val data = "aaaaaaaaabbbbbbbb"
			val encoded = CoreUtils.compressAndBase64(data)
			
			val decoded = CoreUtils.decompressFromBase64(encoded)
			
			assert(decoded === data)
		}
	}

}