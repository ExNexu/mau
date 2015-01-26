package mau

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.reflect.ClassTag

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import us.bleibinha.akka.actor.locking._

trait MauDatabase {

  protected implicit def actorSystem: ActorSystem
  protected implicit val ec: ExecutionContext = actorSystem.dispatcher
  protected val timeoutDuration = 45 seconds
  protected implicit val timeout = Timeout(timeoutDuration)
  protected val lockActor = LockActor(timeoutDuration)
  private val dummyImplicit = new DummyImplicit()

  def save[A <: Model[A]: MauStrategy: MauSerializer: MauDeSerializer: ClassTag](obj: A): Future[A] = {
    // first remove from keys, then persist, then add to keys
    val action = () ⇒ {
      val mauStrategy = implicitly[MauStrategy[A]]

      val keysOfObj = mauStrategy.getKeys(obj)
      val keptKeys = obj.id.fold(Future.successful(Set.empty[Key]))(id ⇒ unsafeRemoveFromKeys(id, keysOfObj))
      val keysToAdd = keptKeys map (keysOfObj -- _)

      val persistedObj = keptKeys flatMap (_ ⇒ persist(obj))

      persistedObj flatMap { persistedObj ⇒
        val id = persistedObj.id.get
        keysToAdd flatMap { keysToAdd ⇒
          val keysFuture = Future.sequence(
            keysToAdd map { key ⇒
              addToKey(id, key, mauStrategy.typeName)
            }
          )
          keysFuture map (_ ⇒ persistedObj)
        }
      }
    }
    obj.id.fold(action.apply)(id ⇒ lockActor.ask(LockAwareRequest(id, action)).mapTo[A])
  }

  def save[A <: Model[A]: MauStrategy: MauSerializer: MauDeSerializer: ClassTag](seq: Seq[A]): Future[Seq[A]] = {
    val mauStrategy = implicitly[MauStrategy[A]]
    val mauSerializer = implicitly[MauSerializer[A]]
    val mauDeSerializer = implicitly[MauDeSerializer[A]]
    val classTag = implicitly[ClassTag[A]]
    Future.sequence(
      seq map (save(_)(mauStrategy, mauSerializer, mauDeSerializer, classTag))
    )
  }

  def get[A <: Model[A]: MauStrategy: MauDeSerializer](id: Id): Future[Option[A]]

  def get[A <: Model[A]: MauStrategy: MauDeSerializer](seq: Seq[Id]): Future[Seq[A]] = {
    val mauStrategy = implicitly[MauStrategy[A]]
    val mauDeSerializer = implicitly[MauDeSerializer[A]]
    val seqWithOpts = Future.sequence(
      seq map (get(_)(mauStrategy, mauDeSerializer))
    )
    seqWithOpts.map(_.flatten)
  }

  def getKeyContent[A <: Model[A]: MauStrategy: MauDeSerializer](key: Key, filterFunc: Option[FilterFunction[A]] = None): Future[Seq[A]] = {
    val pureKeyContent = getPureKeyContent(key)
    filterFunc match {
      case Some(filterFunc) ⇒ pureKeyContent.map(_.filter(filterFunc))
      case None             ⇒ pureKeyContent
    }
  }

  def delete[A <: Model[A]: MauStrategy: MauDeSerializer](id: Id): Future[Long] = {
    val action = () ⇒ { unsafeDelete(id) }
    lockActor.ask(LockAwareRequest(id, action)).mapTo[Long]
  }

  def delete[A <: Model[A]: MauStrategy: MauDeSerializer](obj: A): Future[Long] =
    obj.id match {
      case Some(id) ⇒
        delete(id)
      case None ⇒
        Future.successful(0)
    }

  def delete[A <: Model[A]: MauStrategy: MauDeSerializer](seq: Seq[Id]): Future[Long] = {
    val mauStrategy = implicitly[MauStrategy[A]]
    val mauDeSerializer = implicitly[MauDeSerializer[A]]
    val deletedObjCounts = Future.sequence(
      seq map (delete(_)(mauStrategy, mauDeSerializer))
    )
    deletedObjCounts.map(_.sum)
  }

  def delete[A <: Model[A]](seq: Seq[A])(implicit mauStrategy: MauStrategy[A], mauDeSerializer: MauDeSerializer[A], di: DummyImplicit): Future[Long] = {
    val deletedObjCounts = Future.sequence(
      seq map (delete(_)(mauStrategy, mauDeSerializer))
    )
    deletedObjCounts.map(_.sum)
  }

  def deleteKeyContent[A <: Model[A]: MauStrategy: MauDeSerializer](key: Key, filterFunc: Option[FilterFunction[A]] = None): Future[Long] = {
    val mauStrategy = implicitly[MauStrategy[A]]
    val mauDeSerializer = implicitly[MauDeSerializer[A]]
    val keyContent = getKeyContent(key, filterFunc)
    keyContent flatMap (delete(_)(mauStrategy, mauDeSerializer, dummyImplicit))
  }

  def countKeyContent[A <: Model[A]: MauStrategy: MauDeSerializer](key: Key, filterFunc: Option[FilterFunction[A]] = None): Future[Int] =
    getKeyContent(key, filterFunc).map(_.size)

  protected def persist[A <: Model[A]: MauStrategy: MauSerializer](obj: A): Future[A]

  protected def remove[A <: Model[A]: MauStrategy](id: Id): Future[Long]

  protected def addToKey(id: Id, key: Key, typeName: String): Future[Long]

  protected def removeFromKey(id: Id, key: Key, typeName: String): Future[Long]

  protected def getPureKeyContent[A <: Model[A]: MauStrategy: MauDeSerializer](key: Key): Future[Seq[A]]

  private def unsafeDelete[A <: Model[A]: MauStrategy: MauDeSerializer](id: Id): Future[Long] =
    unsafeRemoveFromKeys(id) flatMap (_ ⇒ remove(id))

  // returns a Set of keys the object is still present in
  private def unsafeRemoveFromKeys[A <: Model[A]: MauStrategy: MauDeSerializer](id: Id, keysToKeep: Set[Key] = Set.empty): Future[Set[Key]] =
    get(id) flatMap {
      case Some(obj) ⇒
        val mauStrategy = implicitly[MauStrategy[A]]
        val oldKeys = mauStrategy.getKeys(obj)
        val keysToRemove = oldKeys -- keysToKeep
        val keptKeys = oldKeys -- keysToRemove
        val removalFromKeys = Future.sequence(
          keysToRemove map (key ⇒
            removeFromKey(id, key, mauStrategy.typeName))
        )
        removalFromKeys map (_ ⇒ keptKeys)
      case _ ⇒ Future.successful(Set.empty)
    }
}
