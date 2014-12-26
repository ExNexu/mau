package mau

import annotation.implicitNotFound

@implicitNotFound(msg = "Cannot find MauStrategy type class for ${A}")
trait MauStrategy[A <: Model[A]] {

  def typeName: String

  def getKeys(obj: A): List[Key]
}

trait ModifiableMauStrategy[A <: Model[A]] extends MauStrategy[A] {
  private var keyMethods: List[KeyMethod[A]] = Nil

  override def getKeys(obj: A): List[Key] = keyMethods flatMap (_(obj))

  def addKeymethod(keyMethod: KeyMethod[A]) {
    keyMethods = keyMethod :: keyMethods
  }

  def addKeymethods(newKeyMethods: List[KeyMethod[A]]) {
    keyMethods = keyMethods ::: newKeyMethods
  }
}
