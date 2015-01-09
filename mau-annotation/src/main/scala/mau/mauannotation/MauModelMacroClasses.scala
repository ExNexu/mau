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
      case q"${ namespace: String }" :: Nil if namespace != "" ⇒
        MauInfo(Some(namespace))
      case q"${ namespace: String }" :: q"${ showGenerated: Boolean }" :: Nil if namespace != "" ⇒
        MauInfo(Some(namespace), Some(showGenerated))
      case q"${ showGenerated: Boolean }" :: Nil ⇒
        MauInfo(None, Some(showGenerated))
      case _ ⇒
        MauInfo()
    }
  }

  case class MauInfo(namespaceOpt: Option[String] = None, showGeneratedOpt: Option[Boolean] = None) {
    val namespace = namespaceOpt.getOrElse("Mau")
    val showGenerated = showGeneratedOpt.getOrElse(false)
  }

  case class DeconstructedMauModelClass(mods: Modifiers, className: TypeName, fields: List[ValDef], bases: List[Tree], body: List[Tree]) {

    val hasAllIndex = mods.annotations.collectFirst { case q"new allIndex()" ⇒ () }.isDefined

    val indexedFields =
      fields filter { field ⇒
        field.mods.annotations match {
          case q"new indexed()" :: _ ⇒ true
          case _                     ⇒ false
        }
      }

    val compoundIndexes =
      mods.annotations.collect {
        case q"new compoundIndex($indexNameTree, $indexFieldList)" ⇒
          val q"List(..$indexFieldTrees)" = indexFieldList
          val indexFields = indexFieldTrees map treeToString
          val indexName = treeToString(indexNameTree)
          DeconstructedCompoundIndex(indexName, indexFields)
      }

    def getFieldValDef(fieldName: String): ValDef =
      fields.collectFirst {
        case q"$mods val $tname: $tpt = $expr" if tname.toString == fieldName ⇒ q"val $tname: $tpt = $expr"
      }.get

    private def treeToString(tree: Tree) = s"$tree".replaceAll("\"", "") // TODO: meh
  }

  object DeconstructedMauModelClass {
    def apply(classDecl: ClassDef): DeconstructedMauModelClass = {
      val q"$mods class $className(..$fields) extends ..$bases { ..$body }" = classDecl
      DeconstructedMauModelClass(mods, className, fields, bases, body)
    }
  }

  case class DeconstructedCompoundIndex(name: String, fields: List[String])
}
