package mau.mauannotation

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros._

object MauModelMacroInstance extends mauModelMacro

class mauModel(namespace: String = "") extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro MauModelMacroInstance.impl
}

class indexed extends StaticAnnotation

class mauModelMacro {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Expr[Any] = {
    import c.universe._

    case class MauInfo(namespaceOpt: Option[String] = None) {
      val namespace = namespaceOpt.getOrElse("Mau")
    }

    val mauInfo = {
      val annotation = c.prefix.tree
      val q"new mauModel(..$fields)" = annotation
      fields match {
        case q"${namespace: String}" :: Nil if namespace != "" ⇒ MauInfo(Some(namespace))
        case _ ⇒ MauInfo()
      }
    }

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

    def modifiedDeclaration(classDecl: ClassDef, compDeclOpt: Option[ModuleDef] = None) = {
      val modifiedClassDecl = modifyClass(classDecl)
      val modifiedCompDecl = modifyCompanion(modifiedClassDecl, compDeclOpt)
      //println(modifiedClassDecl)
      //println(modifiedCompDecl)

      c.Expr(q"""
        $modifiedClassDecl
        $modifiedCompDecl
      """)
    }

    def modifyClass(classDecl: ClassDef) = {
      val q"case class $className(..$fields) extends ..$bases { ..$body }" = classDecl
      val basesWithModel = tq"Model[$className]" :: bases
      val withId = q"override def withId(id: Id) = copy(id = Some(id))"
      q"""
        case class $className(..$fields) extends ..$basesWithModel {
          ..$body
          $withId
        }
      """
    }

    def modifyCompanion(classDecl: ClassDef, compDeclOpt: Option[ModuleDef]) = {
      val deconstructedMauModelClass = deconstructMauModelClass(classDecl)
      val className = deconstructedMauModelClass.className
      val sprayJsonFormat = createSprayJsonFormat(deconstructedMauModelClass)
      val mauSerialization = createMauSerialization(deconstructedMauModelClass)
      val mauStrategy = createMauStrategy(deconstructedMauModelClass)
      val repositoryClass = createRepositoryClass(deconstructedMauModelClass)
      val mauDatabase = createMauDatabase(deconstructedMauModelClass, mauInfo)
      val repositoryVal = createRepositoryVal(deconstructedMauModelClass)
      val companionBodyAddition =
        q"""
          ..$additionalCompanionImports
          $sprayJsonFormat
          ..$mauSerialization
          $mauStrategy
          $repositoryClass
          $mauDatabase
          $repositoryVal
        """
      compDeclOpt.fold(
        q"object ${className.toTermName} { ..$companionBodyAddition }"
      ) { compDecl ⇒
          val q"object $obj extends ..$bases { ..$body }" = compDecl
          q"""
            object $obj extends ..$bases {
              ..$body
              ..$companionBodyAddition
            }
          """
        }
    }

    def getMauInfo(moduleDecl: ModuleDef) = {
      val q"object $obj extends ..$bases { ..$body }" = moduleDecl
      val namespaceOpt = body collectFirst {
        case q"val namespace = $namespace" ⇒ namespace
      } map (_.toString)
      MauInfo(namespaceOpt)
    }

    def deconstructMauModelClass(classDecl: ClassDef) = {
      try {
        val q"case class $className(..$fields) extends ..$bases { ..$body }" = classDecl
        DeconstructedMauModelClass(null, className, fields, bases, body)
      } catch {
        case _: MatchError ⇒ c.abort(c.enclosingPosition, "Annotation is only supported on a case class")
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
          q"private val jsonFormat = $jsonFormatMethod"
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
          def get(id: Id) = mauDatabase.get(id)(mauStrategy, mauDeSerializer)
          def delete(id: Id) = mauDatabase.delete(id)(mauStrategy, mauDeSerializer)
          def delete(obj: $className) = mauDatabase.delete(obj)(mauStrategy)
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
          mauDatabase.getKeyContent($fieldNameConstant + s"=$${value.hashCode}", $filterMethod)(mauStrategy, mauDeSerializer)
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

    def createRepositoryVal(deconstructedMauModelClass: DeconstructedMauModelClass) = {
      q"""
        val mauRepository = new MauRepository(mauDatabase)
      """
    }

    case class DeconstructedMauModelClass(annot: Tree, className: TypeName, fields: List[ValDef], bases: List[Tree], body: List[Tree]) {
      val indexedFields =
        fields filter { field ⇒
          field.mods.annotations match {
            case q"new indexed()" :: _ ⇒ true
            case _ ⇒ false
          }
        }
    }

    annottees match {
      case (classDecl: ClassDef) :: Nil ⇒ modifiedDeclaration(classDecl)
      case (classDecl: ClassDef) :: (compDecl: ModuleDef) :: Nil ⇒ modifiedDeclaration(classDecl, Some(compDecl))
      case _ ⇒ c.abort(c.enclosingPosition, "Invalid annottee")
    }
  }
}

