package se.lu.nateko.cp.cpauth.opensaml

import se.lu.nateko.cp.cpauth.Utils

object OpenSamlUtils {

	val bootstrapOpenSaml: () => Unit = {
		val rootLoggingLevel = Utils.getRootLoggingLevel
		
		Utils.setRootLoggingLevelToInfo()
		
		org.opensaml.DefaultBootstrap.bootstrap()
		
		Utils.setRootLoggingLevel(rootLoggingLevel)
		
		() => ()
	}

	private[this] val domSerializer: org.w3c.dom.ls.LSSerializer = {
		import  org.w3c.dom.bootstrap.DOMImplementationRegistry
		import  org.w3c.dom.ls.DOMImplementationLS
		
		val registry = DOMImplementationRegistry.newInstance()
		
		val domImpl = registry.getDOMImplementation("LS").asInstanceOf[DOMImplementationLS]
		domImpl.createLSSerializer()
	}

	def xmlToStr(xml: org.w3c.dom.Element): String = domSerializer.writeToString(xml)

}