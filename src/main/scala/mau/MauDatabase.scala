package mau

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait MauDatabase {

  protected implicit def ec: ExecutionContext

  def save[T <: Model: MauStrategy: MauSerializer](obj: T): Future[T] = {
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

  def get[T <: Model: MauStrategy: MauDeSerializer](id: Id): Future[Option[T]]

  def getKeyContent[T <: Model: MauStrategy: MauDeSerializer](key: Key, filterFunc: Option[(T) ⇒ Boolean] = None): Future[List[T]] = {
    val pureKeyContent = getPureKeyContent(key)
    filterFunc match {
      case Some(filterFunc) ⇒ pureKeyContent.map(_.filter(filterFunc))
      case None             ⇒ pureKeyContent
    }
  }

  def delete[T <: Model: MauStrategy: MauDeSerializer](id: Id): Future[Int] =
    get(id) flatMap {
      case Some(obj) ⇒ delete(obj)
      case _         ⇒ Future.successful(0)
    }

  def delete[T <: Model: MauStrategy](obj: T): Future[Int] =
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

  protected def persist[T <: Model: MauStrategy: MauSerializer](obj: T): Future[T]

  protected def remove[T <: Model: MauStrategy](id: Id): Future[Int]

  protected def addToKey(id: Id, key: Key): Future[Int]

  protected def removeFromKey(id: Id, key: Key): Future[Int]

  protected def getPureKeyContent[T <: Model: MauStrategy: MauDeSerializer](key: Key): Future[List[T]]

}

