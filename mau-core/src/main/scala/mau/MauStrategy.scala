package mau

import annotation.implicitNotFound

@implicitNotFound(msg = "Cannot find MauStrategy type class for ${A}")
trait MauStrategy[A <: Model[A]] {

  def typeName: String

  def getKeys(obj: A): Set[Key]
}

trait ModifiableMauStrategy[A <: Model[A]] extends MauStrategy[A] {
  private var keyFunctions: Set[KeyFunction[A]] = Set.empty

  override def getKeys(obj: A): Set[Key] =
    keyFunctions flatMap (_(obj))

  def addKeyFunction(keyfun: KeyFunction[A]) {
    keyFunctions = keyFunctions + keyfun
  }

  def addKeyFunctions(newKeyFuns: Iterable[KeyFunction[A]]) {
    keyFunctions = keyFunctions ++ newKeyFuns
  }
}
