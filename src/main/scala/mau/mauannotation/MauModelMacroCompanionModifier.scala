package mau.mauannotation

import scala.reflect.macros._
import scala.reflect.macros._

private[mauannotation] trait MauModelMacroCompanionModifier {
  self: MauModelMacroImpl ⇒

  val c: blackbox.Context
  import c.universe._

  val additionalCompanionImports =
    q"""
      import spray.json._
      import spray.json.DefaultJsonProtocol._
      import mau._
      import mau.mauspray._
      import mau.mauredis._

      import akka.actor.ActorSystem
      import redis.RedisClient
    """

  def modifyCompanion(classDecl: ClassDef, compDeclOpt: Option[ModuleDef]) = {
    val deconstructedMauModelClass = DeconstructedMauModelClass(classDecl)
    val className = deconstructedMauModelClass.className
    val sprayJsonFormat = createSprayJsonFormat(deconstructedMauModelClass)
    val mauSerialization = createMauSerialization(deconstructedMauModelClass)
    val mauStrategy = createMauStrategy(deconstructedMauModelClass)
    val repositoryClass = createRepositoryClass(deconstructedMauModelClass)
    val mauDatabase = createMauDatabase(deconstructedMauModelClass, mauInfo)
    val repositoryVal = createRepositoryVal(deconstructedMauModelClass)
    val companionBodyAddition =
      q"""
        $sprayJsonFormat
        ..$mauSerialization
        $mauStrategy
        $repositoryClass
        $mauDatabase
        $repositoryVal
      """
    compDeclOpt.fold(
      q"""
        object ${className.toTermName} {
          ..$additionalCompanionImports
          ..$companionBodyAddition
        }
      """
    ) { compDecl ⇒
        val q"object $obj extends ..$bases { ..$body }" = compDecl
        q"""
          object $obj extends ..$bases {
            ..$additionalCompanionImports
            ..$body
            ..$companionBodyAddition
          }
        """
      }
  }

  def createSprayJsonFormat(deconstructedMauModelClass: DeconstructedMauModelClass) = {
    val fieldsLength = deconstructedMauModelClass.fields.length
    val className = deconstructedMauModelClass.className
    fieldsLength match {
      case 0 ⇒ c.abort(c.enclosingPosition, "Cannot create json formatter for case class with no fields")
      case _ ⇒ {
        val applyMethod = q"${className.toTermName}.apply"
        val jsonFormatMethodName = TermName(s"jsonFormat$fieldsLength")
        val jsonFormatMethod = q"$jsonFormatMethodName($applyMethod)"
        q"implicit val jsonFormat = $jsonFormatMethod"
      }
    }
  }

  def createMauSerialization(deconstructedMauModelClass: DeconstructedMauModelClass) =
    q"""
      private val mauSerializer = jsonWriterToMauSerializer(jsonFormat)
      private val mauDeSerializer = jsonReaderToMauDeSerializer(jsonFormat)
    """

  def createMauStrategy(deconstructedMauModelClass: DeconstructedMauModelClass) = {
    val className = deconstructedMauModelClass.className
    val typeName = Constant(className.toString)
    val indexedFields = deconstructedMauModelClass.indexedFields
    val keyMethods = indexedFields map (getKeyMethodForIndexedField(_, className))
    q"""
      private object mauStrategy extends ModifiableMauStrategy[$className] {
        override val typeName = $typeName
        addKeymethods($keyMethods)
      }
    """
  }

  def getKeyMethodForIndexedField(field: ValDef, className: TypeName) = {
    val fieldName = field.name
    val fieldNameConstant = Constant(fieldName.toString)
    q"""
      (obj: $className) ⇒ List($fieldNameConstant + s"=$${obj.$fieldName.hashCode}")
    """
  }

  def createRepositoryClass(deconstructedMauModelClass: DeconstructedMauModelClass) = {
    val className = deconstructedMauModelClass.className
    val indexedFields = deconstructedMauModelClass.indexedFields
    val findMethods = indexedFields map (getFindMethodForIndexedField(_, className))
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
        ..$findMethods
      }
    """
  }

  def getFindMethodForIndexedField(field: ValDef, className: TypeName) = {
    val fieldName = field.name
    val fieldType = field.tpt
    val fieldNameConstant = Constant(fieldName.toString)
    val findMethod = TermName(s"findBy${fieldName.toString.capitalize}")
    val filterMethod = q"Some((potential: $className) ⇒ value.equals(potential.$fieldName))"
    q"""
      def $findMethod(value: $fieldType) =
        mauDatabase.getKeyContent(
          $fieldNameConstant + s"=$${value.hashCode}",
          $filterMethod
        )(
          mauStrategy,
          mauDeSerializer
        )
    """
  }

  def createMauDatabase(deconstructedMauModelClass: DeconstructedMauModelClass, mauInfo: MauInfo) = {
    val className = deconstructedMauModelClass.className
    val namespace = mauInfo.namespace
    val actorSystemName = Constant(s"$className-redis-actorSystem")
    val namespaceConstant = Constant(namespace)
    q"""
      private val mauDatabase = MauDatabaseRedis(RedisClient()(ActorSystem($actorSystemName)), $namespaceConstant)
    """
  }

  def createRepositoryVal(deconstructedMauModelClass: DeconstructedMauModelClass) =
    q"val mauRepository = new MauRepository(mauDatabase)"
}
