resolvers += ("ICOS CP Nexus repo" at "https://repo.icos-cp.eu/content/groups/public")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("com.typesafe.play" % "sbt-twirl" % "1.6.0-RC2")

addSbtPlugin("se.lu.nateko.cp" % "icoscp-sbt-deploy" % "0.3.3")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.13.0")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0")
