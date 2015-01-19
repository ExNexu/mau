package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.test._

class CompoundIndexAnnotationTest extends MauRedisSpec("CompoundIndexAnnotationTest", true) {

  describe("@compoundIndex class annotation") {
    it("should allow to find by compoundIndex") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val retrievedPeople = await(personMauRepo.findByNameAge("Hans", 27))
      retrievedPeople should be(Seq(savedPerson))
      val retrievedPerson = retrievedPeople(0)
      retrievedPerson.name should be(person.name)
    }

    it("should allow to delete by compoundIndex") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val deleteResult = await(personMauRepo.deleteByNameAge("Hans", 27))
      deleteResult should be(1)
      val retrievedPerson = await(personMauRepo.get(id))
      retrievedPerson should be(None)
    }

    it("should allow to count by compoundIndex") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val countResult = await(personMauRepo.countByNameAge("Hans", 27))
      countResult should be(1)
    }
  }

  @mauModel("Mau:Test:CompoundIndexAnnotationTest", false)
  @sprayJson
  @compoundIndex("NameAge", List("name", "age"))
  case class Person(
    id: Option[Id],
    name: String,
    age: Int)
}
