val defaultScala = "3.2.0"

val commonSettings = Seq(
	organization := "se.lu.nateko.cp",
	scalaVersion := defaultScala,

	libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.13" % "test",

	scalacOptions ++= Seq(
		"-Xtarget:11",
		"-encoding", "UTF-8",
		"-unchecked",
		"-feature",
		"-deprecation"
	)
)

val publishingSettings = Seq(
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
		version := "0.7.0",
		libraryDependencies ++= Seq(
			"io.spray"              %% "spray-json"                         % "1.3.6"
		),
	)


lazy val viewsCore = (project in file("viewsCore"))
	.settings(commonSettings: _*)
	.settings(publishingSettings: _*)
	.enablePlugins(SbtTwirl)
	.settings(
		name := "views-core",
		version := "0.6.2",
		libraryDependencies ++= Seq(
			"io.spray"              %% "spray-json"                         % "1.3.6"
		),
	)


val akkaVersion = "2.6.19"
val akkaHttpVersion = "10.2.9"
val cpauthMain = Some("se.lu.nateko.cp.cpauth.Main")

lazy val fetchIdpList = taskKey[Unit]("Fetches SAML IdP list from SWAMID")

resolvers := {
	("ICOS CP Nexus repo" at "https://repo.icos-cp.eu/content/groups/public") +:
	resolvers.value
}

lazy val cpauth = (project in file("."))
	.dependsOn(cpauthCore, viewsCore)
	.settings(commonSettings: _*)
	.enablePlugins(SbtTwirl, IcosCpSbtDeployPlugin)
	.settings(
		name := "cpauth",
		version := "0.6.0",
		libraryDependencies ++= Seq(
			"com.typesafe.akka"      %% "akka-http-spray-json"               % akkaHttpVersion excludeAll("io.spray") cross CrossVersion.for3Use2_13,
			"com.typesafe.akka"      %% "akka-http-testkit"                  % akkaHttpVersion % "test" cross CrossVersion.for3Use2_13,
			"com.typesafe.akka"      %% "akka-stream-testkit"                % akkaVersion     % "test" cross CrossVersion.for3Use2_13,
			"com.typesafe.akka"      %% "akka-stream"                        % akkaVersion cross CrossVersion.for3Use2_13,
			"com.typesafe.akka"      %% "akka-slf4j"                         % akkaVersion cross CrossVersion.for3Use2_13,
			"ch.qos.logback"         %  "logback-classic"                    % "1.1.3",
			"org.opensaml"           %  "opensaml"                           % "2.6.6",
			"org.scala-lang.modules" %% "scala-xml"                          % "2.1.0",
			"org.apache.santuario"   %  "xmlsec"                             % "2.0.7", //to force a newer version
//			"xalan"                  %  "serializer"                         % "2.7.2", //for DOM serialization to strings during debug
			"net.jcip"               %  "jcip-annotations"                   % "1.0",
			"org.joda"               %  "joda-convert"                       % "1.7",
			"org.hsqldb"             %  "hsqldb"                             % "2.3.4",
			"jakarta.mail"           %  "jakarta.mail-api"                   % "1.6.7",
			"org.postgresql"         % "postgresql"                          % "42.2.12",
			"org.apache.commons"     % "commons-dbcp2"                       % "2.7.0" exclude("commons-logging", "commons-logging")
		),

		fork := true,

		cpDeployTarget := "cpauth",
		cpDeployBuildInfoPackage := "se.lu.nateko.cp.cpauth",
		cpDeployPreAssembly := Def.sequential(Test / test, fetchIdpList).value,

		fetchIdpList := {
			import java.nio.file.{StandardCopyOption, Files, Paths}
			val url = new java.net.URL("http://mds.swamid.se/md/swamid-idp-transitive.xml")
			val file = Paths.get("./src/main/resources/swamid-idps.xml")
			streams.value.log.info("Fetching SAML identity provider list from SWAMID...")
			Files.copy(url.openStream(), file, StandardCopyOption.REPLACE_EXISTING)
		},

		//initialCommands in console := """""",

		assembly / mainClass  := cpauthMain,
		Compile / mainClass := cpauthMain

	)
