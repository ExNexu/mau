organization := "us.bleibinha"

name := "mau"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.11.0", "2.11.1", "2.11.2")

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _)

libraryDependencies ++= Seq(
  "com.etaty.rediscala" %% "rediscala" % "1.4.0",
  "io.spray" %%  "spray-json" % "1.3.1",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M1" cross CrossVersion.full)

scalacOptions ++= Seq(
  "-unchecked",
  "-feature",
  "-deprecation",
  "-Ywarn-dead-code",
  "-encoding", "UTF-8"
)
