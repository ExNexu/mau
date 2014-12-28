package mau.mauannotation

import scala.reflect.macros._

private[mauannotation] abstract class MauModelMacroImpl
  extends MauModelMacroClasses
  with MauModelMacroClassModifier
  with MauModelMacroCompanionModifier
  with MauModelMacroRepository {

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
    if (mauInfo.showGenerated) {
      println("________________________________________________")
      println("------------> GENERATED CASE CLASS <------------")
      println("¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯")
      println(modifiedClassDecl)
      println("________________________________________________")
      println("---------> GENERATED COMPANION OBJECT <---------")
      println("¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯")
      println(modifiedCompDecl)
    }

    c.Expr(
      q"""
        $modifiedClassDecl
        $modifiedCompDecl
      """
    )
  }
}
