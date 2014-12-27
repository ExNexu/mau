package mau.mauannotation

import scala.reflect.macros._

private[mauannotation] trait MauModelMacroClassModifier {
  self: MauModelMacroImpl ⇒

  val c: blackbox.Context
  import c.universe._

  def modifyClass(classDecl: ClassDef) = {
    val q"case class $className(..$fields) extends ..$bases { ..$body }" = classDecl
    val basesWithModel = tq"Model[$className]" :: bases
    val withId = q"override def withId(id: Id) = copy(id = Some(id))"
    q"""
      case class $className(..$fields) extends ..$basesWithModel {
        ..$body
        $withId
      }
    """
  }
}
