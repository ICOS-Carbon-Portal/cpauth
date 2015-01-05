package se.lu.nateko.cpauth.opensaml

object MetadataAnalyzer {

	OpenSamlUtils.bootstrapOpenSaml()

//	implicit val system = ActorSystem("samltests")
//	import system.dispatcher
//
//	val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
//	
//	def getIdpMeta: String = {
//		val response: Future[String] = pipeline(Get(idpUrl)).map(_.entity.asString)
//		Await.result(response, 3.seconds)
//	}
	
//	def getIdpMeta: MetadataProvider = {
//		val metaProvider = new HTTPMetadataProvider(new Timer(), new HttpClient(), idpUrl)
//		metaProvider.setParserPool(parserPool)
//		metaProvider.initialize()
//		metaProvider
//	}	
}