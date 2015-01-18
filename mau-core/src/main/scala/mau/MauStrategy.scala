package mau

import annotation.implicitNotFound

@implicitNotFound(msg = "Cannot find MauStrategy type class for ${A}")
trait MauStrategy[A <: Model[A]] {

  def typeName: String

  def getKeys(obj: A): Set[Key]
}

trait ModifiableMauStrategy[A <: Model[A]] extends MauStrategy[A] {
  private var keyMethods: Set[KeyMethod[A]] = Set.empty

  override def getKeys(obj: A): Set[Key] =
    keyMethods flatMap (_(obj))

  def addKeymethod(keyMethod: KeyMethod[A]) {
    keyMethods = keyMethods + keyMethod
  }

  def addKeymethods(newKeyMethods: Iterable[KeyMethod[A]]) {
    keyMethods = keyMethods ++ newKeyMethods
  }
}
