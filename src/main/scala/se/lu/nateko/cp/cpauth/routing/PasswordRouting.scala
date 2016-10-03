package se.lu.nateko.cp.cpauth.routing

import scala.util.Success
import scala.util.Failure
import se.lu.nateko.cp.cpauth.core.UserId
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
import se.lu.nateko.cp.cpauth.core.AuthSource

trait PasswordRouting extends CpauthDirectives {

	def cookieFactory: CookieFactory

	private def authUser(uid: UserId, password: String): Future[UserEntry] =
		Utils.slowFailureDown(userDb.authenticateUser(uid, password), 500 millis)

	lazy val passwordRoute: Route = pathPrefix("password"){
		get{
			path("accountslist"){
				admin(onSuccess(userDb.listUsers) {users => complete(users)})
			}
		} ~
		post{
			path("login"){
				formFields('mail, 'password)((mail, password) =>

					onSuccess(authUser(UserId(mail), password)){ uEntry =>

						cookieFactory.makeAuthenticationCookie(uEntry.id, AuthSource.Password) match{
							case Success(cookie) => setCookie(cookie)(complete(StatusCodes.OK))
							case Failure(err) => failWith(err)
						}
					}
				)
			} ~
			path("changepassword"){
				user(uid =>
					formFields('oldPass, 'newPass)((oldPass, newPass) => {
						val result = for(
							userEntry <- authUser(uid, oldPass);
							_ <- userDb.updateUser(uid, userEntry, newPass)
						) yield ()
						onSuccess(result)(complete(StatusCodes.OK))
					})
				)
			} ~
			path("deleteownaccount"){
				user(uid =>
					onSuccess(userDb.dropUser(uid))(logout)
				)
			} ~
			admin{
				path("createaccount"){
					formFields('mail, 'password){(mail, password) =>
						val uid = UserId(mail)
						onSuccess(userDb.userExists(uid)) {
							case true => complete((StatusCodes.Forbidden, "User already exists"))
							case false =>
								val userEntry = UserEntry(uid, false)
								onSuccess(userDb.addUser(userEntry, password)){
									complete(StatusCodes.OK)
								}
						}
					}
				} ~
				path("deleteaccount"){
					formField('mail)(mail =>
						onSuccess(userDb.dropUser(UserId(mail)))(complete(StatusCodes.OK))
					)
				} ~
				path("makeadmin"){
					formField('mail)(mail =>
						onSuccess(userDb.setAdminRights(UserId(mail), true))(complete(StatusCodes.OK))
					)
				} ~
				path("unmakeadmin"){
					formField('mail)(mail =>
						onSuccess(userDb.setAdminRights(UserId(mail), false))(complete(StatusCodes.OK))
					)
				}
			}
		}
	}

}
