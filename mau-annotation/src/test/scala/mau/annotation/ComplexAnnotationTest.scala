package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.test._

class ComplexAnnotationTest extends MauRedisSpec("ComplexAnnotationTest", true) {

  describe("@mauModel annotation with a complex object") {
    it("should allow to save and get") {
      val personMauRepo = Person.mauRepo
      val pet = Pet("Merlin")
      val person = Person(None, "Hans", pet)
      val savedPerson = await(personMauRepo.save(person))
      savedPerson.id should be('defined)
      val id = savedPerson.id.get
      val retrievedPerson = await(personMauRepo.get(id))
      retrievedPerson should be(Some(savedPerson))
      retrievedPerson.get.pet should be(pet)
    }
  }

  @mauModel("Mau:Test:ComplexAnnotationTest", true)
  @sprayJson
  case class Person(
    id: Option[Id],
    name: String,
    pet: Pet)

  object Person {
    implicit val petJsonFormat = Pet.petJsonFormat
  }

  case class Pet(name: String)

  object Pet {
    import spray.json._
    import spray.json.DefaultJsonProtocol._

    implicit val petJsonFormat = jsonFormat1(Pet.apply)
  }
}
