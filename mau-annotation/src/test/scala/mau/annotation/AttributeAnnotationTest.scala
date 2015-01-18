package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.test._

class AttributeAnnotationTest extends MauRedisSpec("AttributeAnnotationTest", true) {

  describe("@attribute annotation") {
    it("should save and retrieve an attribute") {
      val personMauRepo = Person.mauRepository
      val owner = await(personMauRepo.save(Person("Hans")))
      val car = Car("BMW", owner.id.get, None)
      val retrievedOwner = await(car.owner)
      retrievedOwner should be(Some(owner))
    }

    it("should save and retrieve an optional attribute") {
      val personMauRepo = Person.mauRepository
      val owner = await(personMauRepo.save(Person("Hans")))
      val secondOwner = await(personMauRepo.save(Person("Peter")))
      val car = Car("BMW", owner.id.get, Some(secondOwner.id.get))
      val retrievedSecondOwner = await(car.secondOwner)
      retrievedSecondOwner should be(Some(secondOwner))
    }
  }

  @mauModel("Mau:Test:AttributeAnnotationTest", true)
  @sprayJson
  case class Car(
    id: Option[Id],
    name: String,
    @attribute("Person") ownerId: Id,
    @attribute("Person") secondOwnerId: Option[Id])

  @mauModel("Mau:Test:AttributeAnnotationTest", true)
  @sprayJson
  case class Person(
    id: Option[Id],
    name: String)
}
