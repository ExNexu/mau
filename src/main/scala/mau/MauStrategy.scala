package mau

trait MauStrategy[T <: Model] {

  def typeName: String

  def getKeys(obj: T): List[Key]
}
