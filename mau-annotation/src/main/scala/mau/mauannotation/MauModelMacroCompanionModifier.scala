package mau.mauannotation

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
      import mau.mausprayjson._
      import mau.mauredis._

      import akka.actor.ActorSystem
      import redis.RedisClient
    """

  val allKey = Constant("all")

  def modifyCompanion(classDecl: ClassDef, compDeclOpt: Option[ModuleDef]) = {
    val deconstructedMauModelClass = DeconstructedMauModelClass(classDecl)
    val className = deconstructedMauModelClass.className
    val convenientApplyMethod = createConvenientApplyMethod(deconstructedMauModelClass)
    val sprayJsonFormat = createSprayJsonFormat(deconstructedMauModelClass)
    val mauSerialization = createMauSerialization(deconstructedMauModelClass)
    val mauStrategy = createMauStrategy(deconstructedMauModelClass)
    val repositoryClass = createRepositoryClass(deconstructedMauModelClass)
    val mauDatabase = createMauDatabase(deconstructedMauModelClass, mauInfo)
    val repositoryVal = createRepositoryVal(deconstructedMauModelClass)
    val companionBodyAddition =
      q"""
        $convenientApplyMethod
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
      """) { compDecl ⇒
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

  def createConvenientApplyMethod(deconstructedMauModelClass: DeconstructedMauModelClass) = {
    val className = deconstructedMauModelClass.className
    val fields = deconstructedMauModelClass.fields
    val fieldsWithoutId = fields.collect {
      case q"$mods val $tname: $tpt = $expr" if tname != TermName("id") ⇒ q"val $tname: $tpt = $expr"
    }
    val fieldsWithoutIdNames = fieldsWithoutId.map(_.name)
    q"""
      def apply(..$fieldsWithoutId): $className = new $className(None, ..$fieldsWithoutIdNames)
    """
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
    val indexedFieldKeyMethods = indexedFields map (getKeyMethodForIndexedField(_, className))
    val allIndexKeyMethod = getKeyMethodForAllIndex(deconstructedMauModelClass)
    val compoundIndexKeyMethods = getKeyMethodsForCompoundIndexes(deconstructedMauModelClass)
    val keyMethods = allIndexKeyMethod.toList ::: compoundIndexKeyMethods ::: indexedFieldKeyMethods
    q"""
      private object mauStrategy extends ModifiableMauStrategy[$className] {
        override val typeName = $typeName
        addKeymethods($keyMethods)
      }
    """
  }

  def getKeyMethodForIndexedField(field: ValDef, className: TypeName) = {
    val fieldName = field.name
    val keyForIndexedField = getKeyForIndexedField(fieldName, q"obj.$fieldName")
    q"(obj: $className) ⇒ List($keyForIndexedField)"
  }

  def getKeyForIndexedField(fieldName: TermName, value: RefTree) = {
    val fieldNameConstant = Constant(fieldName.toString)
    q"""
      $fieldNameConstant + s"=$${$value.hashCode}"
    """
  }

  def getKeyMethodForAllIndex(deconstructedMauModelClass: DeconstructedMauModelClass) =
    if (deconstructedMauModelClass.hasAllIndex) {
      val className = deconstructedMauModelClass.className
      Some(q"(obj: $className) ⇒ List($allKey)")
    } else
      None

  def getKeyForCompoundIndex(fieldNames: List[TermName], values: List[RefTree]) = {
    val fieldNamesAndValues = fieldNames.zip(values)
    val keys = fieldNamesAndValues map {
      case (fieldName, value) ⇒
        getKeyForIndexedField(fieldName, value)
    }
    q"""$keys.mkString(":")"""
  }

  def getKeyMethodsForCompoundIndexes(deconstructedMauModelClass: DeconstructedMauModelClass) =
    deconstructedMauModelClass.compoundIndexes map { compoundIndex ⇒
      val className = deconstructedMauModelClass.className
      val fields = compoundIndex.fields
      val fieldTermNames = fields.map(fieldName ⇒ TermName(s"$fieldName"))
      val valueFields = fields map (field ⇒
        RefTree(q"obj", TermName(field)))
      val keyForCompoundIndex = getKeyForCompoundIndex(fieldTermNames, valueFields)
      q"(obj: $className) ⇒ List($keyForCompoundIndex)"
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
}