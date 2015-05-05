package se.lu.nateko.cp.cpauth

import scala.util.Try
import org.opensaml.saml2.core.Response
import se.lu.nateko.cp.cpauth.core.CookieToToken
import se.lu.nateko.cp.cpauth.core.AuthConfig
import se.lu.nateko.cp.cpauth.core.SamlConfig
import se.lu.nateko.cp.cpauth.core.UrlsConfig
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
		userInfoTry = getUserInfo(statements);
		userInfo <- provideDebug(userInfoTry, goodResponse);
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

	private def provideDebug(uinfoTry: Try[UserInfo], response: Response): Try[UserInfo] = uinfoTry match {
		case ok: Success[UserInfo] => ok
		case Failure(err) => Exceptions.failure{
			val responseAsString = OpenSamlUtils.xmlToStr(response.getDOM)
			err.getMessage + "\nSAML response was:\n" + responseAsString
		}
	}
}