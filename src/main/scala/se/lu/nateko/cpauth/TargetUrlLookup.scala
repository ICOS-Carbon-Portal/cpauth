package se.lu.nateko.cpauth

import spray.http.Uri
import scala.collection.concurrent

trait TargetUrlLookup {
	def memorize(requestId: String, uri: Uri): Unit
	def getAndForget(requestId: String): Option[Uri]
}

/**
 * TODO Add a job for scheduled garbage cleanups to avoid potential memory leaks
 */
class MapBasedUrlLookup extends TargetUrlLookup {
	import scala.collection.JavaConversions._

	private[this] val map: concurrent.Map[String, Uri] = new java.util.concurrent.ConcurrentHashMap[String, Uri]

	def memorize(requestId: String, uri: Uri): Unit = map.put(requestId, uri)

	def getAndForget(requestId: String): Option[Uri] = map.remove(requestId)

}