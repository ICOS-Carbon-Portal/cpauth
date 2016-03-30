
lazy val commonSettings = Seq(
	organization := "se.lu.nateko.cp",
	scalaVersion := "2.11.7",

	libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test",
	
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
		version := "0.3-SNAPSHOT",
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

val akkaVersion = "2.3.14"
val sprayVersion = "1.3.3"
val cpauthMain = Some("se.lu.nateko.cp.cpauth.Main")

lazy val cpauth = (project in file("."))
	.dependsOn(cpauthCore)
	.settings(commonSettings: _*)
	.settings(
		name := "Carbon Portal Authentication Service",
		version := "0.3",

		libraryDependencies ++= Seq(
			"org.opensaml"       %  "opensaml"         % "2.6.4" withJavadoc,
			"xerces"             %  "xercesImpl"       % "2.11.0",
			"net.jcip"           %  "jcip-annotations" % "1.0",
			"org.joda"           %  "joda-convert"     % "1.2",
			"com.typesafe.akka"  %% "akka-actor"       % akkaVersion,
			"com.typesafe.akka"  %% "akka-slf4j"       % akkaVersion,
			"com.typesafe.akka"  %% "akka-testkit"     % akkaVersion % "test",
			"com.typesafe.slick" %% "slick"            % "3.0.1",
			"org.hsqldb"         %  "hsqldb"           % "2.3.2",
			"com.zaxxer"         %  "HikariCP"         % "2.3.5",
			"io.spray"           %% "spray-client"     % sprayVersion,
			"io.spray"           %% "spray-can"        % sprayVersion,
			"io.spray"           %% "spray-routing"    % sprayVersion,
			"io.spray"           %% "spray-json"       % "1.3.2",
			"io.spray"           %% "spray-testkit"    % sprayVersion % "test",
			"ch.qos.logback"     %  "logback-classic"  % "1.0.13"
		),

		fork := true,

		initialCommands in console := """
			import se.lu.nateko.cp.cpauth._
			import se.lu.nateko.cp.cpauth.accounts.Users
			import se.lu.nateko.cp.cpauth.core.UserInfo""",

		mainClass in assembly := cpauthMain,
		mainClass in (Compile, run) := cpauthMain

	)

Revolver.settings

