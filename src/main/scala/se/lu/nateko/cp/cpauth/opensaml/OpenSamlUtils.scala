package se.lu.nateko.cp.cpauth.opensaml

import se.lu.nateko.cp.cpauth.utils.Utils
import org.opensaml.core.config.InitializationService
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

object OpenSamlUtils {

	val bootstrapOpenSaml: () => Unit = {
		val rootLoggingLevel = Utils.getRootLoggingLevel
		
		Utils.setRootLoggingLevelToInfo()

		Security.addProvider(new BouncyCastleProvider())

		InitializationService.initialize()
		
		Utils.setRootLoggingLevel(rootLoggingLevel)
		
		() => ()
	}

	private val domSerializer: org.w3c.dom.ls.LSSerializer = {
		import  org.w3c.dom.bootstrap.DOMImplementationRegistry
		import  org.w3c.dom.ls.DOMImplementationLS
		
		val registry = DOMImplementationRegistry.newInstance()
		
		val domImpl = registry.getDOMImplementation("LS").asInstanceOf[DOMImplementationLS]
		domImpl.createLSSerializer()
	}

	def xmlToStr(xml: org.w3c.dom.Element): String = domSerializer.writeToString(xml)

}