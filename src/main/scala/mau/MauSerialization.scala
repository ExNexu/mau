package mau

trait MauSerializer[T] {
  def serialize(obj: T): String
}

trait MauDeSerializer[T] {
  def deserialize(string: String): T
}
