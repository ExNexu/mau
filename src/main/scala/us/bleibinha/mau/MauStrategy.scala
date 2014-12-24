package us.bleibinha.mau

// TODO: Require JsonFormat here and remove from MauDatabase
trait MauStrategy[T <: Model] {

  def typeName: String

  def getKeys(obj: T): List[Key]
}
