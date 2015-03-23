package se.lu.nateko.cpauth.accounts

import slick.driver.HsqldbDriver.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import se.lu.nateko.cpauth.core.UserInfo
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Users {

	private[this] val db = Database.forConfig("hsqldb")

	def closeDb(): Unit = db.close()

	val users = TableQuery[UserAccounts]

	def setup(): Unit = Await.result(db.run(users.schema.create), Duration.Inf)
	def drop(): Unit = Await.result(db.run(users.schema.drop), Duration.Inf)

	def addUser(uinfo: UserInfo, password: String): Future[Unit] = {
		val action = users. += ((0, uinfo.givenName, uinfo.surname, uinfo.mail, password))
		db.run(action).flatMap{ x =>
			if(x == 1) Future.successful(())
			else Future.failed(new Exception("Was supposed to add one user, added " + x))
		}
	}
}