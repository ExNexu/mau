package mau.test

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.ActorSystem
import redis.RedisClient

abstract class MauRedisSpec(val testName: String, val cleanup: Boolean = true) extends MauSpec {
  implicit val actorSystem = ActorSystem(s"$testName-System")
  val redisClient = RedisClient()
  val namespace = s"Mau:Test:$testName"

  override def afterEach() {
    if (cleanup) {
      val keys = redisClient.keys(s"$namespace*")
      await(keys map redisClient.del)
    }
  }

  override def afterAll() {
    actorSystem.shutdown()
  }
}
