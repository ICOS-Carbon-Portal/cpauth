val defaultScala = "2.12.4"

val commonSettings = Seq(
	organization := "se.lu.nateko.cp",
	scalaVersion := defaultScala,

	libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test",

	scalacOptions ++= Seq(
		"-target:jvm-1.8",
		"-encoding", "UTF-8",
		"-unchecked",
		"-feature",
		"-deprecation",
		"-Xfuture",
		"-Yno-adapted-args",
		"-Ywarn-dead-code",
		"-Ywarn-numeric-widen",
		"-Ywarn-unused"
	)
)

val publishingSettings = Seq(
	crossScalaVersions := Seq(defaultScala, "2.11.11"),
	publishTo := {
		val nexus = "https://repo.icos-cp.eu/content/repositories/"
		if (isSnapshot.value)
			Some("snapshots" at nexus + "snapshots")
		else
			Some("releases"  at nexus + "releases")
	},
	credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
)

lazy val cpauthCore = (project in file("core"))
	.settings(commonSettings: _*)
	.settings(publishingSettings: _*)
	.settings(
		name := "cpauth-core",
		version := "0.5.1-SNAPSHOT",
		libraryDependencies ++= Seq(
		)
	)


lazy val viewsCore = (project in file("viewsCore"))
	.settings(commonSettings: _*)
	.settings(publishingSettings: _*)
	.enablePlugins(SbtTwirl)
	.settings(
		name := "views-core",
		version := "0.3.3-SNAPSHOT",
		scalacOptions += "-Ywarn-unused-import:false"
	)


val akkaVersion = "2.4.19"
val akkaHttpVersion = "10.0.9"
val cpauthMain = Some("se.lu.nateko.cp.cpauth.Main")

lazy val fetchIdpList = taskKey[Unit]("Fetches SAML IdP list from SWAMID")

lazy val cpauth = (project in file("."))
	.dependsOn(cpauthCore, viewsCore)
	.settings(commonSettings: _*)
	.enablePlugins(SbtTwirl, IcosCpSbtDeployPlugin)
	.settings(
		name := "Carbon Portal Authentication Service",
		version := "0.4.2",
		libraryDependencies ++= Seq(
			"com.typesafe.akka"      %% "akka-http-spray-json"               % akkaHttpVersion,
			"com.typesafe.akka"      %% "akka-http-testkit"                  % akkaHttpVersion % "test",
			"com.typesafe.akka"      %% "akka-slf4j"                         % akkaVersion,
			"com.typesafe.akka"      %% "akka-stream"                        % akkaVersion,
			"ch.qos.logback"         %  "logback-classic"                    % "1.1.3",
			"org.opensaml"           %  "opensaml"                           % "2.6.6",
			"org.scala-lang.modules" %% "scala-xml"                          % "1.0.5",
			"org.apache.santuario"   %  "xmlsec"                             % "2.0.7", //to force a newer version
//			"xalan"                  %  "serializer"                         % "2.7.2", //for DOM serialization to strings during debug
			"net.jcip"               %  "jcip-annotations"                   % "1.0",
			"org.joda"               %  "joda-convert"                       % "1.7",
			"org.hsqldb"             %  "hsqldb"                             % "2.3.4",
			"org.apache.commons"     % "commons-email"                       % "1.4"
		),

		fork := true,

		cpDeployTarget := "cpauth",
		cpDeployBuildInfoPackage := "se.lu.nateko.cp.cpauth",

		fetchIdpList := {
			import java.nio.file.{StandardCopyOption, Files, Paths}
			val url = new java.net.URL("http://mds.swamid.se/md/swamid-idp-transitive.xml")
			val file = Paths.get("./src/main/resources/swamid-idps.xml")
			Files.copy(url.openStream(), file, StandardCopyOption.REPLACE_EXISTING)
		},

		assembly := (Def.taskDyn{
			val original = assembly.taskValue
			fetchIdpList.value
			Def.task(original.value)
		}).value,

		//initialCommands in console := """""",

		mainClass in assembly := cpauthMain,
		mainClass in (Compile, run) := cpauthMain

	)
