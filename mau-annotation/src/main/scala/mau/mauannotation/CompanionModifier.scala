package mau.mauannotation

import scala.reflect.macros._

private[mauannotation] trait CompanionModifier extends MacroHelper {
  self: MacroImpl ⇒

  val c: blackbox.Context
  import c.universe._

  def createCompanionImports(sprayJson: Boolean) = {
    val sprayJsonImports =
      if (sprayJson)
        q"""
        import spray.json._
        import spray.json.DefaultJsonProtocol._
      """
      else
        emptyQQuote
    q"""
      ..$sprayJsonImports
      import mau._
      import mau.mausprayjson._
      import mau.mauredis._

      import akka.actor.ActorSystem
      import redis.RedisClient
    """
  }

  val allKey = Constant("all")

  def modifyCompanion(classDecl: ClassDef, compDeclOpt: Option[ModuleDef]) = {
    val deconstructedMauModelClass = DeconstructedMauModelClass(classDecl)
    val className = deconstructedMauModelClass.className
    val hasSprayJson = deconstructedMauModelClass.hasSprayJson
    val additionalCompanionImports = createCompanionImports(hasSprayJson)
    val convenientApplyMethod = createConvenientApplyMethod(deconstructedMauModelClass)
    val mauSerialization = createMauSerialization(deconstructedMauModelClass)
    val mauStrategy = createMauStrategy(deconstructedMauModelClass)
    val repositoryClass = createRepositoryClass(deconstructedMauModelClass)
    val mauDatabase = createMauDatabase(deconstructedMauModelClass, mauInfo)
    val repositoryVal = createRepositoryVal(deconstructedMauModelClass)
    val companionBodyAddition =
      q"""
        $convenientApplyMethod
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

  def createMauSerialization(deconstructedMauModelClass: DeconstructedMauModelClass) =
    if (deconstructedMauModelClass.hasSprayJson) {
      val sprayJsonFormat = createSprayJsonFormat(deconstructedMauModelClass)
      q"""
        ..$sprayJsonFormat
        private val mauSerializer = jsonWriterToMauSerializer(sprayJsonFormat)
        private val mauDeSerializer = jsonReaderToMauDeSerializer(sprayJsonFormat)
      """
    } else
      emptyQQuote

  def createSprayJsonFormat(deconstructedMauModelClass: DeconstructedMauModelClass) = {
    val fields = deconstructedMauModelClass.fields
    val className = deconstructedMauModelClass.className
    val classTermName = deconstructedMauModelClass.className.toTermName
    fields.length match {
      case 0 ⇒ c.abort(c.enclosingPosition, "Cannot create json formatter for case class with no fields")
      case _ ⇒ {
        val fieldNames =
          fields.map(field ⇒ Literal(Constant(field.name.toString)))
        val applyMethod = q"$classTermName.apply"
        val jsonFormatMethod = q"jsonFormat($applyMethod, ..$fieldNames)"
        q"implicit val sprayJsonFormat: JsonFormat[$className] = $jsonFormatMethod"
      }
    }
  }

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
