package mau.mauannotation

import scala.reflect.macros._
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
    val findAllMethod = getFindAllMethod(deconstructedMauModelClass, className)
    val generatedMethods = findAllMethod.toList ::: findMethods ::: deleteMethods ::: countMethods
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

  def getFindAllMethod(deconstructedMauModelClass: DeconstructedMauModelClass, className: TypeName) =
    if (deconstructedMauModelClass.hasAllIndex)
      Some(
        q"""
          def findAll =
            mauDatabase.getKeyContent[$className](
              $allKey
            )(
              mauStrategy,
              mauDeSerializer
            )
        """
      )
    else
      None

  def createRepositoryVal(deconstructedMauModelClass: DeconstructedMauModelClass) =
    q"val mauRepository = new MauRepository(mauDatabase)"
}
