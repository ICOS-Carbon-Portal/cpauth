package se.lu.nateko.cp.cpauth.oauth

import scala.concurrent.Future
import scala.util.Try

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import se.lu.nateko.cp.cpauth.OAuthProviderConfig
import se.lu.nateko.cp.cpauth.core.SprayJsonUtils.*
import spray.json.*


class OrcidAuthenticationService(config: OAuthProviderConfig)(implicit system: ActorSystem, mat: Materializer) {
	import OrcidAuthenticationService._
	import system.dispatcher

	private val http = Http(system)

	def retrieveUserInfo(singleUseCode: String): Future[OrcidUserInfo] = {
		for(
			ui <- getBasicInfo(singleUseCode);
			emails <- getUserEmails(ui.orcidId)
		) yield {
			val firstPrimary = emails.filter(_.isVerified).sortBy(!_.isPrimary).headOption.map(_.email)
			ui.copy(email = firstPrimary)
		}
	}

	private def getBasicInfo(singleUseCode: String): Future[OrcidUserInfo] = {

		val request = HttpRequest(
			uri = "https://orcid.org/oauth/token",
			method = HttpMethods.POST,
			headers = Accept(MediaTypes.`application/json`) :: Nil,
			entity = FormData(
				"client_secret" -> config.clientSecret,
				"client_id" -> config.clientId,
				"grant_type" -> "authorization_code",
				"redirect_uri" -> config.redirectPath,
				"code" -> singleUseCode
			).toEntity
		)

		http.singleRequest(request)
			.flatMap(resp => Unmarshal(resp.entity).to[JsValue])
			.flatMap{jsv =>

				val basicInfoTry: Try[OrcidUserInfo] = for(
					jso <- ensure[JsObject](jsv);
					orcidId <- getStringField(jso, "orcid")
				) yield {
					val nameOpt: Option[String] = getStringFieldOpt(jso, "name")
						.flatMap(n => if(n.trim.isEmpty) None else Some(n.trim))

					val (givenName, surname) = nameOpt match {
						case None => (None, None)
						case Some(name) => name match{
							case nameRegex(gn, sn) => (Some(gn), Some(sn))
							case _ => (None, Some(name))
						}
					}
					OrcidUserInfo(orcidId, None, givenName, surname)
				}

				Future.fromTry(basicInfoTry)
			}
	}

	private def getUserEmails(orcidId: String): Future[Seq[EmailInfo]] = {
		import se.lu.nateko.cp.cpauth.core.CoreUtils.tryseq

		val request = HttpRequest(
			uri = s"https://pub.orcid.org/v2.0/$orcidId/email/",
			headers = Accept(MediaTypes.`application/json`) :: Nil
		)

		http.singleRequest(request)
			.flatMap(resp => Unmarshal(resp.entity).to[JsValue])
			.flatMap{jsv =>
				Future.fromTry(
					for(
						jso <- ensure[JsObject](jsv);
						arr = getFieldOpt[JsArray](jso, "email").getOrElse(JsArray.empty);
						jsEmailObjs <- getElements[JsObject](arr);
						emails <- tryseq(jsEmailObjs.map(getEmailInfo))
					) yield emails
				)
			}
	}

}


private object OrcidAuthenticationService{

	private val nameRegex = """(\w+)\s+(.+)""".r

	private class EmailInfo(val email: String, val isPrimary: Boolean, val isVerified: Boolean)

	private def getEmailInfo(emailObj: JsObject): Try[EmailInfo] = for(
		email <- getStringField(emailObj, "email");
		primary <- getField[JsBoolean](emailObj, "primary");
		verified <- getField[JsBoolean](emailObj, "verified")
	) yield new EmailInfo(email, primary.value, verified.value)

}
