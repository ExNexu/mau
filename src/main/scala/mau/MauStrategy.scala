package mau

import annotation.implicitNotFound

@implicitNotFound(msg = "Cannot find MauStrategy type class for ${T}")
trait MauStrategy[T <: Model] {

  def typeName: String

  def getKeys(obj: T): List[Key]
}
