import sbt._
import sbt.Keys._

object Build extends Build {
  import BuildSettings._
  import Dependencies._

  // configure prompt to show current project
  override lazy val settings = super.settings :+ {
    shellPrompt := { s ⇒ Project.extract(s).currentProject.id + " > " }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Root Project
  // -------------------------------------------------------------------------------------------------------------------

  lazy val root = Project("root", file("."))
    .aggregate(mauTest, mauCore, mauRedis, mauSprayJson, mauAnnotation)
    .settings(basicSettings: _*)
    .settings(noPublishing: _*)

  // -------------------------------------------------------------------------------------------------------------------
  // Modules
  // -------------------------------------------------------------------------------------------------------------------

  lazy val mauTest = Project("mau-test", file("mau-test"))
    .settings(mauModuleSettings: _*)
    .settings(libraryDependencies ++=
      compile(redisScala, riakScala, scalaTest)
    )

  lazy val mauCore = Project("mau-core", file("mau-core"))
    .dependsOn(mauTest % "test")
    .settings(mauModuleSettings: _*)
    .settings(
      libraryDependencies ++=
        compile(akkaActorLocking)
    )

  lazy val mauRedis = Project("mau-redis", file("mau-redis"))
    .dependsOn(
      mauCore,
      mauTest % "test",
      mauSprayJson % "test"
    )
    .settings(mauModuleSettings: _*)
    .settings(libraryDependencies ++=
      compile(redisScala)
    )

  lazy val mauRiak = Project("mau-riak", file("mau-riak"))
    .dependsOn(
      mauCore,
      mauTest % "test",
      mauSprayJson % "test"
    )
    .settings(mauModuleSettings: _*)
    .settings(libraryDependencies ++=
      compile(riakScala)
    )

  lazy val mauSprayJson = Project("mau-spray-json", file("mau-spray-json"))
    .dependsOn(
      mauCore,
      mauTest % "test"
    )
    .settings(mauModuleSettings: _*)
    .settings(libraryDependencies ++=
      compile(sprayJson)
    )

  lazy val mauAnnotation = Project("mau-annotation", file("mau-annotation"))
    .dependsOn(
      mauCore,
      mauTest % "test",
      mauRedis % "test",
      mauSprayJson % "test"
    )
    .settings(mauModuleSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % SCALAVERSION,
        compilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M1" cross CrossVersion.full)
      )
    )

  // -------------------------------------------------------------------------------------------------------------------
  // Example Projects
  // -------------------------------------------------------------------------------------------------------------------

}
