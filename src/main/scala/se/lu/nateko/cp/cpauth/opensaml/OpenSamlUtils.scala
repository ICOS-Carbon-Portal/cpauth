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
	
}