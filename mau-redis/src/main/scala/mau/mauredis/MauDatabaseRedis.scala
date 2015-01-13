package mau.mauredis

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.actor.ActorSystem
import mau._
import redis.RedisClient

class MauDatabaseRedis(
  protected val client: RedisClient,
  protected val namespace: String)(
    override protected implicit val actorSystem: ActorSystem) extends MauDatabase {

  override def get[A <: Model[A]: MauStrategy: MauDeSerializer](id: Id): Future[Option[A]] = {
    val mauStrategy = implicitly[MauStrategy[A]]
    val redisKey = longKeyForId(id, mauStrategy.typeName)
    val string: Future[Option[String]] = client.get[String](redisKey)
    string map { string ⇒
      string map { string ⇒
        val mauDeSerializer = implicitly[MauDeSerializer[A]]
        mauDeSerializer.deserialize(string)
      }
    }
  }

  override protected def persist[A <: Model[A]: MauStrategy: MauSerializer](obj: A): Future[A] = {
    val mauStrategy = implicitly[MauStrategy[A]]
    val mauSerializer = implicitly[MauSerializer[A]]

    val id = obj.id.getOrElse(generateId())
    val objWithId = obj.id.fold(obj.withId(id))(_ ⇒ obj)
    val serializedObj = mauSerializer.serialize(objWithId)
    val redisKey = longKeyForId(id, mauStrategy.typeName)

    val saveResult = client.set(redisKey, serializedObj) // TODO: What does the Boolean mean?
    saveResult map (_ ⇒ objWithId)
  }

  override protected def remove[A <: Model[A]: MauStrategy](id: Id): Future[Long] = {
    val mauStrategy = implicitly[MauStrategy[A]]

    val redisKey = longKeyForId(id, mauStrategy.typeName)

    client.del(redisKey)
  }

  override protected def addToKey(id: Id, key: Key, typeName: String): Future[Long] = {
    val redisKey = longKey(key, typeName)
    client.sadd(redisKey, id)
  }

  override protected def removeFromKey(id: Id, key: Key, typeName: String): Future[Long] = {
    val redisKey = longKey(key, typeName)
    client.srem(redisKey, id)
  }

  override protected def getPureKeyContent[A <: Model[A]: MauStrategy: MauDeSerializer](key: Key): Future[Seq[A]] = {
    val mauStrategy = implicitly[MauStrategy[A]]
    val redisKey = longKey(key, mauStrategy.typeName)
    val ids: Future[Seq[String]] = client.smembers[String](redisKey)
    val objs: Future[Seq[A]] =
      ids flatMap { ids ⇒
        val objOptions = Future.sequence(
          ids map { id ⇒
            get[A](id)
          }
        )
        objOptions.map(_.flatten)
      }
    objs
  }

  private def longKey(key: Key, typeName: String) = s"$namespace:$typeName:$key"

  private def longKeyForId(id: Id, typeName: String) = longKey(s"id:$id", typeName)

  private def generateId(): Id = UUID.randomUUID().toString

}

object MauDatabaseRedis {
  def apply(client: RedisClient, namespace: String)(implicit actorSystem: ActorSystem): MauDatabase =
    new MauDatabaseRedis(client, namespace)
}
