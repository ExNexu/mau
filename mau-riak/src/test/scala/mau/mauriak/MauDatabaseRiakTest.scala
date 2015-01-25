package mau.mauriak

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import mau._
import mau.mausprayjson._
import mau.test._
import spray.json._
import spray.json.DefaultJsonProtocol._

class MauDatabaseRiakTest extends MauRiakSpec("MauDatabaseRiakTest") {
  import PersonProtocol._

  val _ = (personMauStrategy, personJsonFormat) // TODO: Implicit resolution, meh

  describe("MauDatabaseRiak") {

    it("should save and get an object") {
      val person = Person(None, "Name")
      val (savedPerson, readPerson) = saveAndGet(person)
      Some(savedPerson) should be(readPerson)
      person.name should be(readPerson.get.name)
    }

    it("should handle getting a nonexisting object") {
      val readPerson = await(mauDatabaseRiak.get[Person]("123"))
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
      val removeResult = await(mauDatabaseRiak.delete(savedPerson))
      removeResult should be(1L)
      val readPerson2 = await(mauDatabaseRiak.get[Person](id))
      readPerson2 should be(None)
    }

    it("should delete an object by its id") {
      val person = Person(None, "Name")
      val (savedPerson, readPerson) = saveAndGet(person)
      Some(savedPerson) should be(readPerson)
      val id = savedPerson.id.get
      val removeResult = await(mauDatabaseRiak.delete(id))
      removeResult should be(1L)
      val readPerson2 = await(mauDatabaseRiak.get[Person](id))
      readPerson2 should be(None)
    }

    it("should allow to save, delete and save again an object") {
      val person = Person(None, "Name")
      val (savedPerson, readPerson) = saveAndGet(person)
      val id = savedPerson.id.get
      val removeResult = await(mauDatabaseRiak.delete(id))
      removeResult should be(1L)
      val (savedPerson2, readPerson2) = saveAndGet(savedPerson)
      Some(savedPerson2) should be(readPerson2)
    }

    it("should handle deleting a nonexisting object") {
      val removeResult = await(mauDatabaseRiak.delete("123"))
      removeResult should be(0L)
    }

    it("should get an object from an index") {
      val person = Person(None, "one")
      val savedPerson = await(mauDatabaseRiak.save(person))
      val id = savedPerson.id.get
      val personsWithNameOne = await(mauDatabaseRiak.getKeyContent[Person]("name=one"))
      personsWithNameOne should be(List(savedPerson))
    }

    it("should handle getting a nonexisting object from an index") {
      val indexResult = await(mauDatabaseRiak.getKeyContent[Person]("123"))
      indexResult should be(Nil)
    }

    it("should get Nil from an empty index") {
      val personsWithNameOne = await(mauDatabaseRiak.getKeyContent[Person]("name=one"))
      personsWithNameOne should be(Nil)
    }

    it("should remove an object from an index when it is deleted") {
      val person = Person(None, "one")
      val savedPerson = await(mauDatabaseRiak.save(person))
      val id = savedPerson.id.get
      val personsWithNameOne = await(mauDatabaseRiak.getKeyContent[Person]("name=one"))
      personsWithNameOne should be(List(savedPerson))
      val removeResult = await(mauDatabaseRiak.delete(savedPerson))
      removeResult should be(1L)
      val personsWithNameOne2 = await(mauDatabaseRiak.getKeyContent[Person]("name=one"))
      personsWithNameOne2 should be(Nil)
    }

    it("should update an index when an object is updated") {
      val person = Person(None, "one")
      val savedPerson = await(mauDatabaseRiak.save(person))
      val id = savedPerson.id.get
      val personsWithNameOne = await(mauDatabaseRiak.getKeyContent[Person]("name=one"))
      personsWithNameOne should be(List(savedPerson))
      val updatedPerson = savedPerson.copy(name = "two")
      val savedUpdatedPerson = await(mauDatabaseRiak.save(updatedPerson))
      val personsWithNameOne2 = await(mauDatabaseRiak.getKeyContent[Person]("name=one"))
      personsWithNameOne2 should be(Nil)
      val personsWithNameTwo = await(mauDatabaseRiak.getKeyContent[Person]("name=two"))
      personsWithNameTwo should be(List(updatedPerson))
    }

  }

  private def saveAndGet(person: Person): (Person, Option[Person]) = {
    val savedPerson = await(mauDatabaseRiak.save(person))
    val id = savedPerson.id.get
    val readPerson = await(mauDatabaseRiak.get[Person](id))
    (savedPerson, readPerson)
  }

  val mauDatabaseRiak = MauDatabaseRiak(riakClient, namespace)

  case class Person(id: Option[Id], name: String) extends Model[Person] {
    override def withId(id: Id) = copy(id = Some(id))
  }

  object PersonProtocol {
    implicit val personJsonFormat = jsonFormat2(Person.apply)

    implicit object personMauStrategy extends MauStrategy[Person] {
      override val typeName = "Person"
      override def getKeys(person: Person): Set[Key] =
        Set(s"name=${person.name}")
    }
  }
}
