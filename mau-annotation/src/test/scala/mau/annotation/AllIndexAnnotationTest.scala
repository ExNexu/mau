package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.test._

class AllIndexAnnotationTest extends MauRedisSpec("AllIndexAnnotationTest", true) {

  describe("@allIndex class annotation") {
    it("should allow to find all instances") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val retrievedPeople = await(personMauRepo.findAll)
      retrievedPeople should be(Seq(savedPerson))
      val retrievedPerson = retrievedPeople(0)
      retrievedPerson.name should be(person.name)
    }

    it("should allow to delete all instances") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val deleteResult = await(personMauRepo.deleteAll)
      deleteResult should be(1)
      val retrievedPerson = await(personMauRepo.get(id))
      retrievedPerson should be(None)
    }

    it("should allow to count all instances") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val countResult = await(personMauRepo.countAll)
      countResult should be(1)
    }
  }

  @mauModel("Mau:Test:AllIndexAnnotationTest", false)
  @sprayJson
  @allIndex
  case class Person(
    id: Option[Id],
    name: String,
    age: Int)
}
