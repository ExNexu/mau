package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.test._

class AttributeAnnotationTest extends MauRedisSpec("AttributeAnnotationTest", true) {

  describe("@attribute annotation") {
    it("should save and retrieve an attribute") {
      val personMauRepo = Person.mauRepo
      val owner = await(personMauRepo.save(Person("Hans")))
      val car = Car("BMW", owner.id.get, None, Nil)
      val retrievedOwner = await(car.owner)
      retrievedOwner should be(Some(owner))
    }

    it("should save and retrieve an optional attribute") {
      val personMauRepo = Person.mauRepo
      val owner = await(personMauRepo.save(Person("Hans")))
      val secondOwner = await(personMauRepo.save(Person("Peter")))
      val car = Car("BMW", owner.id.get, Some(secondOwner.id.get), Nil)
      val retrievedSecondOwner = await(car.secondOwner)
      retrievedSecondOwner should be(Some(secondOwner))
    }

    it("should save and retrieve a list attribute") {
      val personMauRepo = Person.mauRepo
      val owner = await(personMauRepo.save(Person("Hans")))
      val previousOwner1 = await(personMauRepo.save(Person("Peter")))
      val previousOwner2 = await(personMauRepo.save(Person("Sonja")))
      val previousOwners = List(previousOwner1, previousOwner2)
      val previousOwnerIds = previousOwners map (_.id.get)
      val car = Car("BMW", owner.id.get, None, previousOwnerIds)
      val retrievedPreviousOwners = await(car.previousOwners)
      retrievedPreviousOwners should be(previousOwners)
    }

    it("should be able to be indexed") {
      val personMauRepo = Person.mauRepo
      val owner = await(personMauRepo.save(Person("Hans")))
      val car = Car("BMW", owner.id.get, None, Nil)
      val carMauRepo = Car.mauRepo
      val savedCar = await(carMauRepo.save(car))
      val retrievedCar = await(carMauRepo.findByOwnerId(owner.id.get))
      retrievedCar should be(List(savedCar))
    }
  }

  @mauModel("Mau:Test:AttributeAnnotationTest", false)
  @sprayJson
  case class Car(
    id: Option[Id],
    name: String,
    //@attribute("Person")@indexed ownerId: Id,
    @indexed @attribute("Person") ownerId: Id,
    @attribute("Person") secondOwnerId: Option[Id],
    @attribute("Person") previousOwnersId: List[Id])

  @mauModel("Mau:Test:AttributeAnnotationTest", false)
  @sprayJson
  case class Person(
    id: Option[Id],
    name: String)
}
