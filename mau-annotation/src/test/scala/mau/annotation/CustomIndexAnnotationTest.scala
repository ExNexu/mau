package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.test._

class CustomIndexAnnotationTest extends MauRedisSpec("CustomIndexAnnotationTest", true) {

  describe("@customIndex annotation") {
    it("should allow to find instances") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val retrievedPeople = await(personMauRepo.findByFirstLetter('H'))
      retrievedPeople should be(Seq(savedPerson))
      val retrievedPerson = retrievedPeople(0)
      retrievedPerson.name should be(person.name)
    }

    it("should allow to delete instances") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val deleteResult = await(personMauRepo.deleteByFirstLetter('H'))
      deleteResult should be(1)
      val retrievedPerson = await(personMauRepo.get(id))
      retrievedPerson should be(None)
    }

    it("should allow to count instances") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val countResult = await(personMauRepo.countByFirstLetter('H'))
      countResult should be(1)
    }
  }

  describe("@customIndex annotation (tuple)") {
    it("should allow to find instances") {
      val personMauRepo = Person.mauRepo
      val person = Person(None, "Hans", 27)
      val savedPerson = await(personMauRepo.save(person))
      val id = savedPerson.id.get
      val retrievedPeople = await(personMauRepo.findByFirstLetterAge(('H', 27)))
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

    @customIndex("FirstLetter")
    private val firstLetterIndex = CustomIndexDeclaration[Person, Char](
      keySaveFunction =
        (person: Person) ⇒ Set(s"firstLetterOfName=${person.name.headOption.getOrElse("")}"),
      keyGetFunction =
        (char: Char) ⇒ Set(s"firstLetterOfName=$char")
    )

    @customIndex("FirstLetterAge")
    private val firstLetterAgeIndex = CustomIndexDeclaration[Person, (Char, Int)](
      keySaveFunction =
        (person: Person) ⇒ Set(s"firstLetter=${person.name.headOption.getOrElse("")}:age=${person.age}"),
      keyGetFunction =
        (charAge: (Char, Int)) ⇒ Set(s"firstLetter=${charAge._1}:age=${charAge._2}")
    )
  }
}
