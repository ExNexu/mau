import sbt._

object Dependencies {

  val resolutionRepos = Seq(
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
  )

  def compile(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  val redisScala = "com.etaty.rediscala" %% "rediscala" % "1.4.0"
  val sprayJson = "io.spray" %% "spray-json" % "1.3.1"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.1"
}
