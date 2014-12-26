package mau.mauannotation

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros._

object mauModelMacroInstance extends mauModelMacro

class mauModel extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro mauModelMacroInstance.impl
}

class mauModelMacro {
  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def modifiedDeclaration(classDecl: ClassDef, compDeclOpt: Option[ModuleDef] = None) = {
      val deconstructedMauModelClass = deconstructMauModelClass(classDecl)
      val repositoryDecl = mauModelRepositoryClass(deconstructedMauModelClass)
      val compDecl = modifiedCompanion(compDeclOpt, repositoryDecl, deconstructedMauModelClass.className)

      c.Expr(q"""
        $classDecl
        $compDecl
      """)
    }

    def deconstructMauModelClass(classDecl: ClassDef) = {
      try {
        val q"case class $className($idField, ..$fields) extends ..$bases { ..$body }" = classDecl
        DeconstructedMauModelClass(className, idField, fields)
      } catch {
        case _: MatchError ⇒ c.abort(c.enclosingPosition, "Annotation is only supported on a case class")
      }
    }

    def modifiedCompanion(compDeclOpt: Option[ModuleDef], repositoryDecl: ClassDef, className: TypeName) = {
      compDeclOpt map { compDecl ⇒
        val q"object $obj extends ..$bases { ..$body }" = compDecl
        q"""
          object $obj extends ..$bases {
            ..$body
            $repositoryDecl
          }
        """
      } getOrElse {
        q"object ${className.toTermName} { $repositoryDecl }"
      }
    }

    def mauModelRepositoryClass(deconstructedMauModelClass: DeconstructedMauModelClass) = {
      q"""
        class Repository {
        }
      """
    }

    case class DeconstructedMauModelClass(className: TypeName, idField: ValDef, fields: List[ValDef]) {
      //TODO: idField must be idField
    }

    annottees.map(_.tree) match {
      case (classDecl: ClassDef) :: Nil ⇒ modifiedDeclaration(classDecl)
      case (classDecl: ClassDef) :: (compDecl: ModuleDef) :: Nil ⇒ modifiedDeclaration(classDecl, Some(compDecl))
      case _                          ⇒ c.abort(c.enclosingPosition, "Invalid annottee")
    }
  }
}

