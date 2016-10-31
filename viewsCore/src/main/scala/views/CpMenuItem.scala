package views

import java.net.URI

case class CpMenuItem(label: String, ref: URI, children: Seq[CpMenuItem] = Nil)

