val defaultScala = "3.3.4"

val defaultScalacOptions = Seq(
	"-Xtarget:21",
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
		version := "0.10.1",
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
		version := "0.7.13",
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
	("Shibboleth Releases" at "https://build.shibboleth.net/maven/releases/") +:
	resolvers.value
}

lazy val cpauth = (project in file("."))
	.dependsOn(viewsCore, georestheart)
	.settings(commonSettings: _*)
	.enablePlugins(SbtTwirl, IcosCpSbtDeployPlugin)
	.settings(
		name := "cpauth",
		version := "0.8.0",
		libraryDependencies ++= Seq(
			"com.typesafe.akka"      %% "akka-http-testkit"                  % akkaHttpVersion % "test" cross CrossVersion.for3Use2_13,
			"com.typesafe.akka"      %% "akka-stream-testkit"                % akkaVersion     % "test" cross CrossVersion.for3Use2_13,
			"com.typesafe.akka"      %% "akka-stream"                        % akkaVersion cross CrossVersion.for3Use2_13,
			"com.typesafe.akka"      %% "akka-slf4j"                         % akkaVersion cross CrossVersion.for3Use2_13,
			"ch.qos.logback"         %  "logback-classic"                    % "1.5.16",
			"org.opensaml"           %  "opensaml-saml-impl"                 % "4.3.2",
			"org.scala-lang.modules" %% "scala-xml"                          % "2.3.0",
//			"xalan"                  %  "serializer"                         % "2.7.2", //for DOM serialization to strings during debug
			"net.jcip"               %  "jcip-annotations"                   % "1.0",
			"org.hsqldb"             %  "hsqldb"                             % "2.3.4",
		),

		fork := true,

		cpDeployTarget := "cpauth",
		cpDeployBuildInfoPackage := "se.lu.nateko.cp.cpauth",
		cpDeployPreAssembly := Def.sequential(
			envri.js / clean,
			envri.jvm / clean,
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
			val fname = "swamid-idps.xml"
			val url = new java.net.URI("http://mds.swamid.se/md/swamid-idp-transitive.xml").toURL()
			val resourcesFolder = (Compile / resourceDirectory).value
			val file = resourcesFolder.toPath.resolve(fname)
			streams.value.log.info(s"Fetching SAML identity provider list from SWAMID to $file ...")
			try{
				Files.copy(url.openStream(), file, StandardCopyOption.REPLACE_EXISTING)
				val classDirFolder = (Compile / classDirectory).value
				val targetFile = classDirFolder.toPath.resolve(fname)
				Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
			} catch{
				case _: Throwable =>
					streams.value.log.warn(s"SAML IdP list fetch failed, will use stale list if available (check that it is!)")
			}
		},

		//initialCommands in console := """""",

		assembly / mainClass  := cpauthMain,

		assembly / assemblyMergeStrategy := {
			case PathList(ps @ _*) if ps.last == "module-info.class" => MergeStrategy.discard
			case x => ((assembly / assemblyMergeStrategy).value)(x)
		},

		Compile / mainClass := cpauthMain

	)
