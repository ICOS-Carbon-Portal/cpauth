val defaultScala = "3.3.0"

val defaultScalacOptions = Seq(
	"-Xtarget:11",
	"-encoding", "UTF-8",
	"-unchecked",
	"-feature",
	"-deprecation"
)

val commonSettings = Seq(
	organization := "se.lu.nateko.cp",
	scalaVersion := defaultScala,
	libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.15" % "test",
	scalacOptions ++= defaultScalacOptions
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

lazy val envri = crossProject(JSPlatform, JVMPlatform)
	.crossType(CrossType.Pure)
	.in(file("envri"))
	.settings(commonSettings: _*)
	.settings(publishingSettings: _*)
	.settings(
		name := "envri",
		organization := "eu.icoscp",
		version := "0.1.0",
	)

lazy val cpauthCore = project
	.in(file("core"))
	.dependsOn(envri.jvm)
	.settings(commonSettings: _*)
	.settings(publishingSettings: _*)
	.enablePlugins(SbtTwirl)
	.settings(
		name := "cpauth-core",
		version := "0.9.0",
		libraryDependencies ++= Seq(
			"io.spray"              %% "spray-json"         % "1.3.6",
			"com.typesafe"           % "config"             % "1.4.2",
			"com.sun.mail"           % "jakarta.mail"       % "1.6.7",
			"org.slf4j"              % "slf4j-api"          % "1.7.36",
		)
	)


lazy val viewsCore = (project in file("viewsCore"))
	.dependsOn(envri.jvm, cpauthCore)
	.settings(commonSettings: _*)
	.settings(publishingSettings: _*)
	.enablePlugins(SbtTwirl)
	.settings(
		name := "views-core",
		version := "0.7.5",
	)

val akkaVersion = "2.6.19"
val akkaHttpVersion = "10.2.9"
val cpauthMain = Some("se.lu.nateko.cp.cpauth.Main")

lazy val georestheart = (project in file("georestheart"))
	.dependsOn(cpauthCore)
	.settings(commonSettings: _*)
	.settings(publishingSettings: _*)
	.settings(
		name := "georestheart",
		organization := "eu.icoscp",
		version := "0.1.0",
		libraryDependencies ++= Seq(
			"com.typesafe.akka"      %% "akka-http-spray-json"               % akkaHttpVersion excludeAll("io.spray") cross CrossVersion.for3Use2_13,
			"com.typesafe.akka"      %% "akka-stream"                        % akkaVersion cross CrossVersion.for3Use2_13,
		),
	)

lazy val fetchIdpList = taskKey[Unit]("Fetches SAML IdP list from SWAMID")

resolvers := {
	("ICOS CP Nexus repo" at "https://repo.icos-cp.eu/content/groups/public") +:
	resolvers.value
}

lazy val cpauth = (project in file("."))
	.dependsOn(viewsCore, georestheart)
	.settings(commonSettings: _*)
	.enablePlugins(SbtTwirl, IcosCpSbtDeployPlugin)
	.settings(
		name := "cpauth",
		version := "0.7.0",
		libraryDependencies ++= Seq(
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
		),

		fork := true,

		cpDeployTarget := "cpauth",
		cpDeployBuildInfoPackage := "se.lu.nateko.cp.cpauth",
		cpDeployPreAssembly := Def.sequential(
			envri / clean,
			cpauthCore / clean,
			viewsCore / clean,
			georestheart / clean,
			clean,
			Test / test,
			fetchIdpList
		).value,
		cpDeployPlaybook := "core.yml",
		cpDeployPermittedInventories := Some(Seq("staging", "production")),
		cpDeployInfraBranch := "master",

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
