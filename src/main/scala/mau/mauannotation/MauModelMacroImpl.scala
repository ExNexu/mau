package mau.mauannotation

import scala.reflect.macros._

private[mauannotation] abstract class MauModelMacroImpl extends MauModelMacroClasses with MauModelMacroClassModifier with MauModelMacroCompanionModifier {
  val c: blackbox.Context
  import c.universe._

  def generate(annottees: Seq[c.Tree]): c.Expr[Any] =
    annottees match {
      case (classDecl: ClassDef) :: Nil ⇒ modifiedDeclaration(classDecl)
      case (classDecl: ClassDef) :: (compDecl: ModuleDef) :: Nil ⇒ modifiedDeclaration(classDecl, Some(compDecl))
      case _ ⇒ c.abort(c.enclosingPosition, "Invalid annottee")
    }

  def modifiedDeclaration(classDecl: ClassDef, compDeclOpt: Option[ModuleDef] = None) = {
    val modifiedClassDecl = modifyClass(classDecl)
    val modifiedCompDecl = modifyCompanion(modifiedClassDecl, compDeclOpt)
    //println(modifiedClassDecl)
    //println(modifiedCompDecl)

    c.Expr(
      q"""
        $modifiedClassDecl
        $modifiedCompDecl
      """
    )
  }
}
