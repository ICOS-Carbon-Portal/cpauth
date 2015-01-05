package se.lu.nateko.cpauth.opensaml

import se.lu.nateko.cpauth.Utils

object OpenSamlUtils {

	val bootstrapOpenSaml: () => Unit = {
		val rootLoggingLevel = Utils.getRootLoggingLevel
		
		Utils.setRootLoggingLevelToInfo()
		
		org.opensaml.DefaultBootstrap.bootstrap()
		
		Utils.setRootLoggingLevel(rootLoggingLevel)
		
		() => ()
	}
	
}