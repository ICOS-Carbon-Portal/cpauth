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
import se.lu.nateko.cp.cpauth.services.CookieFactory
import se.lu.nateko.cp.cpauth.utils.Utils
import spray.json.JsBoolean
import se.lu.nateko.cp.cpauth.core.AuthSource
import se.lu.nateko.cp.cpauth.services.EmailSender
import se.lu.nateko.cp.cpauth.services.PasswordLifecycleHandler

trait PasswordRouting extends CpauthDirectives {

	def cookieFactory: CookieFactory
	def passwordHandler: PasswordLifecycleHandler

	lazy val passwordRoute: Route = pathPrefix("password"){
		get{
			path("accountslist"){
				admin(onSuccess(userDb.listUsers) {users => complete(users)})
			} ~
			path("initpassreset" / Remaining){token =>
				setCookie(cookieFactory.makeAuthCookie(token)){
					redirect("/passwordreset/", StatusCodes.Found)
				}
			}
		} ~
		post{
			path("login"){
				formFields('mail, 'password){(mail, password) =>
					val uEntryFuture = passwordHandler.authUser(UserId(mail), password)
					onSuccess(uEntryFuture){ uEntry =>

						cookieFactory.makeTokenBase64(uEntry.id, AuthSource.Password) match{
							case Success(token) =>
								val cookie = cookieFactory.makeAuthCookie(token)
								setCookie(cookie)(complete(StatusCodes.OK))
							case Failure(err) => failWith(err)
						}
					}
				}
			} ~
			path("changepassword"){
				user(uid =>
					formFields('oldPass, 'newPass)((oldPass, newPass) => {
						val result = passwordHandler.changePassword(uid, oldPass, newPass)
						onSuccess(result)(complete(StatusCodes.OK))
					})
				)
			} ~
			path("setpassword"){
				token{ authToken =>
					if(authToken.source == AuthSource.PasswordReset){
						formFields('newPass){newPass =>
							val done = passwordHandler.setPassword(authToken.userId, newPass)
							onSuccess(done)(complete(StatusCodes.OK))
						}
					} else complete(StatusCodes.Forbidden)
				}
			} ~
			path("deleteownaccount"){
				user(uid =>
					onSuccess(userDb.dropUser(uid))(logout)
				)
			} ~
			path("initpassreset" / Segment){email =>
				onSuccess(passwordHandler.sendResetEmail(UserId(email))){
					complete(StatusCodes.OK)
				}
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
