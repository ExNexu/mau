package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.test._

class ComplexAttributeAnnotationTest extends MauRedisSpec("ComplexAttributeAnnotationTest", true) {

  describe("complex @attribute annotation") {
    it("should save and retrieve an attribute of an attribute") {
      val personMauRepo = Person.mauRepo
      val salesContact = await(personMauRepo.save(Person("Hans", None)))
      val owner = await(personMauRepo.save(Person("Peter", salesContact.id)))
      val car = Car("BMW", owner.id.get)
      val retrievedOwner = await(car.owner)
      retrievedOwner should be(Some(owner))
      val retrievedSalesContact = await(retrievedOwner.get.salesContact)
      retrievedSalesContact should be(Some(salesContact))
    }
  }

  @mauModel("Mau:Test:ComplexAttributeAnnotationTest", false)
  @sprayJson
  case class Car(
    id: Option[Id],
    name: String,
    @indexed @attribute("Person") ownerId: Id)

  @mauModel("Mau:Test:ComplexAttributeAnnotationTest", false)
  @sprayJson
  case class Person(
    id: Option[Id],
    name: String,
    @attribute("Person") salesContactId: Option[Id])
}
