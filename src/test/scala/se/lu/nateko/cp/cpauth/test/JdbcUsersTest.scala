package se.lu.nateko.cp.cpauth.test

import java.sql.DriverManager
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.ExecutionContext.Implicits.global
import se.lu.nateko.cp.cpauth.accounts._
import se.lu.nateko.cp.cpauth.core._
import se.lu.nateko.cp.cpauth.core.AuthenticationFailedException
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class JdbcUsersTest extends AnyFunSuite with ScalaFutures {

	def getConnection() = {
		Class.forName("org.hsqldb.jdbc.JDBCDataSource")
		DriverManager.getConnection("jdbc:hsqldb:mem:test")
	}

	test("Simple test of all operations") {
		val jdu = new se.lu.nateko.cp.cpauth.accounts.JdbcUsers(() => getConnection())
		Await.ready(jdu.init(), 2.seconds)
		val uid = UserId("ordinaryuser")
		val ue  = UserEntry(uid, false)

		// There are no users.
		assertResult(Seq())   { jdu.listUsers.futureValue }
		// There really isn't.
		assertResult(false)	  { jdu.userExists(uid).futureValue }
		// But it should be ok to add one.
		assertResult( () )	  { jdu.addUser(ue, "password").futureValue }
		// Once the user has been added there should be users to list.
		assertResult(Seq(ue)) { jdu.listUsers.futureValue }
		assertResult(true)	  { jdu.userExists(uid).futureValue }
		assertResult(false)   { jdu.userIsAdmin(uid).futureValue }
		// And to authenticate
		assertResult(ue)	  { jdu.authenticateUser(uid, "password").futureValue }
		// But only using the correct password.
		val a = jdu.authenticateUser(uid, "wrong!")
		assert(a.failed.futureValue.isInstanceOf[AuthenticationFailedException.type])

		// Once we drop the only user, it's should be gone.
		jdu.dropUser(uid).futureValue
		assertResult(false)   { jdu.userExists(uid).futureValue }
		assertResult(Seq())   { jdu.listUsers.futureValue }

		// Of course, with no users, there is no user to authenticate.
		val b = jdu.authenticateUser(uid, "password")
		assert(b.failed.futureValue.isInstanceOf[AuthenticationFailedException.type])

		// Let's set up another user
		val aid = UserId("admin")
		val aue = UserEntry(aid, true)
		// And then add both users
		assertResult( () )	  { jdu.addUser(ue, "password").futureValue }
		assertResult( () )	  { jdu.addUser(aue, "password").futureValue }
		// Now there should be two users
		assertResult( 2 ) { jdu.listUsers.futureValue.length }
		// And one of them is admin
		assertResult(true)    { jdu.userIsAdmin(aid).futureValue }
		// Remove admin rights
		jdu.setAdminRights(aid, false).futureValue
		assertResult(false)    { jdu.userIsAdmin(aid).futureValue }
		// And add them back again
		jdu.setAdminRights(aid, true).futureValue
		assertResult(true)    { jdu.userIsAdmin(aid).futureValue }

		// You can make nonexisting users admin.
		val c = UserId("nosuchuser")
		jdu.setAdminRights(c, true).futureValue
		// But it doesn't let them authenticate
		assertResult(false)    { jdu.userIsAdmin(c).futureValue }

		// Check password
		assertResult(ue) { jdu.authenticateUser(uid, "password").futureValue }
		// Then change password
		jdu.updateUser(uid, ue, "newpassword").futureValue
		// The old password should no longer authenticate
		val d = jdu.authenticateUser(uid, "password")
		assert(d.failed.futureValue.isInstanceOf[AuthenticationFailedException.type])
		// But the new one should
		assertResult(ue) { jdu.authenticateUser(uid, "newpassword").futureValue }

	}
}
