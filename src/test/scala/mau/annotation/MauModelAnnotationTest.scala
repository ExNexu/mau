package mau.mauannotation

import mau._
import mau.test._

@mauModel case class Person(id: Option[Id], name: String, age: Int)

class MauModelAnnotationTest extends MauSpec {

  describe("@mauModel annotation") {
    it("should create a Repository class") {
      val repoClass = new Person.Repository()
    }
  }
}

