package mau.mauriak

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.actor.ActorSystem
import com.scalapenos.riak._
import mau._

class MauDatabaseRiak(
  protected val client: RiakClient,
  protected val namespace: String)(
    override protected implicit val actorSystem: ActorSystem) extends MauDatabase {

  protected val bucket = client.bucket(namespace)

  override def get[A <: Model[A]: MauStrategy: MauDeSerializer](id: Id): Future[Option[A]] = {
    val mauStrategy = implicitly[MauStrategy[A]]
    val riakKey = longKeyForId(id, mauStrategy.typeName)
    val string: Future[Option[String]] =
      bucket.fetch(riakKey).map(_.map(_.as[String]))
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
    val riakKey = longKeyForId(id, mauStrategy.typeName)

    val saveResult = bucket.store(riakKey, serializedObj)
    saveResult map (_ ⇒ objWithId)
  }

  override protected def remove[A <: Model[A]: MauStrategy](id: Id): Future[Long] = {
    val mauStrategy = implicitly[MauStrategy[A]]

    val riakKey = longKeyForId(id, mauStrategy.typeName)

    bucket.fetch(riakKey) flatMap {
      case Some(_) ⇒
        bucket.delete(riakKey).map(_ ⇒ 1L)
      case None ⇒
        Future.successful(0L)
    }
  }

  override protected def addToKey(id: Id, key: Key, typeName: String): Future[Long] = {
    val riakKey = longKey(key, typeName)
    val existingIds: Future[Set[String]] =
      bucket.fetch(riakKey).map(_.map(_.as[String].split(",").toSet).getOrElse(Set.empty))
    val newIdsMeta = existingIds map { existingIds ⇒
      val newIds: Set[String] = existingIds + id
      (newIds.mkString(","), newIds.size)
    }
    newIdsMeta flatMap {
      case (newIds, indexSize) ⇒
        bucket.store(riakKey, newIds).map(_ ⇒ indexSize)
    }
  }

  override protected def removeFromKey(id: Id, key: Key, typeName: String): Future[Long] = {
    val riakKey = longKey(key, typeName)
    val existingIds: Future[Set[String]] =
      bucket.fetch(riakKey).map(_.map(_.as[String].split(",").toSet).getOrElse(Set.empty))
    val newIdsMeta = existingIds map { existingIds ⇒
      val newIds: Set[String] = existingIds - id
      (newIds.mkString(","), newIds.size)
    }
    newIdsMeta flatMap {
      case (newIds, indexSize) ⇒
        bucket.store(riakKey, newIds).map(_ ⇒ indexSize)
    }
  }

  override protected def getPureKeyContent[A <: Model[A]: MauStrategy: MauDeSerializer](key: Key): Future[Seq[A]] = {
    val mauStrategy = implicitly[MauStrategy[A]]
    val riakKey = longKey(key, mauStrategy.typeName)
    val ids: Future[Seq[String]] =
      bucket.fetch(riakKey).map(_.map(_.as[String].split(",").toSeq).getOrElse(Seq.empty))
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

object MauDatabaseRiak {
  def apply(client: RiakClient, namespace: String)(implicit actorSystem: ActorSystem): MauDatabase =
    new MauDatabaseRiak(client, namespace)
}
