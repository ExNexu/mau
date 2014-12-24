package mau.mauredis

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import java.util.UUID

import mau._
import redis.RedisClient

class MauDatabaseRedis(
  protected val client: RedisClient,
  protected val namespace: String,
  override protected implicit val ec: ExecutionContext) extends MauDatabase {

  override def get[T <: Model: MauStrategy: MauDeSerializer](id: Id): Future[Option[T]] = {
    val mauStrategy = implicitly[MauStrategy[T]]
    val key = longKeyForId(id, mauStrategy.typeName)
    val string: Future[Option[String]] = client.get[String](key)
    string map { string ⇒
      string map { string ⇒
        val mauDeSerializer = implicitly[MauDeSerializer[T]]
        mauDeSerializer.deserialize(string)
      }
    }
  }

  override protected def persist[T <: Model: MauStrategy: MauSerializer](obj: T): Future[T] = {
    val mauStrategy = implicitly[MauStrategy[T]]
    val mauSerializer = implicitly[MauSerializer[T]]

    val id = obj.id.getOrElse(generateId())
    val objWithId = obj.id.fold(obj.withId(id))(_ ⇒ obj)
    val serializedObj = mauSerializer.serialize(objWithId)
    val key = longKeyForId(id, mauStrategy.typeName)

    val saveResult = client.set(key, serializedObj) // TODO: What does the Boolean mean?
    saveResult map (_ ⇒ objWithId)
  }

  override protected def remove[T <: Model: MauStrategy](id: Id): Future[Int] = ???

  override protected def addToKey(id: Id, key: Key): Future[Int] = ???

  override protected def removeFromKey(id: Id, key: Key): Future[Int] = ???

  override protected def getPureKeyContent[T <: Model: MauStrategy: MauDeSerializer](key: Key): Future[List[T]] = ???

  private def longKey(key: Key, typeName: String) = s"$namespace:$typeName:$key"

  private def longKeyForId(id: Id, typeName: String) = longKey(id, typeName)

  private def generateId(): Id = UUID.randomUUID().toString

}

object MauDatabaseRedis {
  def apply(client: RedisClient, namespace: String)(implicit ec: ExecutionContext): MauDatabase =
    new MauDatabaseRedis(client, namespace, ec)
}
