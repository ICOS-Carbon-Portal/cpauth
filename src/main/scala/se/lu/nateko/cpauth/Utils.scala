package se.lu.nateko.cpauth

import org.opensaml.xml.XMLObject
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.convert.WrapAsScala

object Utils {
  
  val domSerializer: org.w3c.dom.ls.LSSerializer = {
    import  org.w3c.dom.bootstrap.DOMImplementationRegistry
    import  org.w3c.dom.ls.DOMImplementationLS
    
    val registry = DOMImplementationRegistry.newInstance()
    
    val domImpl = registry.getDOMImplementation("LS").asInstanceOf[DOMImplementationLS]
    domImpl.createLSSerializer()
  }
  
  def xmlToStr(xml: org.w3c.dom.Element): String = domSerializer.writeToString(xml)
  
  def extractClasses(xmlObj: XMLObject): Seq[Class[_]] = {
    if(xmlObj == null)
      Nil
    else if(xmlObj.hasChildren)
      xmlObj.getOrderedChildren.asScala.flatMap(extractClasses)
    else
      Seq(xmlObj.getClass)
  }
  

	import org.slf4j.LoggerFactory
	import ch.qos.logback.classic.{Level, Logger}
	
	def setRootLoggingLevel(level: Level): Unit =
		LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
			.asInstanceOf[Logger]
			.setLevel(level)

	def setRootLoggingLevelToInfo(): Unit = setRootLoggingLevel(Level.INFO)

	def getRootLoggingLevel: Level =
		LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
			.asInstanceOf[Logger]
			.getLevel

	implicit class SafeJavaCollectionWrapper[T](val list: java.util.Collection[T]) extends AnyVal {

		def toSafeIterable: Iterable[T] =
			if(list == null)
				Iterable.empty[T]
			else WrapAsScala.iterableAsScalaIterable(list)
	}
}