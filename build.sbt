import AssemblyKeys._

name := "Carbon Portal Authentication Service"

version := "0.1"

scalaVersion := "2.11.4"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "org.opensaml"       %  "opensaml"         % "2.6.1",
  "xerces"             %  "xercesImpl"       % "2.11.0",
  "net.jcip"           %  "jcip-annotations" % "1.0",
  "com.typesafe.akka"  %% "akka-actor"       % "2.3.6",
  "com.typesafe.akka"  %% "akka-slf4j"       % "2.3.6",
  "com.typesafe.akka"  %% "akka-testkit"     % "2.3.6" % "test",
  "io.spray"           %% "spray-client"     % "1.3.2",
  "io.spray"           %% "spray-can"        % "1.3.2",
  "io.spray"           %% "spray-routing"    % "1.3.2",
  "io.spray"           %% "spray-testkit"    % "1.3.2" % "test",
  "commons-codec"      %  "commons-codec"    % "1.9",
  "ch.qos.logback"      % "logback-classic"  % "1.0.13"
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
import se.lu.nateko.samltest.Main._
"""

assemblySettings

Revolver.settings
