package se.lu.nateko.cp.cpauth.utils

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.Marshalling._
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model._
import scala.concurrent.Future
import play.twirl.api.Html

object TemplatePageMarshalling {

	private def getHtml(html: Html, charset: HttpCharset) = HttpResponse(
		entity = HttpEntity(
			ContentType.WithCharset(MediaTypes.`text/html`, charset),
			html.body
		)
	)

	def marshaller: ToResponseMarshaller[Html] = Marshaller(
		implicit exeCtxt => html => Future.successful(
			WithOpenCharset(MediaTypes.`text/html`, getHtml(html, _)) :: Nil
		)
	)

}