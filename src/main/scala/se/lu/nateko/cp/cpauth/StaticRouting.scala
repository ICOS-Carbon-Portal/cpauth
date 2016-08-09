package se.lu.nateko.cp.cpauth

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes

trait StaticRouting {

	private[this] val pages = Set("login", "home", "administration")

	val staticRoute: Route =
		path("favicon.ico"){
			getFromResource("favicon.ico")
		} ~
		pathPrefix(Segment){page =>
			if(pages.contains(page)) {
				pathSingleSlash{
					getFromResource(s"www/$page/index.html")
				} ~
				pathEnd{
					redirect(s"/$page/", StatusCodes.Found)
				} ~
				path(s"$page.js"){
					getFromResource(s"www/$page/$page.js")
				}
			} else reject
		}
}
