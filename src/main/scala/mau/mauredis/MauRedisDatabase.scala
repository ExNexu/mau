package mau.mauredis

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import mau._
import redis.RedisClient
import spray.json.JsonReader
import spray.json.JsonWriter

class MauDatabaseRedis(val client: RedisClient, val namespace: String) extends MauDatabase {

  override def get[T <: Model: MauStrategy: JsonReader](id: Id)(implicit ec: ExecutionContext): Future[Option[T]] = ???

  override def getKeyContent[T <: Model: MauStrategy: JsonReader](key: Key)(implicit ec: ExecutionContext): Future[List[T]] = ???

  override protected def persist[T <: Model: MauStrategy: JsonWriter](obj: T)(implicit ec: ExecutionContext): Future[T] = ???

  override protected def remove[T <: Model: MauStrategy](id: Id)(implicit ec: ExecutionContext): Future[Int] = ???

  override protected def addToKey(id: Id, key: Key)(implicit ec: ExecutionContext): Future[Int] = ???

  override protected def removeFromKey(id: Id, key: Key)(implicit ec: ExecutionContext): Future[Int] = ???

}

object MauDatabaseRedis {
  def apply(client: RedisClient, namespace: String): MauDatabase =
    new MauDatabaseRedis(client, namespace)
}
