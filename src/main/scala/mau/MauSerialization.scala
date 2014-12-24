package mau

import annotation.implicitNotFound

@implicitNotFound(msg = "Cannot find MauSerializer type class for ${A}")
trait MauSerializer[A] {
  def serialize(obj: A): String
}

@implicitNotFound(msg = "Cannot find MauSerializer type class for ${A}")
trait MauDeSerializer[A] {
  def deserialize(string: String): A
}
