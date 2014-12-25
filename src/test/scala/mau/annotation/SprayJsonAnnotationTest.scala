package mau.annotation

import mau.test._
import spray.json._
import spray.json.DefaultJsonProtocol._

@json case class Person(name: String, age: Int)

class SprayJsonAnnotationTest extends MauSpec {

  describe("@json annotation") {
    it("should create a correct formatter for case classes") {
      val person = Person("Victor Hugo", 46)
      val json = person.toJson
      json === JsObject(
        "name" -> JsString("Victor Hugo"),
        "age" -> JsNumber(46)
      )
      json.convertTo[Person] should be(person)
    }
  }
}

