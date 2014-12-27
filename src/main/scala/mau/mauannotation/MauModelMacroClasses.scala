package mau.mauannotation

import scala.reflect.macros._

private[mauannotation] trait MauModelMacroClasses {
  self: MauModelMacroImpl ⇒

  val c: blackbox.Context
  import c.universe._

  val mauInfo = {
    val annotation = c.prefix.tree
    val q"new mauModel(..$fields)" = annotation
    fields match {
      case q"${ namespace: String }" :: Nil if namespace != "" ⇒ MauInfo(Some(namespace))
      case _ ⇒ MauInfo()
    }
  }

  case class MauInfo(namespaceOpt: Option[String] = None) {
    val namespace = namespaceOpt.getOrElse("Mau")
  }

  case class DeconstructedMauModelClass(mods: Modifiers, className: TypeName, fields: List[ValDef], bases: List[Tree], body: List[Tree]) {
    val hasAllIndex = mods.annotations.collectFirst{ case q"new allIndex()" ⇒ () }.isDefined
    val indexedFields =
      fields filter { field ⇒
        field.mods.annotations match {
          case q"new indexed()" :: _ ⇒ true
          case _                     ⇒ false
        }
      }
  }

  object DeconstructedMauModelClass {
    def apply(classDecl: ClassDef): DeconstructedMauModelClass = {
      val q"$mods class $className(..$fields) extends ..$bases { ..$body }" = classDecl
      DeconstructedMauModelClass(mods, className, fields, bases, body)
    }
  }
}

