package se.lu.nateko.cp.viewscore

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI

import java.net.URL
import java.util.Scanner

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.w3c.dom.Node

import javax.xml.parsers.DocumentBuilderFactory
import views.CpMenuItem
import views.CpMenuGroup
import views.CpMenuLeaf


object MenuFetcher {

	def getMenu: Try[Seq[CpMenuItem]] = for(
		in <- getPageStream();
		html <- findMenu(in);
		node <- parseDiv(html);
		menu <- parseMenu(node)
	) yield menu

	private def getPageStream(): Try[InputStream] = Try{
		new URL(views.CpMenu.cpHome).openStream()
	}

	private def findMenu(in: InputStream): Try[String] = {
		var res: String = ""
		val scanner = new Scanner(in)
		scanner.useDelimiter("div")


		while(res == "" && scanner.hasNext){
			val block = scanner.next()
			if(block.contains("id=\"cp_theme_d8_menu\"")){
				res = "<div" + block + "div>"
				scanner.close()
			}
		}
		if(res.isEmpty) Failure(new Exception("Could not find menu in CP's main page HTML")) else Success(res)
	}

	private def parseDiv(html: String): Try[Node] = Try{
		val imgPattern = "<img.*?>".r
		val xml = imgPattern.replaceAllIn(html, "").replaceAll("&", "&amp;")

		DocumentBuilderFactory
			.newInstance()
			.newDocumentBuilder()
			.parse(new ByteArrayInputStream(xml.getBytes))
			.getDocumentElement()
	}

	private def parseMenu(node: Node): Try[Seq[CpMenuItem]] = Try{
		val topItems = children("ul", node).flatMap(ul => children("li", ul))
		topItems.map(parseMenuItem)
	}

	private def parseMenuItem(liNode: Node): CpMenuItem = {
		val anchor = children("a", liNode).headOption.getOrElse(throw new Exception("Missing <a> tag in <li>"))
		val label = anchor.getTextContent

		val childItems = parseMenu(liNode).get
		if(childItems.isEmpty) {
			val ref = anchor.getAttributes.getNamedItem("href").getTextContent
			CpMenuLeaf(label, new URI(ref))
		}else CpMenuGroup(label, childItems)
	}

	private def children(name: String, node: Node): IndexedSeq[Node] = {
		val children = node.getChildNodes
		IndexedSeq.range(0, children.getLength).collect{
			case i if(children.item(i).getNodeName == name) => children.item(i)
		}
	}
}
