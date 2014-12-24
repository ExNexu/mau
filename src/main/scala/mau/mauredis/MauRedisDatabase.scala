package mau.mauredis

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import mau._
import redis.RedisClient
import spray.json.JsonReader
import spray.json.JsonWriter

class MauDatabaseRedis(
  protected val client: RedisClient,
  protected val namespace: String,
  override protected implicit val ec: ExecutionContext) extends MauDatabase {

  override def get[T <: Model: MauStrategy: JsonReader](id: Id): Future[Option[T]] = ???

  override protected def persist[T <: Model: MauStrategy: JsonWriter](obj: T): Future[T] = ???

  override protected def remove[T <: Model: MauStrategy](id: Id): Future[Int] = ???

  override protected def addToKey(id: Id, key: Key): Future[Int] = ???

  override protected def removeFromKey(id: Id, key: Key): Future[Int] = ???

  override protected def getPureKeyContent[T <: Model: MauStrategy: JsonReader](key: Key): Future[List[T]] = ???

}

object MauDatabaseRedis {
  def apply(client: RedisClient, namespace: String)(implicit ec: ExecutionContext): MauDatabase =
    new MauDatabaseRedis(client, namespace, ec)
}
