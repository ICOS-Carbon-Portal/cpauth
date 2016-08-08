
lazy val commonSettings = Seq(
	organization := "se.lu.nateko.cp",
	scalaVersion := "2.11.8",

	libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test",

	scalacOptions ++= Seq(
		"-unchecked",
		"-deprecation",
		"-Xlint",
		"-Ywarn-dead-code",
		"-language:_",
		"-target:jvm-1.8",
		"-encoding", "UTF-8"
	)
)

lazy val cpauthCore = (project in file("core"))
	.settings(commonSettings: _*)
	.settings(
		name := "cpauth-core",
		version := "0.4-SNAPSHOT",
		libraryDependencies ++= Seq(
		),
		publishTo := {
			val nexus = "https://repo.icos-cp.eu/content/repositories/"
			if (isSnapshot.value)
				Some("snapshots" at nexus + "snapshots") 
			else
				Some("releases"  at nexus + "releases")
		},
		credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
	)

val akkaVersion = "2.4.8"
val cpauthMain = Some("se.lu.nateko.cp.cpauth.Main")

lazy val cpauth = (project in file("."))
	.dependsOn(cpauthCore)
	.settings(commonSettings: _*)
	.settings(
		name := "Carbon Portal Authentication Service",
		version := "0.4",

		libraryDependencies ++= Seq(
			"com.typesafe.akka"      %% "akka-http-spray-json-experimental"  % akkaVersion,
			"com.typesafe.akka"      %% "akka-slf4j"                         % akkaVersion,
			"com.typesafe.akka"      %% "akka-http-testkit"                  % akkaVersion % "test",
			"ch.qos.logback"         %  "logback-classic"                    % "1.1.3",
			"org.opensaml"           %  "opensaml"                           % "2.6.4",
			"org.scala-lang.modules" %% "scala-xml"                          % "1.0.5",
			"net.jcip"               %  "jcip-annotations"                   % "1.0",
			"org.joda"               %  "joda-convert"                       % "1.7",
			"com.typesafe.slick"     %% "slick"                              % "3.1.1",
			"com.typesafe.slick"     %% "slick-hikaricp"                     % "3.1.1",
			"org.hsqldb"             %  "hsqldb"                             % "2.3.4"
		),

		fork := true,

		initialCommands in console := """
			import se.lu.nateko.cp.cpauth._
			import se.lu.nateko.cp.cpauth.accounts.Users
			import se.lu.nateko.cp.cpauth.core.UserInfo""",

		mainClass in assembly := cpauthMain,
		mainClass in (Compile, run) := cpauthMain

	)

