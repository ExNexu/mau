package mau

import annotation.implicitNotFound

@implicitNotFound(msg = "Cannot find MauStrategy type class for ${A}")
trait MauStrategy[A <: Model[A]] {

  def typeName: String

  def getKeys(obj: A): List[Key]
}
