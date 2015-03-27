package se.lu.nateko.cpauth.accounts

import slick.driver.HsqldbDriver.api._

class UserAccounts(tag: Tag) extends Table[(Int, String, String, String, String)](tag, "USERS"){
	def id = column[Int]("USER_ID", O.AutoInc)
	def mail = column[String]("MAIL", O.PrimaryKey)
	def givenName = column[String]("GIVENNAME")
	def surname = column[String]("SURNAME")
	def password = column[String]("PASSWORD")

	def * = (id, givenName, surname, mail, password)
}