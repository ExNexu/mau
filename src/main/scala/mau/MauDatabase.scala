package mau

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import spray.json.JsonReader
import spray.json.JsonWriter

trait MauDatabase {

  def save[T <: Model: MauStrategy: JsonWriter](obj: T)(implicit ec: ExecutionContext): Future[T] = {
    // first delete, then persist, then add to keys
    val deleteOldObj = obj.id.fold(Future.successful(0))(_ ⇒ delete(obj))

    val persistedObj = deleteOldObj flatMap (_ ⇒ persist(obj))

    persistedObj map { persistedObj ⇒
      val id = persistedObj.id.get
      val mauStrategy = implicitly[MauStrategy[T]]
      val keys = mauStrategy.getKeys(persistedObj)
      keys map { key ⇒
        addToKey(id, key)
      }
      persistedObj
    }
  }

  def get[T <: Model: MauStrategy: JsonReader](id: Id)(implicit ec: ExecutionContext): Future[Option[T]]

  def getKeyContent[T <: Model: MauStrategy: JsonReader](key: Key)(implicit ec: ExecutionContext): Future[List[T]]

  def delete[T <: Model: MauStrategy: JsonReader](id: Id)(implicit ec: ExecutionContext): Future[Int] =
    get(id) flatMap {
      case Some(obj) ⇒ delete(obj)
      case _ ⇒ Future.successful(0)
    }

  def delete[T <: Model: MauStrategy](obj: T)(implicit ec: ExecutionContext): Future[Int] =
    obj.id match {
      case Some(id) ⇒
        // first remove from keys, then remove object
        val mauStrategy = implicitly[MauStrategy[T]]
        val keys = mauStrategy.getKeys(obj)
        val removalFromKeys = Future.sequence(
          keys map (key ⇒
            removeFromKey(id, key)
          )
        )
        removalFromKeys flatMap (_ ⇒ remove(id))
      case None ⇒
        Future.successful(0)
    }

  protected def persist[T <: Model: MauStrategy: JsonWriter](obj: T)(implicit ec: ExecutionContext): Future[T]

  protected def remove[T <: Model: MauStrategy](id: Id)(implicit ec: ExecutionContext): Future[Int]

  protected def addToKey(id: Id, key: Key)(implicit ec: ExecutionContext): Future[Int]

  protected def removeFromKey(id: Id, key: Key)(implicit ec: ExecutionContext): Future[Int]

}

