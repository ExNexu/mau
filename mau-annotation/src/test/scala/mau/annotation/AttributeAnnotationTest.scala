package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.test._

class AttributeAnnotationTest extends MauRedisSpec("AttributeAnnotationTest", true) {

  describe("@attribute annotation") {
    it("should save and retrieve an attribute") {
      val personMauRepo = Person.mauRepository
      val owner = await(personMauRepo.save(Person("Hans")))
      val car = Car("BMW", owner.id.get)
      val retrievedOwner = await(car.owner)
      retrievedOwner should be(Some(owner))
    }
  }

  @mauModel("Mau:Test:AttributeAnnotationTest", true)
  @sprayJson
  case class Car(
    id: Option[Id],
    name: String,
    @attribute("Person") ownerId: Id)
  // Id ending is required, Person needs to be a MauModel, automatically indexed!?
  // @attribute("Person") secondOwner: Option[Id] // just standard option support
  // need constructor with (name, person) // returns Future[Car], saves person if not yet saved
  // lazy val owner: Future[Option[Person]]
  // if non-lazy: val owner: Option[Person]
  // if it's option, it's also just option

  @mauModel("Mau:Test:AttributeAnnotationTest", true)
  @sprayJson
  case class Person(
    id: Option[Id],
    name: String)
}
