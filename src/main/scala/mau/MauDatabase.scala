package mau

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait MauDatabase {

  protected implicit def ec: ExecutionContext

  def save[A <: Model[A]: MauStrategy: MauSerializer](obj: A): Future[A] = {
    // first delete, then persist, then add to keys
    val deleteOldObj = obj.id.fold(Future.successful(0L))(A ⇒ delete(obj))

    val persistedObj = deleteOldObj flatMap (A ⇒ persist(obj))

    persistedObj map { persistedObj ⇒
      val id = persistedObj.id.get
      val mauStrategy = implicitly[MauStrategy[A]]
      val keys = mauStrategy.getKeys(persistedObj)
      keys map { key ⇒
        addToKey(id, key, mauStrategy.typeName)
      }
      persistedObj
    }
  }

  def get[A <: Model[A]: MauStrategy: MauDeSerializer](id: Id): Future[Option[A]]

  def getKeyContent[A <: Model[A]: MauStrategy: MauDeSerializer](key: Key, filterFunc: Option[(A) ⇒ Boolean] = None): Future[Seq[A]] = {
    val pureKeyContent = getPureKeyContent(key)
    filterFunc match {
      case Some(filterFunc) ⇒ pureKeyContent.map(_.filter(filterFunc))
      case None             ⇒ pureKeyContent
    }
  }

  def delete[A <: Model[A]: MauStrategy: MauDeSerializer](id: Id): Future[Long] =
    get(id) flatMap {
      case Some(obj) ⇒ delete(obj)
      case _         ⇒ Future.successful(0)
    }

  def delete[A <: Model[A]: MauStrategy](obj: A): Future[Long] =
    obj.id match {
      case Some(id) ⇒
        // first remove from keys, then remove object
        val mauStrategy = implicitly[MauStrategy[A]]
        val keys = mauStrategy.getKeys(obj)
        val removalFromKeys = Future.sequence(
          keys map (key ⇒
            removeFromKey(id, key, mauStrategy.typeName)
          )
        )
        removalFromKeys flatMap (_ ⇒ remove(id))
      case None ⇒
        Future.successful(0)
    }

  protected def persist[A <: Model[A]: MauStrategy: MauSerializer](obj: A): Future[A]

  protected def remove[A <: Model[A]: MauStrategy](id: Id): Future[Long]

  protected def addToKey(id: Id, key: Key, typeName: String): Future[Long]

  protected def removeFromKey(id: Id, key: Key, typeName: String): Future[Int]

  protected def getPureKeyContent[A <: Model[A]: MauStrategy: MauDeSerializer](key: Key): Future[Seq[A]]

}

