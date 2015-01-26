package mau.mauannotation

import scala.reflect.macros._
import scala.reflect.macros._

private[mauannotation] trait CustomIndexMethodCreator extends MacroHelper {
  self: MacroImpl ⇒

  val c: blackbox.Context
  import c.universe._

  def getCustomIndexInfos(compDeclOpt: Option[ModuleDef]): List[CustomIndexInfo] =
    compDeclOpt.fold(
      List.empty[CustomIndexInfo]
    )(
        getCustomIndexInfosFromComp
      )

  def getCustomIndexInfosFromComp(compDecl: ModuleDef): List[CustomIndexInfo] = {
    val q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }" = compDecl

    val className = tname.toTypeName

    val valDefs =
      body.collect {
        case valDef: ValDef ⇒ valDef
      }

    valDefs.flatMap { valDef ⇒
      val q"$mods val $pat = $customIndexExpr" = valDef

      val indexNameTree = mods.annotations.collectFirst {
        case q"new customIndex($indexNameTree)" ⇒
          indexNameTree
      }

      indexNameTree match {
        case Some(indexNameTree) ⇒
          customIndexExpr match {
            case q"CustomIndexDeclaration[..$tpts]($keySaveFunction, $keyGetFunction)" ⇒
              Some(getCustomIndexInfoFromFunctions(className, keySaveFunction, keyGetFunction, None))
            case q"CustomIndexDeclaration[..$tpts]($keySaveFunction, $keyGetFunction, $getFilterFunction)" ⇒
              // TODO getFilterFunction
              Some(getCustomIndexInfoFromFunctions(className, keySaveFunction, keyGetFunction, None))
            case _ ⇒
              None
          }
        case _ ⇒
          None
      }
    }
  }

  def getCustomIndexInfoFromFunctions(className: TypeName, keySaveFunction: Tree, keyGetFunction: Tree, getFilterFunction: Option[Tree]): CustomIndexInfo = {
    val q"$_ = $keyFunctionTree" = keySaveFunction
    val keyFunction = keyFunctionTree.asInstanceOf[Function]
    val indexMethods = getIndexMethods(className, keyGetFunction, getFilterFunction)
    CustomIndexInfo(keyFunction, indexMethods)
  }

  def getIndexMethods(className: TypeName, keyGetFunction: Tree, getFilterFunction: Option[Tree]): List[DefDef] = {
    val q"$_ = $keyGetFunctionTree" = keyGetFunction
    val q"(..$keyGetParams) => $keyGetExpr" = keyGetFunctionTree
    val paramsTermNames = keyGetParams.map(_.name)
      val find = q"""
          def findX(..$keyGetParams) = {
            val keys = $keyGetFunctionTree.apply(..$paramsTermNames)
            val keyContents = Future.sequence(
              keys map { key ⇒
                mauDatabase.getKeyContent[$className](
                  key
                )(
                  mauStrategy,
                  mauDeSerializer
                )
              }
            )
            keyContents.map(_.flatten.toSeq)
          }
        """
        List(find)
  }

  case class CustomIndexInfo(keyFunction: Function, indexMethods: List[DefDef])
}
