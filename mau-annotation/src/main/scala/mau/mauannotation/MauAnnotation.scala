package mau.mauannotation

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros._

class indexed extends StaticAnnotation

class allIndex extends StaticAnnotation

class sprayJson extends StaticAnnotation

class compoundIndex(indexName: String, indexedFields: List[String]) extends StaticAnnotation

class mauModel(namespace: String = "", showGenerated: Boolean = false) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro MauModelMacroInstance.impl
}

private[mauannotation] object MauModelMacroInstance extends mauModelMacro

private[mauannotation] class mauModelMacro {
  def impl(c1: blackbox.Context)(annottees: c1.Tree*): c1.Expr[Any] = {
    val mauModelMacroImpl = new { val c: c1.type = c1 } with MauModelMacroImpl
    mauModelMacroImpl.generate(annottees)
  }
}
