package se.lu.nateko.cp.cpauth

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.opensaml.saml2.core.Response

import akka.http.scaladsl.model.headers.HttpCookie
import se.lu.nateko.cp.cpauth.core.CookieToToken
import se.lu.nateko.cp.cpauth.core.Exceptions
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.cpauth.opensaml.AllStatements
import se.lu.nateko.cp.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cp.cpauth.opensaml.AssertionValidator
import se.lu.nateko.cp.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cp.cpauth.opensaml.OpenSamlUtils
import se.lu.nateko.cp.cpauth.opensaml.ResponseStatusController
import se.lu.nateko.cp.cpauth.opensaml.StatementExtractor
import se.lu.nateko.cp.cpauth.opensaml.ValidatedAssertion

class CookieFactory(config: CpauthConfig) {
	
	private[this] val tokenMakerTry = SignedTokenMaker(config.auth.priv)

	def getLastIdpCookie(idpId: String): HttpCookie = HttpCookie(
		name = config.saml.idpCookieName,
		value = idpId,
		secure = false,
		domain = Some(config.http.serviceHost),
		path = Some(config.http.loginPath),
		httpOnly = false, //needs to be accessed by Javascript on the client
		maxAge = Some(31536000) //1 year in seconds
	)

	def makeAuthenticationCookie(response: Response, extractor: AssertionExtractor, idpLib: IdpLibrary): Try[HttpCookie] = for(
		goodResponse <- ResponseStatusController.ensureSuccess(response);
		validator <- AssertionValidator(goodResponse, idpLib);
		assertions = extractor.extractAssertions(goodResponse).map(validator.validate);
		statements = StatementExtractor.extractAttributeStringValues(assertions);
		userInfoTry = getUserInfo(statements);
		userInfo <- provideDebug(userInfoTry, assertions);
		cookie <- makeAuthenticationCookie(userInfo)
	) yield cookie

	def makeAuthenticationCookie(userInfo: UserId): Try[HttpCookie] = for(
		tokenMaker <- tokenMakerTry;
		token = tokenMaker.makeToken(userInfo)
	)yield HttpCookie(
		name = config.auth.pub.authCookieName,
		value = CookieToToken.constructCookieContent(token),
		domain = Some(config.http.authDomain),
		path = Some("/"),
		secure = true,
		httpOnly = true
	)


	def getUserInfo(statements: AllStatements): Try[UserId] = {
		val attrs = config.saml.attributes
		for(
			mail <- statements.getSingleValue(attrs.mail)
		) yield UserId(email = mail)
	}

	private def provideDebug(uinfoTry: Try[UserId], assertions: => Iterable[ValidatedAssertion]): Try[UserId] = uinfoTry match {
		case ok: Success[UserId] => ok
		case Failure(err) => Exceptions.failure{
			val assertionsAsString = assertions.map(_.assertion.getDOM).map(OpenSamlUtils.xmlToStr).mkString("\n")
			err.getMessage + "\nReturned assertions were:\n" + assertionsAsString
		}
	}
}
