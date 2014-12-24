package mau.mauredis

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import mau._
import redis.RedisClient
import spray.json.JsonReader
import spray.json.JsonWriter
import spray.json.JsValue
import spray.json.JsonParser

class MauDatabaseRedis(
  protected val client: RedisClient,
  protected val namespace: String,
  override protected implicit val ec: ExecutionContext) extends MauDatabase {

  override def get[T <: Model: MauStrategy: JsonReader](id: Id): Future[Option[T]] = {
    val mauStrategy = implicitly[MauStrategy[T]]
    val key = longKeyForId(id, mauStrategy.typeName)
    val json: Future[Option[String]] = client.get[String](key)
    json map { json ⇒
      json map { json ⇒
        val jsonJsValue = JsonParser(json)
        val jsonReader = implicitly[JsonReader[T]]
        jsonReader.read(jsonJsValue)
      }
    }
  }

  override protected def persist[T <: Model: MauStrategy: JsonWriter](obj: T): Future[T] = ???

  override protected def remove[T <: Model: MauStrategy](id: Id): Future[Int] = ???

  override protected def addToKey(id: Id, key: Key): Future[Int] = ???

  override protected def removeFromKey(id: Id, key: Key): Future[Int] = ???

  override protected def getPureKeyContent[T <: Model: MauStrategy: JsonReader](key: Key): Future[List[T]] = ???

  private def longKey(key: Key, typeName: String) = s"$namespace:$typeName:$key"
  private def longKeyForId(id: Id, typeName: String) = longKey(id, typeName)

}

object MauDatabaseRedis {
  def apply(client: RedisClient, namespace: String)(implicit ec: ExecutionContext): MauDatabase =
    new MauDatabaseRedis(client, namespace, ec)
}
