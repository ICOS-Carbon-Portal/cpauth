package se.lu.nateko.cp.cpauth.utils

import akka.actor.Scheduler
import akka.http.scaladsl.model.Uri

import java.time.Instant
import scala.collection.concurrent
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

/**
 * State an OIDC login needs to carry across the redirect to the OpenID Provider.
 * The 'state' parameter is the lookup key, which makes it a genuine CSRF binding:
 * a callback whose 'state' is not in the store is rejected.
 */
case class PendingOidcLogin(
	targetUrl: Option[Uri],
	nonce: String,
	codeVerifier: String,
	created: Instant
)

trait OidcLoginStore:
	def memorize(state: String, pending: PendingOidcLogin): Unit
	/** Single-use: a given 'state' can be redeemed at most once, so replays fail. */
	def getAndForget(state: String): Option[PendingOidcLogin]

/**
 * Unlike [[MapBasedUrlLookup]], entries are evicted after `ttl` so abandoned logins
 * cannot accumulate indefinitely.
 */
class MapBasedOidcLoginStore(ttl: FiniteDuration = 5.minutes) extends OidcLoginStore:
	import scala.jdk.CollectionConverters.*

	private val map: concurrent.Map[String, PendingOidcLogin] =
		new java.util.concurrent.ConcurrentHashMap[String, PendingOidcLogin].asScala

	def memorize(state: String, pending: PendingOidcLogin): Unit = map.put(state, pending)

	def getAndForget(state: String): Option[PendingOidcLogin] = map.remove(state).filter(isFresh)

	private def isFresh(pending: PendingOidcLogin): Boolean =
		pending.created.plusMillis(ttl.toMillis).isAfter(Instant.now)

	def evictExpired(): Unit = map.filterInPlace((_, pending) => isFresh(pending))

	/** Schedules periodic eviction. Returns a handle that can be cancelled on shutdown. */
	def scheduleEviction(using scheduler: Scheduler, exe: ExecutionContext) =
		scheduler.scheduleAtFixedRate(ttl, ttl)(() => evictExpired())

end MapBasedOidcLoginStore
