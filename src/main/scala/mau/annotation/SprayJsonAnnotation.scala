package mau.annotation

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros._

object jsonMacroInstance extends jsonMacro

class json extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro jsonMacroInstance.impl
}

class jsonMacro {
  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def extractClassNameAndFields(classDecl: ClassDef) = {
      try {
        val q"case class $className(..$fields) extends ..$bases { ..$body }" = classDecl
        (className, fields)
      } catch {
        case _: MatchError ⇒ c.abort(c.enclosingPosition, "Annotation is only supported on case class")
      }
    }

    def jsonFormatter(className: TypeName, fields: List[ValDef]) = {
      val fieldsLength = fields.length
      fieldsLength match {
        case 0 ⇒ c.abort(c.enclosingPosition, "Cannot create json formatter for case class with no fields")
        case _ ⇒ {
          val applyMethod = q"${className.toTermName}.apply"
          val jsonFormatMethodName = TermName(s"jsonFormat$fieldsLength")
          val jsonFormatMethod = q"$jsonFormatMethodName($applyMethod)"
          q"implicit val jsonAnnotationFormat: JsonFormat[$className] = $jsonFormatMethod"
        }
      }
    }

    def modifiedCompanion(compDeclOpt: Option[ModuleDef], format: ValDef, className: TypeName) = {
      compDeclOpt map { compDecl ⇒
        // Add the formatter to the existing companion object
        val q"object $obj extends ..$bases { ..$body }" = compDecl
        q"""
          object $obj extends ..$bases {
            ..$body
            $format
          }
        """
      } getOrElse {
        // Create a companion object with the formatter
        q"object ${className.toTermName} { $format }"
      }
    }

    def modifiedDeclaration(classDecl: ClassDef, compDeclOpt: Option[ModuleDef] = None) = {
      val (className, fields) = extractClassNameAndFields(classDecl)
      val format = jsonFormatter(className, fields)
      val compDecl = modifiedCompanion(compDeclOpt, format, className)

      // Return both the class and companion object declarations
      c.Expr(q"""
        $classDecl
        $compDecl
      """)
    }

    annottees.map(_.tree) match {
      case (classDecl: ClassDef) :: Nil ⇒ modifiedDeclaration(classDecl)
      case (classDecl: ClassDef) :: (compDecl: ModuleDef) :: Nil ⇒ modifiedDeclaration(classDecl, Some(compDecl))
      case _ ⇒ c.abort(c.enclosingPosition, "Invalid annottee")
    }
  }
}

