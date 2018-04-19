package se.lu.nateko.cp.cpauth.utils

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.Marshalling._
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model._
import scala.concurrent.Future
import play.twirl.api._

object TemplatePageMarshalling {

	def marshaller[F <: BufferedContent[F]]: ToResponseMarshaller[F] = Marshaller(
		implicit exeCtxt => content => Future{

			val mediaType = content match {
				case _: Html => MediaTypes.`text/html`
				case _: JavaScript => MediaTypes.`application/javascript`
				case _ =>
					throw new Exception(s"Unsupported template type ${content.getClass.getName}")
			}

			val responseMaker = (charset: HttpCharset) => HttpResponse(
				entity = HttpEntity(ContentType.WithCharset(mediaType, charset), content.body)
			)

			WithOpenCharset(mediaType, responseMaker) :: Nil
		}
	)

}