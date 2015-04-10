package se.lu.nateko.cp.cpauth.accounts

import java.security.MessageDigest
import slick.driver.HsqldbDriver.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import se.lu.nateko.cp.cpauth.core.UserInfo
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Base64
import scala.util.control.NoStackTrace
import se.lu.nateko.cp.cpauth.core.AuthenticationFailedException
import se.lu.nateko.cp.cpauth.core.Exceptions

trait UsersIo{
	def addUser(uinfo: UserInfo, password: String, isAdmin: Boolean): Future[Unit]
	def userExists(mail: String): Future[Boolean]
	def authenticateUser(mail: String, password: String): Future[UserInfo]
	def dropUser(mail: String): Future[Int]
	def updateUser(oldMail: String, uinfo: UserInfo, newPass: String): Future[Int]
	def listUsers: Future[Seq[UserInfo]]
	def userIsAdmin(mail: String): Future[Boolean]
}

object Users extends UsersIo {

	def hash(mail: String, pass: String): String = {
		val md = MessageDigest.getInstance("MD5")
		md.update(mail.getBytes("UTF-8"))
		val salt: Array[Byte] = md.digest
		val hmd = MessageDigest.getInstance("SHA-256")
		hmd.update( salt ++ pass.getBytes("UTF-8"))
		val hashBytes = hmd.digest
		Base64.getEncoder.encodeToString(hashBytes)
	}

	private[this] val db = Database.forConfig("cpauth.hsqldb")

	def closeDb(): Unit = {
		println("Shutting down Users database")
		db.close()
	}

	val users = TableQuery[UserAccounts]

	def setup(): Unit = Await.result(db.run(users.schema.create), Duration.Inf)
	def drop(): Unit = Await.result(db.run(users.schema.drop), Duration.Inf)

	def addUser(uinfo: UserInfo, password: String, isAdmin: Boolean): Future[Unit] = {
		val passHash = hash(uinfo.mail, password)

		val action = users. += ((0, uinfo.givenName, uinfo.surname, uinfo.mail, passHash, isAdmin))

		db.run(action).flatMap{ x =>
			if(x == 1) Future.successful(())
			else Exceptions.failedFuture("Was supposed to add one user, added " + x)
		}
	}

	def userExists(mail: String): Future[Boolean] = {
		val user = users.filter(_.mail === mail).exists
		db.run(user.result)
	}

	def authenticateUser(mail: String, password: String): Future[UserInfo] = {
		val passHash = hash(mail, password)

		val userQ = for(
			user <- users if user.mail === mail && user.password === passHash
		) yield (user.givenName, user.surname)

		db.run(userQ.result).flatMap(_.toList match{
			case Nil =>
				Future.failed(AuthenticationFailedException)
			case (givenName, surname) :: Nil =>
				Future.successful(UserInfo(givenName = givenName, surname = surname, mail = mail))
			case _ =>
				Exceptions.failedFuture("Inconsistent database state: duplicate user ")
		})
	}

	def dropUser(mail: String): Future[Int] = {
		val action = users.filter(_.mail === mail).delete
		db.run(action)
	}

	def updateUser(oldMail: String, uinfo: UserInfo, newPass: String): Future[Int] = {
		val newPassHash = hash(uinfo.mail, newPass)

		val q = for(user <- users if user.mail === oldMail)
			yield (user.givenName, user.surname, user.mail, user.password)

		val upd = q.update((uinfo.givenName, uinfo.surname, uinfo.mail, newPassHash))

		db.run(upd)
	}
	
	def listUsers: Future[Seq[UserInfo]] = {
		val q = for(user <- users) yield
			(user.givenName, user.surname, user.mail)
		
		db.run(q.result).map(_.map({
			case (givenName, surname, mail) => UserInfo(givenName, surname, mail)
		}))
	}
	
	def userIsAdmin(mail: String): Future[Boolean] = {
		val q = users.filter(user => user.mail === mail && user.isAdmin).exists
		db.run(q.result)
	}

}