package mau.mauannotation

import scala.reflect.macros._

private[mauannotation] trait MacroHelper {
  val c: blackbox.Context
  import c.universe._

  val emptyQQuote = q""

  def treeToString(tree: Tree) = s"$tree".replaceAll("\"", "") // TODO: meh
}
