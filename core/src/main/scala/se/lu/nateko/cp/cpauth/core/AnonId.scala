package se.lu.nateko.cp.cpauth.core

opaque type AnonId <: String = String

object AnonId:
	def apply(uid: UserId, secretSalt: String): AnonId =
		CoreUtils.encodeToBase64String(Crypto.sha256sum(uid.email + secretSalt)).take(12)
