package se.lu.nateko.cpauth

import org.opensaml.xml.XMLObject
import scala.collection.JavaConverters.asScalaBufferConverter

object Utils {
  
  val domSerializer: org.w3c.dom.ls.LSSerializer = {
    import  org.w3c.dom.bootstrap.DOMImplementationRegistry
    import  org.w3c.dom.ls.DOMImplementationLS
    
    val registry = DOMImplementationRegistry.newInstance()
    
    val domImpl = registry.getDOMImplementation("LS").asInstanceOf[DOMImplementationLS]
    domImpl.createLSSerializer()
  }
  
  def xmlToStr(xml: org.w3c.dom.Element): String = domSerializer.writeToString(xml)
  
  def getResourceBytes(resourcePath: String): Array[Byte] = {
    import java.nio.file.{Files, Paths}
    Files.readAllBytes(Paths.get(getClass.getResource(resourcePath).toURI))
  }

  
  def extractClasses(xmlObj: XMLObject): Seq[Class[_]] = {
    if(xmlObj == null)
      Nil
    else if(xmlObj.hasChildren)
      xmlObj.getOrderedChildren.asScala.flatMap(extractClasses)
    else
      Seq(xmlObj.getClass)
  }
}