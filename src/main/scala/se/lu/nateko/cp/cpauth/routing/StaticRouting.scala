package se.lu.nateko.cp.cpauth.routing

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import play.twirl.api.Html
import akka.http.scaladsl.model.Uri.apply
import se.lu.nateko.cp.cpauth.TemplatePageMarshalling

trait StaticRouting {

	private[this] val pages: PartialFunction[String, Html] = {
		case "login" => views.html.CpauthLoginPage()
		case "home" => views.html.CpauthHomePage()
		case "administration" => views.html.CpauthAdminPage()
	}

	private[this] implicit val pageMarsh = TemplatePageMarshalling.marshaller

	val staticRoute: Route =
		path("favicon.ico"){
			getFromResource("favicon.ico")
		} ~
		path("home" ~ Slash){
			complete(views.html.CpauthHomePage())
		} ~
		pathPrefix(Segment){page =>
			if(pages.isDefinedAt(page)) {
				pathSingleSlash{
					complete(pages(page))
				} ~
				pathEnd{
					redirect(s"/$page/", StatusCodes.Found)
				} ~
				path(s"$page.js"){
					getFromResource(s"www/$page.js")
				} ~
				path("common.js"){
					getFromResource(s"www/common.js")
				}
			} else reject
		}
}
