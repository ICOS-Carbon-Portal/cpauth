package se.lu.nateko.cp.cpauth.routing

import scala.util.Success
import scala.util.Failure
import se.lu.nateko.cp.cpauth.core.UserInfo
import se.lu.nateko.cp.cpauth.CpauthJsonProtocol._
import scala.concurrent.Future
import se.lu.nateko.cp.cpauth.accounts.UserEntry
import scala.concurrent.duration._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import se.lu.nateko.cp.cpauth.CookieFactory
import se.lu.nateko.cp.cpauth.Utils
import spray.json.JsBoolean

trait PasswordRouting extends CpauthDirectives {

	def cookieFactory: CookieFactory

	private def authUser(mail: String, password: String): Future[UserEntry] =
		Utils.slowFailureDown(userDb.authenticateUser(mail, password), 500 millis)

	lazy val passwordRoute: Route = pathPrefix("password"){
		get{
			path("accountslist"){
				admin(onSuccess(userDb.listUsers) {users => complete(users)})
			} ~
			path("amilocal"){
				user{user =>
					onSuccess(userDb.userExists(user.mail)){
						isLocal => complete(JsBoolean(isLocal))
					}
				}
			}
		} ~
		post{
			path("login"){
				formFields('mail, 'password)((mail, password) =>

					onSuccess(authUser(mail, password)){ uEntry =>

						cookieFactory.makeAuthenticationCookie(uEntry.info) match{
							case Success(cookie) => setCookie(cookie)(complete(StatusCodes.OK))
							case Failure(err) => failWith(err)
						}
					}
				)
			} ~
			path("changepassword"){
				user(uinfo =>
					formFields('oldPass, 'newPass)((oldPass, newPass) => {
						val result = for(
							userEntry <- authUser(uinfo.mail, oldPass);
							_ <- userDb.updateUser(uinfo.mail, userEntry, newPass)
						) yield ()
						onSuccess(result)(complete(StatusCodes.OK))
					})
				)
			} ~
			path("deleteownaccount"){
				user(uinfo =>
					onSuccess(userDb.dropUser(uinfo.mail))(logout)
				)
			} ~
			admin{
				path("createaccount"){
					formFields('givenName, 'surname, 'mail, 'password)((givenName, surname, mail, password) =>
						onSuccess(userDb.userExists(mail)) {
							case true => complete((StatusCodes.Forbidden, "User already exists"))
							case false =>
								val uinfo = UserInfo(givenName, surname, mail)
								val userEntry = UserEntry(uinfo, false)
								onSuccess(userDb.addUser(userEntry, password)){
									complete(StatusCodes.OK)
								}
						}
					)
				} ~
				path("deleteaccount"){
					formField('mail)(mail =>
						onSuccess(userDb.dropUser(mail))(complete(StatusCodes.OK))
					)
				} ~
				path("makeadmin"){
					formField('mail)(mail =>
						onSuccess(userDb.setAdminRights(mail, true))(complete(StatusCodes.OK))
					)
				} ~
				path("unmakeadmin"){
					formField('mail)(mail =>
						onSuccess(userDb.setAdminRights(mail, false))(complete(StatusCodes.OK))
					)
				}
			}
		}
	}

}
