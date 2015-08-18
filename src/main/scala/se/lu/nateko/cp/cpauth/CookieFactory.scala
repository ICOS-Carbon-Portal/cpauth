package se.lu.nateko.cp.cpauth

import scala.util.Try
import org.opensaml.saml2.core.Response
import se.lu.nateko.cp.cpauth.core.CookieToToken
import se.lu.nateko.cp.cpauth.core.UserInfo
import se.lu.nateko.cp.cpauth.opensaml.AllStatements
import se.lu.nateko.cp.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cp.cpauth.opensaml.AssertionValidator
import se.lu.nateko.cp.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cp.cpauth.opensaml.ResponseStatusController
import se.lu.nateko.cp.cpauth.opensaml.StatementExtractor
import spray.http.HttpCookie
import scala.util.Success
import scala.util.Failure
import se.lu.nateko.cp.cpauth.core.Exceptions
import se.lu.nateko.cp.cpauth.opensaml.OpenSamlUtils
import se.lu.nateko.cp.cpauth.opensaml.ValidatedAssertion

class CookieFactory(config: CpauthConfig) {
	
	private[this] val tokenMakerTry = SignedTokenMaker(config.auth.priv)

	def getLastIdpCookie(idpId: String): HttpCookie = HttpCookie(
		name = config.saml.idpCookieName,
		content = idpId,
		secure = false,
		domain = Some(config.http.serviceHost),
		path = Some(config.http.loginPath),
		httpOnly = false, //needs to be accessed by Javascript on the client
		maxAge = Some(31536000)
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

	def makeAuthenticationCookie(userInfo: UserInfo): Try[HttpCookie] = for(
		tokenMaker <- tokenMakerTry;
		token = tokenMaker.makeToken(userInfo)
	)yield HttpCookie(
		name = config.auth.pub.authCookieName,
		content = CookieToToken.constructCookieContent(token),
		domain = Some(config.http.authDomain),
		path = Some("/"),
		secure = true,
		httpOnly = true
	)


	def getUserInfo(statements: AllStatements): Try[UserInfo] = {
		val attrs = config.saml.attributes
		for(
			givenName <- statements.getSingleValue(attrs.givenName);
			surname <- statements.getSingleValue(attrs.surname);
			mail <- statements.getSingleValue(attrs.mail)
		) yield UserInfo(givenName = givenName, surname = surname, mail = mail)
	}

	private def provideDebug(uinfoTry: Try[UserInfo], assertions: => Iterable[ValidatedAssertion]): Try[UserInfo] = uinfoTry match {
		case ok: Success[UserInfo] => ok
		case Failure(err) => Exceptions.failure{
			val assertionsAsString = assertions.map(_.assertion.getDOM).map(OpenSamlUtils.xmlToStr).mkString("\n")
			err.getMessage + "\nReturned assertions were:\n" + assertionsAsString
		}
	}
}