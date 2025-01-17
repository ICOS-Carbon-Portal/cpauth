package se.lu.nateko.cp.cpauth.services

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.opensaml.saml.saml2.core.Response
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
import se.lu.nateko.cp.cpauth.core.AuthSource
import se.lu.nateko.cp.cpauth.CpauthConfig
import se.lu.nateko.cp.cpauth.utils.SignedTokenMaker
import eu.icoscp.envri.Envri

class CookieFactory(config: CpauthConfig) {
	
	private def tokenMakerTry(using Envri) = SignedTokenMaker(config.auth.priv, "EC")

	def getLastIdpCookie(idpId: String)(implicit envri: Envri): HttpCookie = HttpCookie(
		name = config.saml.idpCookieName,
		value = idpId,
		secure = false,
		domain = Some(config.http.serviceHost(envri)),
		path = Some(config.http.loginPath),
		httpOnly = false, //needs to be accessed by Javascript on the client
		maxAge = Some(31536000) //1 year in seconds
	)


	def makeAuthenticationCookie(
		response: Response,
		extractor: AssertionExtractor,
		idpLib: IdpLibrary
	)(using Envri): Try[(HttpCookie, UserId, AllStatements)] = for(
		goodResponse <- ResponseStatusController.ensureSuccess(response);
		validator <- AssertionValidator(goodResponse, idpLib);
		assertions = extractor.extractAssertions(goodResponse).map(validator.validate(_, goodResponse));
		statements <- StatementExtractor.extractAttributeStringValues(assertions);
		userIdTry = getUserId(statements);
		userId <- provideDebug(userIdTry, assertions);
		tokenBase64 <- makeTokenBase64(userId, AuthSource.Saml);
		cookie = makeAuthCookie(tokenBase64)
	) yield (cookie, userId, statements)


	def makeTokenBase64(userId: UserId, source: AuthSource)(using Envri): Try[String] = for(
		tokenMaker <- tokenMakerTry;
		token = tokenMaker.makeToken(userId, source)
	) yield CookieToToken.constructCookieContent(token)


	def makeAuthCookie(tokenBase64: String)(using envri: Envri) = HttpCookie(
		name = config.auth.pub(envri).authCookieName,
		value = tokenBase64,
		domain = Some(config.auth.pub(envri).authCookieDomain),
		path = Some("/"),
		secure = true,
		httpOnly = true
	)


	def getUserId(statements: AllStatements): Try[UserId] = {
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
