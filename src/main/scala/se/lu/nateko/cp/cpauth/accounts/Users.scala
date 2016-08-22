package se.lu.nateko.cp.cpauth.accounts

import java.security.MessageDigest
import slick.driver.HsqldbDriver.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import se.lu.nateko.cp.cpauth.core.UserId
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Base64
import scala.util.control.NoStackTrace
import se.lu.nateko.cp.cpauth.core.AuthenticationFailedException
import se.lu.nateko.cp.cpauth.core.Exceptions

trait UsersIo{
	def addUser(userEntry: UserEntry, password: String): Future[Unit]
	def userExists(uid: UserId): Future[Boolean]
	def authenticateUser(mail: String, password: String): Future[UserEntry]
	def dropUser(mail: String): Future[Unit]
	def updateUser(oldMail: String, userEntry: UserEntry, newPass: String): Future[Unit]
	def listUsers: Future[Seq[UserEntry]]
	def listUsersOld: Future[Seq[(UserId, String, String)]]
	def userIsAdmin(uid: UserId): Future[Boolean]
	def setAdminRights(mail: String, isAdmin: Boolean): Future[Unit]
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
		//println("Shutting down Users database")
		db.close()
	}

	val users = TableQuery[UserAccounts]

	def setup(): Unit = Await.result(db.run(users.schema.create), Duration.Inf)
	def drop(): Unit = Await.result(db.run(users.schema.drop), Duration.Inf)

	def addUser(userEntry: UserEntry, password: String): Future[Unit] = {
		val uinfo = userEntry.id
		val passHash = hash(uinfo.email, password)

		val action = users. += ((0, "", "", uinfo.email, passHash, userEntry.isAdmin))

		db.run(action).flatMap{ x =>
			if(x == 1) Future.successful(())
			else Exceptions.failedFuture("Was supposed to add one user, added " + x)
		}
	}

	def userExists(uid: UserId): Future[Boolean] = {
		val user = users.filter(_.mail === uid.email).exists
		db.run(user.result)
	}

	def authenticateUser(mail: String, password: String): Future[UserEntry] = {
		val passHash = hash(mail, password)

		val userQ = for(
			user <- users if user.mail === mail && user.password === passHash
		) yield (user.givenName, user.surname, user.isAdmin)

		db.run(userQ.result).flatMap(_.toList match{
			case Nil =>
				Future.failed(AuthenticationFailedException)
			case (givenName, surname, admin) :: Nil =>
				Future.successful(UserEntry(UserId(mail), admin))
			case _ =>
				Exceptions.failedFuture("Inconsistent database state: duplicate user ")
		})
	}

	def dropUser(mail: String): Future[Unit] = ensureSingleUserChange(mail){
		val action = users.filter(_.mail === mail).delete
		db.run(action)
	}

	def updateUser(oldMail: String, userEntry: UserEntry, newPass: String): Future[Unit] =
		ensureSingleUserChange(oldMail){
			val uinfo = userEntry.id
			val newPassHash = hash(uinfo.email, newPass)

			val q = for(user <- users if user.mail === oldMail)
				yield (user.givenName, user.surname, user.mail, user.password, user.isAdmin)

			val upd = q.update(("", "", uinfo.email, newPassHash, userEntry.isAdmin))

			db.run(upd)
		}

	def listUsers: Future[Seq[UserEntry]] = {
		val q = for(user <- users) yield (user.mail, user.isAdmin)

		db.run(q.result).map(_.map({
			case (mail, isAdmin) => UserEntry(UserId(mail), isAdmin)
		}))
	}

	def listUsersOld: Future[Seq[(UserId, String, String)]] = {
		val q = for(user <- users) yield
			(user.givenName, user.surname, user.mail)

		db.run(q.result).map(_.map({
			case (givenName, surname, mail) => (UserId(mail), givenName, surname)
		}))
	}

	def userIsAdmin(uid: UserId): Future[Boolean] = {
		val q = users.filter(user => user.mail === uid.email && user.isAdmin).exists
		db.run(q.result)
	}

	def setAdminRights(mail: String, isAdmin: Boolean): Future[Unit] =
		ensureSingleUserChange(mail){
			val q = for(user <- users if user.mail === mail) yield user.isAdmin
			db.run(q.update(isAdmin))
		}

	private def ensureSingleUserChange(mail: String)(future: Future[Int]): Future[Unit] = future.flatMap{
		case 0 => Exceptions.failedFuture(s"Operation failed. Does user '$mail' exist at all?")
		case 1 => Future.successful(())
		case _ => Exceptions.failedFuture("Unexpected error! Number of updated users was neither 0 nor 1!")
	}

}
