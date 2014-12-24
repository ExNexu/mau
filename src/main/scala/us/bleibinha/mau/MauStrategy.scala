package us.bleibinha.mau

trait MauStrategy[T <: Model] {

  def getKeys(obj: T): List[Key]
}
