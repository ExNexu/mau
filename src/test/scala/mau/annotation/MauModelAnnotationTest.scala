package mau.mauannotation

import scala.concurrent.ExecutionContext.Implicits.global

import mau._
import mau.mauredis._
import mau.mauspray._
import mau.test._
import spray.json._
import spray.json.DefaultJsonProtocol._

class MauModelAnnotationTest extends MauRedisSpec("MauModelAnnotationTest") {

  describe("@mauModel annotation") {
    it("should create a Repository class") {
      val mauDatabaseRedis = MauDatabaseRedis(redisClient, namespace)
      val repoClass = new Person.Repository(mauDatabaseRedis)
    }
  }
}

@mauModel case class Person(id: Option[Id], name: String, age: Int) extends Model[Person] {
  override def withId(id: Id) = copy(id = Some(id))
}

object Person {
  import PersonProtocol._

  val _ = (personMauStrategy, personJsonFormat) // TODO: Implicit resolution, meh
}

object PersonProtocol {
  implicit val personJsonFormat = jsonFormat3(Person.apply)

  implicit object personMauStrategy extends MauStrategy[Person] {
    override val typeName = "Person"
    override def getKeys(person: Person): List[Key] =
      List(s"name=${person.name}")
  }
}

