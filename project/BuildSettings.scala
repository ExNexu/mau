import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt._
import sbt.Keys._

object BuildSettings {
  val VERSION = "0.0.1-SNAPSHOT"
  val SCALAVERSION = "2.11.4"

  lazy val basicSettings = seq(
    version := VERSION,
    homepage := Some(new URL("http://bleibinha.us")),
    organization := "mau",
    organizationHomepage := Some(new URL("http://bleibinha.us")),
    description := "mau",
    startYear := Some(2014),
    licenses := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scalaVersion := SCALAVERSION,
    resolvers ++= Dependencies.resolutionRepos,
    scalacOptions := Seq(
      "-encoding", "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.6",
      "-language:_",
      "-Ywarn-dead-code",
      "-Xlog-reflective-calls"
    )
  )

  lazy val mauModuleSettings =
    basicSettings ++ formatSettings

  lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test := formattingPreferences
  )

  import scalariform.formatter.preferences._
  def formattingPreferences =
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
}

