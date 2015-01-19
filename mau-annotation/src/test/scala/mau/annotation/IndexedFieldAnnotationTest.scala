package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.test._

class IndexedFieldAnnotationTest extends MauRedisSpec("IndexedFieldAnnotationTest", true) {

  describe("@indexed field annotation") {
    it("should allow to find by value") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val retrievedPeople = await(personMauRepo.findByName("Hans"))
      retrievedPeople should be(Seq(savedPerson))
      val retrievedPerson = retrievedPeople(0)
      retrievedPerson.name should be(person.name)
      await(personMauRepo.delete(id))
    }

    it("should allow to delete by value") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val deleteResult = await(personMauRepo.deleteByName("Hans"))
      deleteResult should be(1)
      val retrievedPerson = await(personMauRepo.get(id))
      retrievedPerson should be(None)
    }

    it("should allow to count by value") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val countResult = await(personMauRepo.countByName("Hans"))
      countResult should be(1)
    }
  }

  @mauModel("Mau:Test:IndexedFieldAnnotationTest", false)
  @sprayJson
  case class Person(
    id: Option[Id],
    @indexed name: String,
    age: Int)
}
