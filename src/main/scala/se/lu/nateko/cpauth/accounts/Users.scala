package se.lu.nateko.cpauth.accounts

import java.security.MessageDigest
import slick.driver.HsqldbDriver.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import se.lu.nateko.cpauth.core.UserInfo
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Base64
import scala.util.control.NoStackTrace

object Users {

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

	def closeDb(): Unit = db.close()

	val users = TableQuery[UserAccounts]

	def setup(): Unit = Await.result(db.run(users.schema.create), Duration.Inf)
	def drop(): Unit = Await.result(db.run(users.schema.drop), Duration.Inf)

	def addUser(uinfo: UserInfo, password: String): Future[Unit] = {
		val passHash = hash(uinfo.mail, password)

		val action = users. += ((0, uinfo.givenName, uinfo.surname, uinfo.mail, passHash))

		db.run(action).flatMap{ x =>
			if(x == 1) Future.successful(())
			else Future.failed(new Exception("Was supposed to add one user, added " + x))
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
				failedFuture("Incorrect user name or password")
			case (givenName, surname) :: Nil =>
				Future.successful(UserInfo(givenName = givenName, surname = surname, mail = mail))
			case _ =>
				failedFuture("Inconsistent database state: duplicate user ")
		})
	}

	private def failedFuture[T](msg: String): Future[T] = Future.failed(new Exception(msg) with NoStackTrace)

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

}