package se.lu.nateko.cp.cpauth.utils

import se.lu.nateko.cp.cpauth.core.Crypto

import java.security.SecureRandom
import java.util.Base64

/** Helpers for the OIDC Authorization Code flow with PKCE (RFC 7636). */
object Oidc:

	private val random = new SecureRandom
	private val b64url = Base64.getUrlEncoder.withoutPadding

	/**
	 * Cryptographically random, URL-safe token. Used for `state`, `nonce` and the PKCE
	 * `code_verifier`. 32 bytes gives 43 base64url characters, satisfying RFC 7636's
	 * 43..128 character requirement for code verifiers.
	 */
	def randomToken(nBytes: Int = 32): String =
		val bytes = new Array[Byte](nBytes)
		random.nextBytes(bytes)
		b64url.encodeToString(bytes)

	/** PKCE S256 challenge: base64url(SHA-256(ASCII(code_verifier))). */
	def codeChallengeS256(codeVerifier: String): String =
		b64url.encodeToString(Crypto.sha256sum(codeVerifier))

end Oidc
