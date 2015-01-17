package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.test._

class MauModelAnnotationTest extends MauRedisSpec("MauModelAnnotationTest", true) {

  describe("@mauModel annotation") {
    it("should allow to save, get and delete") {
      val personMauRepo = Person.mauRepository
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      savedPerson.id should be('defined)
      val id = savedPerson.id.get
      val retrievedPerson = await(personMauRepo.get(id))
      retrievedPerson should be(Some(savedPerson))
      retrievedPerson.get.name should be(person.name)
      val deleteResult = await(personMauRepo.delete(id))
      deleteResult should be(1)
      val retrievedPerson2 = await(personMauRepo.get(id))
      retrievedPerson2 should be(None)
    }

    it("should have a convenient constructor") {
      val person = Person("Hans", 27)
      person should be(Person(None, "Hans", 27))
    }

    describe("@indexed field annotation") {
      it("should allow to find by value") {
        val personMauRepo = Person.mauRepository
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
        val personMauRepo = Person.mauRepository
        val person = Person(None, "Hans", 27)
        val savedPerson = await(personMauRepo.save(person))
        val id = savedPerson.id.get
        val deleteResult = await(personMauRepo.deleteByName("Hans"))
        deleteResult should be(1)
        val retrievedPerson = await(personMauRepo.get(id))
        retrievedPerson should be(None)
      }

      it("should allow to count by value") {
        val personMauRepo = Person.mauRepository
        val person = Person(None, "Hans", 27)
        val savedPerson = await(personMauRepo.save(person))
        val id = savedPerson.id.get
        val countResult = await(personMauRepo.countByName("Hans"))
        countResult should be(1)
      }
    }

    describe("@allIndex class annotation") {
      it("should allow to find all instances") {
        val personMauRepo = Person.mauRepository
        val person = Person(None, "Hans", 27)
        val savedPerson = await(personMauRepo.save(person))
        val id = savedPerson.id.get
        val retrievedPeople = await(personMauRepo.findAll)
        retrievedPeople should be(Seq(savedPerson))
        val retrievedPerson = retrievedPeople(0)
        retrievedPerson.name should be(person.name)
      }

      it("should allow to delete all instances") {
        val personMauRepo = Person.mauRepository
        val person = Person(None, "Hans", 27)
        val savedPerson = await(personMauRepo.save(person))
        val id = savedPerson.id.get
        val deleteResult = await(personMauRepo.deleteAll)
        deleteResult should be(1)
        val retrievedPerson = await(personMauRepo.get(id))
        retrievedPerson should be(None)
      }

      it("should allow to count all instances") {
        val personMauRepo = Person.mauRepository
        val person = Person(None, "Hans", 27)
        val savedPerson = await(personMauRepo.save(person))
        val id = savedPerson.id.get
        val countResult = await(personMauRepo.countAll)
        countResult should be(1)
      }
    }

    describe("@compoundIndex class annotation") {
      it("should allow to find by compoundIndex") {
        val personMauRepo = Person.mauRepository
        val person = Person(None, "Hans", 27)
        val savedPerson = await(personMauRepo.save(person))
        val id = savedPerson.id.get
        val retrievedPeople = await(personMauRepo.findByNameAge("Hans", 27))
        retrievedPeople should be(Seq(savedPerson))
        val retrievedPerson = retrievedPeople(0)
        retrievedPerson.name should be(person.name)
      }

      it("should allow to delete by compoundIndex") {
        val personMauRepo = Person.mauRepository
        val person = Person(None, "Hans", 27)
        val savedPerson = await(personMauRepo.save(person))
        val id = savedPerson.id.get
        val deleteResult = await(personMauRepo.deleteByNameAge("Hans", 27))
        deleteResult should be(1)
        val retrievedPerson = await(personMauRepo.get(id))
        retrievedPerson should be(None)
      }

      it("should allow to count by compoundIndex") {
        val personMauRepo = Person.mauRepository
        val person = Person(None, "Hans", 27)
        val savedPerson = await(personMauRepo.save(person))
        val id = savedPerson.id.get
        val countResult = await(personMauRepo.countByNameAge("Hans", 27))
        countResult should be(1)
      }
    }
  }

  @mauModel("Mau:Test:MauModelAnnotationTest", true)
  @sprayJson
  @allIndex
  @compoundIndex("NameAge", List("name", "age"))
  case class Person(
    id: Option[Id],
    @indexed name: String,
    age: Int)
}
