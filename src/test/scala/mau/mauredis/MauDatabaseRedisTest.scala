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

  val _ = (personMauStrategy, personJsonFormat) // TODO: Implicit resolution, meh

  describe("MauDatabaseRedis") {

    it("should save and get an object") {
      val person = Person(None, "Name")
      val (savedPerson, readPerson) = saveAndGet(person)
      Some(savedPerson) should be(readPerson)
      person.name should be(readPerson.get.name)
    }

    it("should handle getting a nonexisting object") {
      val readPerson = await(mauDatabaseRedis.get[Person]("123"))
      readPerson should be(None)
    }

    it("should update an object") {
      val person = Person(None, "Name")
      val (savedPerson, readPerson) = saveAndGet(person)
      Some(savedPerson) should be(readPerson)
      val updatedPerson = savedPerson.copy(name = "Name2")
      val (savedUpdatedPerson, readUpdatedPerson) = saveAndGet(updatedPerson)
      Some(savedUpdatedPerson) should be(readUpdatedPerson)
      updatedPerson.name should be(readUpdatedPerson.get.name)
    }

    it("should delete an object") {
      val person = Person(None, "Name")
      val (savedPerson, readPerson) = saveAndGet(person)
      Some(savedPerson) should be(readPerson)
      val id = savedPerson.id.get
      val removeResult = await(mauDatabaseRedis.delete(savedPerson))
      removeResult should be(1L)
      val readPerson2 = await(mauDatabaseRedis.get[Person](id))
      readPerson2 should be(None)
    }

    it("should delete an object by its id") {
      val person = Person(None, "Name")
      val (savedPerson, readPerson) = saveAndGet(person)
      Some(savedPerson) should be(readPerson)
      val id = savedPerson.id.get
      val removeResult = await(mauDatabaseRedis.delete(id))
      removeResult should be(1L)
      val readPerson2 = await(mauDatabaseRedis.get[Person](id))
      readPerson2 should be(None)
    }

    it("should handle deleting a nonexisting object") {
      val removeResult = await(mauDatabaseRedis.delete("123"))
      removeResult should be(0L)
    }

    it("should get an object from an index") {
      val person = Person(None, "one")
      val savedPerson = await(mauDatabaseRedis.save(person))
      val id = savedPerson.id.get
      val personsWithNameOne = await(mauDatabaseRedis.getKeyContent[Person]("name=one"))
      personsWithNameOne should be(List(savedPerson))
    }

    it("should handle getting a nonexisting object from an index") {
      val indexResult = await(mauDatabaseRedis.getKeyContent[Person]("123"))
      indexResult should be(Nil)
    }

    it("should get Nil from an empty index") {
      val personsWithNameOne = await(mauDatabaseRedis.getKeyContent[Person]("name=one"))
      personsWithNameOne should be(Nil)
    }

    it("should remove an object from an index when it is deleted") {
      val person = Person(None, "one")
      val savedPerson = await(mauDatabaseRedis.save(person))
      val id = savedPerson.id.get
      val personsWithNameOne = await(mauDatabaseRedis.getKeyContent[Person]("name=one"))
      personsWithNameOne should be(List(savedPerson))
      val removeResult = await(mauDatabaseRedis.delete(savedPerson))
      removeResult should be(1L)
      val personsWithNameOne2 = await(mauDatabaseRedis.getKeyContent[Person]("name=one"))
      personsWithNameOne2 should be(Nil)
    }

    it("update an index when an object is updated") {
      val person = Person(None, "one")
      val savedPerson = await(mauDatabaseRedis.save(person))
      val id = savedPerson.id.get
      val personsWithNameOne = await(mauDatabaseRedis.getKeyContent[Person]("name=one"))
      personsWithNameOne should be(List(savedPerson))
      val updatedPerson = savedPerson.copy(name = "two")
      val savedUpdatedPerson = await(mauDatabaseRedis.save(updatedPerson))
      val personsWithNameOne2 = await(mauDatabaseRedis.getKeyContent[Person]("name=one"))
      personsWithNameOne2 should be(Nil)
      val personsWithNameTwo = await(mauDatabaseRedis.getKeyContent[Person]("name=two"))
      personsWithNameTwo should be(List(updatedPerson))
    }

  }

  private def saveAndGet(person: Person): (Person, Option[Person]) = {
      val savedPerson = await(mauDatabaseRedis.save(person))
      val id = savedPerson.id.get
      val readPerson = await(mauDatabaseRedis.get[Person](id))
      (savedPerson, readPerson)
  }

  val mauDatabaseRedis = MauDatabaseRedis(redisClient, namespace)
}

case class Person(id: Option[Id], name: String) extends Model[Person] {
  override def withId(id: Id) = copy(id = Some(id))
}

object PersonProtocol {
  implicit val personJsonFormat = jsonFormat2(Person.apply)

  implicit object personMauStrategy extends MauStrategy[Person] {
    override val typeName = "Person"
    override def getKeys(person: Person): List[Key] =
      List(s"name=${person.name}")
  }
}

