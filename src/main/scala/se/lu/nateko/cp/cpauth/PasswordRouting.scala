package se.lu.nateko.cp.cpauth

import spray.routing.Directives
import spray.routing.Route
import se.lu.nateko.cp.cpauth.accounts.UsersIo
import scala.util.Success
import scala.util.Failure
import spray.http.StatusCodes
import se.lu.nateko.cp.cpauth.core.AuthenticationFailedException
import se.lu.nateko.cp.cpauth.core.UserInfo
import CpauthJsonProtocol._
import scala.concurrent.Future
import se.lu.nateko.cp.cpauth.accounts.UserEntry
import scala.concurrent.duration._

trait PasswordRouting extends Directives with CpauthDirectives {

	def userDb: UsersIo
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
						isLocal => complete(primitiveToJson(isLocal))
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
						onSuccess(result)(_ => complete(StatusCodes.OK))
					})
				)
			} ~
			path("deleteownaccount"){
				user(uinfo =>
					onSuccess(userDb.dropUser(uinfo.mail))(_ => logout)
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
								onSuccess(userDb.addUser(userEntry, password)){ _ =>
									complete(StatusCodes.OK)
								}
						}
					)
				} ~
				path("deleteaccount"){
					formField('mail)(mail =>
						onSuccess(userDb.setAdminRights(mail, true))(_ => complete(StatusCodes.OK))
					)
				} ~
				path("makeadmin"){
					formField('mail)(mail =>
						onSuccess(userDb.setAdminRights(mail, true))(_ => complete(StatusCodes.OK))
					)
				} ~
				path("unmakeadmin"){
					formField('mail)(mail =>
						onSuccess(userDb.setAdminRights(mail, false))(_ => complete(StatusCodes.OK))
					)
				}
			}
		}
	}

	private def admin(inner: => Route): Route = user(uinfo =>
		onComplete(userDb.userIsAdmin(uinfo.mail)){
			case Failure(err) => failWith(err)
			case Success(false) => complete((StatusCodes.Forbidden, "Need to be logged in as CPauth admin"))
			case Success(true) => inner
		}
	)
	
}
