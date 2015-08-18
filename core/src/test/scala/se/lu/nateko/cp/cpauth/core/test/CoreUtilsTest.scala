package se.lu.nateko.cp.cpauth.core.test

import org.scalatest.FunSpec
import se.lu.nateko.cp.cpauth.core.CoreUtils

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
			val data: Array[Byte] = Array(0, 0, 0, 0, 1, 1, 2, 2, 2)
			val encoded = CoreUtils.compress(data)
			
			val decoded = CoreUtils.decompress(encoded)
			
			assert(decoded === data)
		}
	}

}