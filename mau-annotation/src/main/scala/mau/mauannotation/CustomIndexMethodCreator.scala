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

      val indexName = mods.annotations.collectFirst {
        case q"new customIndex($indexNameTree)" ⇒
          indexNameTree.asInstanceOf[Literal]
      }

      indexName match {
        case Some(indexName) ⇒
          customIndexExpr match {
            case q"CustomIndexDeclaration[..$tpts]($keySaveFunction, $keyGetFunction)" ⇒
              Some(getCustomIndexInfoFromFunctions(indexName, className, keySaveFunction, keyGetFunction, None))
            case q"CustomIndexDeclaration[..$tpts]($keySaveFunction, $keyGetFunction, $getFilterFunction)" ⇒
              // TODO getFilterFunction
              Some(getCustomIndexInfoFromFunctions(indexName, className, keySaveFunction, keyGetFunction, None))
            case _ ⇒
              None
          }
        case _ ⇒
          None
      }
    }
  }

  def getCustomIndexInfoFromFunctions(indexName: Literal, className: TypeName, keySaveFunction: Tree, keyGetFunction: Tree, getFilterFunction: Option[Tree]): CustomIndexInfo = {
    val q"$_ = $keyFunctionTree" = keySaveFunction
    val keyFunction = keyFunctionTree.asInstanceOf[Function]
    val indexMethods = getIndexMethods(indexName, className, keyGetFunction, getFilterFunction)
    CustomIndexInfo(keyFunction, indexMethods)
  }

  def getIndexMethods(indexName: Literal, className: TypeName, keyGetFunction: Tree, getFilterFunction: Option[Tree]): List[DefDef] = {
    val q"$_ = $keyGetFunctionTree" = keyGetFunction
    val q"(..$keyGetParams) => $keyGetExpr" = keyGetFunctionTree
    val paramsTermNames = keyGetParams.map(_.name)
    val indexNameString = treeToString(indexName)
    val find =
      getActionMethodForCustomIndex("find", indexNameString, TermName("getKeyContent"), className, keyGetParams, keyGetFunctionTree, paramsTermNames, TermName("flatten"))
    val delete =
      getActionMethodForCustomIndex("delete", indexNameString, TermName("deleteKeyContent"), className, keyGetParams, keyGetFunctionTree, paramsTermNames, TermName("sum"))
    val count =
      getActionMethodForCustomIndex("count", indexNameString, TermName("countKeyContent"), className, keyGetParams, keyGetFunctionTree, paramsTermNames, TermName("sum"))
    List(find, delete, count)
  }

  def getActionMethodForCustomIndex(actionName: String, indexName: String, mauDatabaseMethod: TermName, className: TypeName, argumentFields: List[ValDef], keyGetFunctionTree: Tree, paramsTermNames: List[TermName], flattenMethod: TermName) = {
    val actionMethodName = TermName(s"${actionName}By${indexName}")
    q"""
      def $actionMethodName(..$argumentFields) = {
        val keys = $keyGetFunctionTree.apply(..$paramsTermNames)
        val keyContents = Future.sequence(
          keys.toSeq map { key ⇒
            mauDatabase.$mauDatabaseMethod[$className](
              key
            )(
              mauStrategy,
              mauDeSerializer
            )
          }
        )
        keyContents.map(_.$flattenMethod)
      }
    """
  }

  case class CustomIndexInfo(keyFunction: Function, indexMethods: List[DefDef])
}
