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

  case class DeconstructedMauModelClass(annot: Tree, className: TypeName, fields: List[ValDef], bases: List[Tree], body: List[Tree]) {
    val indexedFields =
      fields filter { field ⇒
        field.mods.annotations match {
          case q"new indexed()" :: _ ⇒ true
          case _                     ⇒ false
        }
      }
  }

  object DeconstructedMauModelClass {
    def apply(classDecl: ClassDef): DeconstructedMauModelClass =
      try {
        val q"case class $className(..$fields) extends ..$bases { ..$body }" = classDecl
        DeconstructedMauModelClass(null, className, fields, bases, body)
      } catch {
        case _: MatchError ⇒ c.abort(c.enclosingPosition, "Annotation is only supported on a case class")
      }
  }
}

