package mau.mauredis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import mau._
import mau.mauspray._
import mau.test._
import spray.json._
import spray.json.DefaultJsonProtocol._

class MauDatabaseRedisTest extends MauRedisSpec("MauDatabaseRedisTest") {
  import PersonProtocol._

  val x = (personMauStrategy, personJsonFormat) // TODO: Implicit resolution, meh

  describe("MauDatabaseRedis") {

    it("should save and get an object") {
      val person1 = Person(None, "Thomas")
      val savedObject = await(mauDatabaseRedis.save(person1))
      val id = savedObject.id.get
      val getObject = await(mauDatabaseRedis.get[Person](id))
      Some(savedObject) should be(getObject)
      person1.name should be(getObject.get.name)
    }

  }

  val mauDatabaseRedis = new MauDatabaseRedis(redisClient, namespace) {
    override protected def remove[A <: Model[A]: MauStrategy](id: Id): Future[Int] = Future.successful(0)

    override protected def addToKey(id: Id, key: Key): Future[Int] = Future.successful(0)

    override protected def removeFromKey(id: Id, key: Key): Future[Int] = Future.successful(0)

    override protected def getPureKeyContent[A <: Model[A]: MauStrategy: MauDeSerializer](key: Key): Future[List[A]] = Future.successful(Nil)
  }

}

case class Person(id: Option[Id], name: String) extends Model[Person] {
  override def withId(id: Id) = copy(id = Some(id))
}

object PersonProtocol {
  implicit val personJsonFormat = jsonFormat2(Person.apply)

  implicit object personMauStrategy extends MauStrategy[Person] {
    override val typeName = "Person"
    override def getKeys(obj: Person): List[Key] = Nil
  }
}

