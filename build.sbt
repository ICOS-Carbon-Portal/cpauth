import AssemblyKeys._

name := "Carbon Portal Authentication Service"

version := "0.1"

scalaVersion := "2.11.2"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-actor"       % "2.3.6",
  "com.typesafe.akka"  %% "akka-slf4j"       % "2.3.6",
  "com.typesafe.akka"  %% "akka-testkit"     % "2.3.6" % "test",
  "io.spray"           %% "spray-can"        % "1.3.2",
  "io.spray"           %% "spray-routing"    % "1.3.2",
  "io.spray"           %% "spray-testkit"    % "1.3.2" % "test",
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

assemblySettings

Revolver.settings