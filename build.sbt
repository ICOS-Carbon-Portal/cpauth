
name := "Carbon Portal Authentication Service"

version := "0.1"

scalaVersion := "2.11.5"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "org.opensaml"       %  "opensaml"         % "2.6.4" withJavadoc,
  "xerces"             %  "xercesImpl"       % "2.11.0",
  "net.jcip"           %  "jcip-annotations" % "1.0",
  "org.joda"           %  "joda-convert"     % "1.2",
  "com.typesafe.akka"  %% "akka-actor"       % "2.3.9",
  "com.typesafe.akka"  %% "akka-slf4j"       % "2.3.9",
  "com.typesafe.akka"  %% "akka-testkit"     % "2.3.9" % "test",
  "io.spray"           %% "spray-client"     % "1.3.2",
  "io.spray"           %% "spray-can"        % "1.3.2",
  "io.spray"           %% "spray-routing"    % "1.3.2",
  "io.spray"           %% "spray-json"       % "1.3.1",
  "io.spray"           %% "spray-testkit"    % "1.3.2" % "test",
  "commons-codec"      %  "commons-codec"    % "1.9",
  "commons-io"         %  "commons-io"       % "2.4",
  "ch.qos.logback"     %  "logback-classic"  % "1.0.13",
  "org.scalatest"      %  "scalatest_2.11"   % "2.2.1" % "test"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

fork := true

initialCommands in console := """
import se.lu.nateko.cpauth.Constants
import se.lu.nateko.cpauth.opensaml._
"""

val cpauthMain = Some("se.lu.nateko.cpauth.Main")
mainClass in assembly := cpauthMain
mainClass in (Compile, run) := cpauthMain

Revolver.settings
