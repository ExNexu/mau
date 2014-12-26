package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.test._

class MauModelAnnotationTest extends MauRedisSpec("MauModelAnnotationTest") {

  describe("@mauModel annotation") {
    it("should create a Repository class to allow save, get and delete") {
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
  }
}

@mauModel("MauTest") case class Person(id: Option[Id], name: String, age: Int)
