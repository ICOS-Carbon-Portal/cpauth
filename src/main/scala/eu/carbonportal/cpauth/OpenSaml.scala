package se.lu.nateko.samltest

import org.opensaml.Configuration
import org.opensaml.common.SAMLObjectBuilder
import org.opensaml.saml2.core.Assertion
import org.opensaml.xml.XMLObjectBuilderFactory
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider
import java.util.Timer
import org.apache.commons.httpclient.HttpClient
import akka.actor.ActorSystem
import spray.http._
import spray.client.pipelining._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import org.opensaml.xml.parse.StaticBasicParserPool
import org.opensaml.DefaultBootstrap
import org.opensaml.xml.XMLObject

object Main{

	def setLoggingLevel(): Unit = {
		import org.slf4j.LoggerFactory;
		import ch.qos.logback.classic.{Level, Logger};

		LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
			.asInstanceOf[Logger]
			.setLevel(Level.INFO);
	}

	setLoggingLevel()

	val idpUrl = "https://idp.lu.se/idp/shibboleth"
	DefaultBootstrap.bootstrap()

//	implicit val system = ActorSystem("samltests")
//	import system.dispatcher
//
//	val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//	
//	def getIdpMeta: String = {
//		val response: Future[String] = pipeline(Get(idpUrl)).map(_.entity.asString)
//		Await.result(response, 3.seconds)
//	}
	
	def getIdpMeta: XMLObject = {
		val metaProvider = new HTTPMetadataProvider(new Timer(), new HttpClient(), idpUrl)
		val parserPool = new StaticBasicParserPool()
		parserPool.initialize()
		metaProvider.setParserPool(parserPool)
		metaProvider.initialize()
		metaProvider.getMetadata
	}	
	// Get the builder factory
	//val builderFactory = Configuration.getSAML2ArtifactBuilderFactory()
 
	// Get the assertion builder based on the assertion element name
	//val builder = builderFactory.getArtifactBuilder(Assertion.DEFAULT_ELEMENT_NAME).asInstanceOf[SAMLObjectBuilder[Assertion]]
 
	// Create the assertion
	//val assertion = builder.buildObject()
}
