package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.test._

class CustomIndexAnnotationTest extends MauRedisSpec("CustomIndexAnnotationTest", true) {

  describe("@customIndex annotation") {
    it("should allow to find an instance") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val retrievedPeople = await(personMauRepo.findX('H'))
      retrievedPeople should be(Seq(savedPerson))
      val retrievedPerson = retrievedPeople(0)
      retrievedPerson.name should be(person.name)
    }
  }

  @mauModel("Mau:Test:CustomIndexAnnotationTest", true)
  @sprayJson
  case class Person(
    id: Option[Id],
    name: String,
    age: Int)

  object Person {

    @customIndex("FirstLetterOfName")
    private val firstLetterOfNameIndex = CustomIndexDeclaration[Person, Char](
      keySaveFunction =
        (person: Person) ⇒ Set(s"firstLetterOfName=${person.name.headOption.getOrElse("")}"),
      keyGetFunction =
        (char: Char) ⇒ Set(s"firstLetterOfName=$char")
    )
  }
}
