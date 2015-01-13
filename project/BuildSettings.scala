import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt._
import sbt.Keys._

object BuildSettings {
  val VERSION = "0.0.1-SNAPSHOT"
  val SCALAVERSION = "2.11.5"

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
    basicSettings ++
    formatSettings ++
    seq(
      // publishing
      credentials += Credentials(Path.userHome / ".ivy2" / ".us-bleibinha-snapshots-credentials"),
      credentials += Credentials(Path.userHome / ".ivy2" / ".us-bleibinha-releases-credentials"),
      credentials += Credentials(Path.userHome / ".ivy2" / ".us-bleibinha-internal-credentials"),
      publishMavenStyle := true,
      publishArtifact in Test := false,
      publishTo := {
        val archiva = "http://bleibinha.us/archiva/repository/"
        Some("internal" at archiva + "internal")
      },
      pomIncludeRepository := { _ => false },
      pomExtra :=
        <scm>
          <url>https://github.com/ExNexu/mau</url>
          <connection>scm:git:git@github.com:ExNexu/mau.git</connection>
        </scm>
        <developers>
          <developer>
            <id>exnexu</id>
            <name>Stefan Bleibinhaus</name>
            <url>http://bleibinha.us</url>
          </developer>
        </developers>
    )

  lazy val noPublishing = seq(
    publish := (),
    publishLocal := (),
    // required until these tickets are closed https://github.com/sbt/sbt-pgp/issues/42,
    // https://github.com/sbt/sbt-pgp/issues/36
    publishTo := None
  )

  lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test := formattingPreferences
  )

  import scalariform.formatter.preferences._
  def formattingPreferences =
    FormattingPreferences()
      .setPreference(AlignParameters, true)
      .setPreference(CompactStringConcatenation, false)
      .setPreference(IndentPackageBlocks, true)
      .setPreference(FormatXml, true)
      .setPreference(PreserveSpaceBeforeArguments, false)
      .setPreference(DoubleIndentClassDeclaration, false)
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
      .setPreference(SpaceBeforeColon, false)
      .setPreference(SpaceInsideBrackets, false)
      .setPreference(SpaceInsideParentheses, false)
      .setPreference(PreserveDanglingCloseParenthesis, true)
      .setPreference(IndentSpaces, 2)
      .setPreference(IndentLocalDefs, false)
      .setPreference(SpacesWithinPatternBinders, true)
}

