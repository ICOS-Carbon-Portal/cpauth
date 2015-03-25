package se.lu.nateko.cpauth

import scala.util.Try
import org.opensaml.saml2.core.Response
import se.lu.nateko.cpauth.core.CookieToToken
import se.lu.nateko.cpauth.core.AuthConfig
import se.lu.nateko.cpauth.core.SamlConfig
import se.lu.nateko.cpauth.core.UrlsConfig
import se.lu.nateko.cpauth.core.UserInfo
import se.lu.nateko.cpauth.opensaml.AllStatements
import se.lu.nateko.cpauth.opensaml.AssertionExtractor
import se.lu.nateko.cpauth.opensaml.AssertionValidator
import se.lu.nateko.cpauth.opensaml.IdpLibrary
import se.lu.nateko.cpauth.opensaml.ResponseStatusController
import se.lu.nateko.cpauth.opensaml.StatementExtractor
import spray.http.HttpCookie

class CookieFactory(config: UrlsConfig with SamlConfig with AuthConfig) {
	
	private[this] val tokenMakerTry = SignedTokenMaker(config)

	def getLastIdpCookie(idpId: String): HttpCookie = HttpCookie(
		name = config.idpCookieName,
		content = idpId,
		secure = false,
		domain = Some(config.serviceHost),
		path = Some(config.loginPath),
		httpOnly = false, //needs to be accessed by Javascript on the client
		maxAge = Some(31536000)
	)

	def makeAuthenticationCookie(response: Response, extractor: AssertionExtractor, idpLib: IdpLibrary): Try[HttpCookie] = for(
		goodResponse <- ResponseStatusController.ensureSuccess(response);
		validator <- AssertionValidator(goodResponse, idpLib);
		assertions = extractor.extractAssertions(goodResponse).map(validator.validate);
		statements = StatementExtractor.extractAttributeStringValues(assertions);
		userInfo <- getUserInfo(statements);
		cookie <- makeAuthenticationCookie(userInfo)
	) yield cookie

	def makeAuthenticationCookie(userInfo: UserInfo): Try[HttpCookie] = for(
		tokenMaker <- tokenMakerTry;
		token = tokenMaker.makeToken(userInfo)
	)yield HttpCookie(
		name = config.authCookieName,
		content = CookieToToken.constructCookieContent(token),
		domain = Some(config.authDomain),
		path = Some("/"),
		secure = true,
		httpOnly = true
	)

	def getUserInfo(statements: AllStatements): Try[UserInfo] = for(
		givenName <- statements.getSingleValue(config.givenNameAttr);
		surname <- statements.getSingleValue(config.surnameAttr);
		mail <- statements.getSingleValue(config.mailAttr)
	) yield UserInfo(givenName = givenName, surname = surname, mail = mail)
}