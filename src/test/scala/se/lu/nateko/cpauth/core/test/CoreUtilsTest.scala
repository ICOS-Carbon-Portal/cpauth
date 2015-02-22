package se.lu.nateko.cpauth.core.test

import org.scalatest.FunSuite
import se.lu.nateko.cpauth.core.CoreUtils

class CoreUtilsTest extends FunSuite {

	test("getResourceBytes from non-existent file gives an empty array"){
		val array: Array[Byte] = CoreUtils.getResourceBytes("/anything/nonexistent.will.do")
		assert(array != null)
		assert(array.length === 0)
	}

}