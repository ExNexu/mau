package mau

trait Model[A] {
  self: A ⇒

  def id: Option[Id]
  def withId(id: Id): A
}
