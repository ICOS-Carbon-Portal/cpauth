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

object Main{

	implicit val system = ActorSystem("samltests")
	import system.dispatcher
	
	val idpUrl = "https://idp.lu.se/idp/shibboleth"

	val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
	
	def getIdpMeta: String = {
		val response: Future[String] = pipeline(Get(idpUrl)).map(_.entity.asString)
		Await.result(response, 3.seconds)
	}
	
	val metaProvider = new HTTPMetadataProvider(new Timer(), new HttpClient(), idpUrl)

	
	
	// Get the builder factory
	val builderFactory = Configuration.getSAML2ArtifactBuilderFactory()
 
	// Get the assertion builder based on the assertion element name
	//val builder = builderFactory.getArtifactBuilder(Assertion.DEFAULT_ELEMENT_NAME).asInstanceOf[SAMLObjectBuilder[Assertion]]
 
	// Create the assertion
	//val assertion = builder.buildObject()
}
