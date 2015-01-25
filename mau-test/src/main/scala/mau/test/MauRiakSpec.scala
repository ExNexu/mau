package mau.test

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

import akka.actor.ActorSystem
import com.scalapenos.riak._

abstract class MauRiakSpec(val testName: String, val cleanup: Boolean = true) extends MauSpec {
  implicit val actorSystem = ActorSystem(s"$testName-System")
  val riakClient = RiakClient(actorSystem)
  val namespace = s"Mau:Test:$testName:"

  override def afterEach() {
    if (cleanup) {
      // TODO: Get all bucket keys, delete everything
    }
  }

  override def afterAll() {
    actorSystem.shutdown()
  }
}
