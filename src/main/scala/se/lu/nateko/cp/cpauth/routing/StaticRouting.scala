package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import play.twirl.api.Html
import akka.http.scaladsl.model.Uri.apply
import se.lu.nateko.cp.cpauth.utils.TemplatePageMarshalling
import se.lu.nateko.cp.cpauth.oauth.facebook.FacebookAuthenticationService

trait StaticRouting {

  def facebookAuth: FacebookAuthenticationService

	private[this] val pages: PartialFunction[String, Option[Html]] = {
		case "login" => Some(views.html.CpauthLoginPage(facebookUrl = facebookAuth.generateService, twitterUrl = ""))
		case "home" => Some(views.html.CpauthHomePage())
		case "administration" => Some(views.html.CpauthAdminPage())
		case "passwordreset" => None
	}

	private[this] implicit val pageMarsh = TemplatePageMarshalling.marshaller

	lazy val staticRoute: Route =
		path("favicon.ico"){
			getFromResource("favicon.ico")
		} ~
		path("home" ~ Slash){
			complete(views.html.CpauthHomePage())
		} ~
		pathPrefix(Segment){pageId =>
			if(pages.isDefinedAt(pageId)) {
				pathSingleSlash{
					pages(pageId) match{
						case Some(page) => complete(page)
						case None => reject
					}
				} ~
				pathEnd{
					redirect(s"/$pageId/", StatusCodes.Found)
				} ~
				path(s"$pageId.js"){
					getFromResource(s"www/$pageId.js")
				} ~
				path("common.js"){
					getFromResource(s"www/common.js")
				}
			} else reject
		}
}
