package us.bleibinha.mau

import scala.concurrent.Future

import spray.json.JsonReader
import spray.json.JsonWriter

trait MauDatabase {

  def save[T <: Model: MauStrategy: JsonWriter](obj: T): Future[T]

  def get[T <: Model: MauStrategy: JsonReader](id: Id): Future[Option[T]]

  def getKeyContent[T <: Model: MauStrategy: JsonReader](key: Key): Future[List[T]]

  def delete[T <: Model: MauStrategy: JsonReader](id: Id): Future[Int]
}


