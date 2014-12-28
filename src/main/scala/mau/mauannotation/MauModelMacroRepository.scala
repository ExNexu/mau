package mau.mauannotation

import scala.reflect.macros._

private[mauannotation] trait MauModelMacroRepository {
  self: MauModelMacroImpl ⇒

  val c: blackbox.Context
  import c.universe._

  def createRepositoryClass(deconstructedMauModelClass: DeconstructedMauModelClass) = {
    val className = deconstructedMauModelClass.className
    val indexedFields = deconstructedMauModelClass.indexedFields
    val findMethods = indexedFields map (getFindMethodForIndexedField(_, className))
    val deleteMethods = indexedFields map (getDeleteMethodForIndexedField(_, className))
    val countMethods = indexedFields map (getCountMethodForIndexedField(_, className))
    val allIndexMethods = getAllIndexMethods(deconstructedMauModelClass, className)
    val compoundIndexMethods = getCompoundIndexMethods(deconstructedMauModelClass, className)
    val generatedMethods = allIndexMethods ::: compoundIndexMethods ::: findMethods ::: deleteMethods ::: countMethods
    q"""
      final class MauRepository private[$className](val mauDatabase: MauDatabase) {
        def save(obj: $className) = mauDatabase.save(obj)(mauStrategy, mauSerializer, mauDeSerializer)
        def save(seq: Seq[$className]) = mauDatabase.save(seq)(mauStrategy, mauSerializer, mauDeSerializer)
        def get(id: Id) = mauDatabase.get(id)(mauStrategy, mauDeSerializer)
        def get(seq: Seq[Id]) = mauDatabase.get(seq)(mauStrategy, mauDeSerializer)
        def delete(id: Id) = mauDatabase.delete(id)(mauStrategy, mauDeSerializer)
        def delete(obj: $className) = mauDatabase.delete(obj)(mauStrategy)
        def delete(seq: Seq[Id]) = mauDatabase.delete(seq)(mauStrategy, mauDeSerializer)
        def delete(seq: Seq[$className])(implicit d: DummyImplicit) = mauDatabase.delete(seq)(mauStrategy)
        ..$generatedMethods
      }
    """
  }

  def getFindMethodForIndexedField(field: ValDef, className: TypeName) =
    getActionMethodForIndexedField(field, className, "find", "getKeyContent")

  def getDeleteMethodForIndexedField(field: ValDef, className: TypeName) =
    getActionMethodForIndexedField(field, className, "delete", "deleteKeyContent")

  def getCountMethodForIndexedField(field: ValDef, className: TypeName) =
    getActionMethodForIndexedField(field, className, "count", "countKeyContent")

  def getActionMethodForIndexedField(field: ValDef, className: TypeName, actionName: String, mauDatabaseMethodStr: String) = {
    val fieldName = field.name
    val fieldType = field.tpt
    val keyForIndexedField = getKeyForIndexedField(fieldName, q"value")
    val actionMethod = TermName(s"${actionName}By${fieldName.toString.capitalize}")
    val filterMethod = q"Some((potential: $className) ⇒ value.equals(potential.$fieldName))"
    val mauDatabaseMethod = TermName(mauDatabaseMethodStr)
    q"""
      def $actionMethod(value: $fieldType) =
        mauDatabase.$mauDatabaseMethod(
          $keyForIndexedField,
          $filterMethod
        )(
          mauStrategy,
          mauDeSerializer
        )
    """
  }

  def getAllIndexMethods(deconstructedMauModelClass: DeconstructedMauModelClass, className: TypeName) =
    if (deconstructedMauModelClass.hasAllIndex) {
      val findAll = q"""
          def findAll =
            mauDatabase.getKeyContent[$className](
              $allKey
            )(
              mauStrategy,
              mauDeSerializer
            )
        """
      val deleteAll = q"""
          def deleteAll =
            mauDatabase.deleteKeyContent[$className](
              $allKey
            )(
              mauStrategy,
              mauDeSerializer
            )
        """
      val countAll = q"""
          def countAll =
            mauDatabase.countKeyContent[$className](
              $allKey
            )(
              mauStrategy,
              mauDeSerializer
            )
        """
      List(findAll, deleteAll, countAll)
    } else
      Nil

  // TODO: Refactor
  def getCompoundIndexMethods(deconstructedMauModelClass: DeconstructedMauModelClass, className: TypeName) = deconstructedMauModelClass.compoundIndexes flatMap { compoundIndex ⇒
    val indexName = compoundIndex.name
    val fields = compoundIndex.fields
    val argumentFields = fields map deconstructedMauModelClass.getFieldValDef
    val fieldTermNames = fields.map(fieldName ⇒ TermName(s"$fieldName"))
    val findByMethodName = TermName(s"findBy$indexName")
    val valueFields = fields map (field ⇒
      RefTree(EmptyTree, TermName(field))
    )
    val keyForCompoundIndex = getKeyForCompoundIndex(fieldTermNames, valueFields)
    val filterMethodParts = fieldTermNames.zip(valueFields).map {
      case (fieldName, valueField) ⇒
        q"""
          $valueField.equals(potential.$fieldName)
        """
    }
    val filterMethod = q"""
        Some(
          (potential: $className) ⇒
            List(..$filterMethodParts).filter(_ == false).isEmpty
        )
      """
    val findBy = q"""
        def $findByMethodName(..$argumentFields) =
          mauDatabase.getKeyContent[$className](
            $keyForCompoundIndex,
            $filterMethod
          )(
            mauStrategy,
            mauDeSerializer
          )
      """
    List(findBy)
  }

  def createRepositoryVal(deconstructedMauModelClass: DeconstructedMauModelClass) =
    q"val mauRepository = new MauRepository(mauDatabase)"
}
