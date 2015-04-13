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

trait PasswordRouting extends Directives with CpauthDirectives {

	
	def userDb: UsersIo
	def cookieFactory: CookieFactory

	lazy val passwordRoute: Route = pathPrefix("password"){
		get{
			path("account" / "list"){
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

					onSuccess(userDb.authenticateUser(mail, password)){ uEntry =>

						cookieFactory.makeAuthenticationCookie(uEntry.info) match{
							case Success(cookie) => setCookie(cookie)(complete(StatusCodes.OK))
							case Failure(err) => failWith(err)
						}
					}
				)
			} ~
			path("account" / "create"){
				admin{
					formFields('givenName, 'surname, 'mail, 'password)((givenName, surname, mail, password) =>
						onSuccess(userDb.userExists(mail)) {
							case true => complete((StatusCodes.Forbidden, "User already exists"))
							case false =>
								val uinfo = UserInfo(givenName, surname, mail)
								onSuccess(userDb.addUser(uinfo, password, false)){ _ =>
									complete(StatusCodes.OK)
								}
						}
					)
				}
			} ~
			path("changepassword"){
				user(uinfo =>
					formFields('oldPass, 'newPass)((oldPass, newPass) => {
						val result = for(
							userEntry <- userDb.authenticateUser(uinfo.mail, oldPass);
							_ <- userDb.updateUser(uinfo.mail, uinfo, newPass, userEntry.isAdmin)
						) yield ()
						onSuccess(result)(_ => complete(StatusCodes.OK))
					})
				)
			} ~
			path("deleteaccount"){
				user(uinfo =>
					onSuccess(userDb.dropUser(uinfo.mail))(_ => logout)
				)
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
