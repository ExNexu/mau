package us.bleibinha.mau

trait Model {
  def id: Option[Id]
  def withId(id: Id): this.type
}
