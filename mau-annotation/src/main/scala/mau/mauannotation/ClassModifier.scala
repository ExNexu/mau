package mau.mauannotation

import scala.reflect.macros._

private[mauannotation] trait ClassModifier extends MacroHelper {
  self: MacroImpl ⇒

  val c: blackbox.Context
  import c.universe._

  def modifyClass(classDecl: ClassDef) = {
    val q"$mods class $className(..$fields) extends ..$bases { ..$body }" = classDecl
    val basesWithModel = tq"Model[$className]" :: bases
    val withId = q"override def withId(id: Id) = copy(id = Some(id))"
    val attributeAccessors = createAttributeAccessors(fields)
    q"""
      $mods class $className(..$fields) extends ..$basesWithModel {
        ..$body
        $withId
        ..$attributeAccessors
      }
    """
  }

  def createAttributeAccessors(fields: List[ValDef]) = {
    val attributeFields = getAttributeFields(fields)
    val additionalImports =
      if (attributeFields.nonEmpty)
        q"import scala.concurrent.Future"
      else
        emptyQQuote
    val attributeValList =
      attributeFields map { attributeField ⇒
        val attributeClassType =
          TypeName(treeToString(attributeField.attributeClassLiteral))
        val attributeClassTermName = attributeClassType.toTermName
        val fieldIdName = attributeField.fieldIdName
        val fieldName = attributeField.fieldName
        q"""
          lazy val $fieldName: Future[Option[$attributeClassType]] =
            $attributeClassTermName.mauRepository.get($fieldIdName)
        """
      }
    q"""
      $additionalImports
      ..$attributeValList
    """
  }

  def getAttributeFields(fields: List[ValDef]) =
    fields flatMap { field ⇒
      field.mods.annotations match {
        case q"new attribute($className)" :: _ ⇒
          Some(AttributedField(field, className.asInstanceOf[Literal]))
        case _ ⇒
          None
      }
    }

  case class AttributedField(field: ValDef, attributeClassLiteral: Literal) {
    val fieldIdName = field.name
    val fieldName =
      fieldIdName.toString match {
        case fieldNameString if fieldNameString.endsWith("Id") ⇒
          TermName(fieldNameString.substring(0, fieldNameString.length - 2))
        case _ ⇒
          throw new Exception(s"@attribute field [$fieldIdName]'s name must end with Id!")
      }
    val fieldType = field.tpt.asInstanceOf[Ident]
    if (fieldType.name.asInstanceOf[TypeName] != TypeName("Id"))
      throw new Exception(s"@attribute field [$fieldIdName] must be of type Id!")
  }
}