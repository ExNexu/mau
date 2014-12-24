package us.bleibinha.mau.redis

import scala.concurrent.Future

import redis.RedisClient
import spray.json.JsonReader
import spray.json.JsonWriter
import us.bleibinha.mau._

class MauDatabaseRedis(val client: RedisClient, val namespace: String) extends MauDatabase {
  def save[T <: Model: MauStrategy: JsonWriter](obj: T): Future[T] = ???

  def get[T <: Model: MauStrategy: JsonReader](id: Id): Future[Option[T]] = ???

  def getKeyContent[T <: Model: MauStrategy: JsonReader](key: Key): Future[List[T]] = ???

  def delete[T <: Model: MauStrategy: JsonReader](id: Id): Future[Int] = ???
}

object MauDatabaseRedis {
  def apply(client: RedisClient, namespace: String) =
    new MauDatabaseRedis(client, namespace)
}
