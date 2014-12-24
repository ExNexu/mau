package mau.mauspray

import mau._
import spray.json.JsonParser
import spray.json.JsonReader
import spray.json.JsonWriter

trait MauSerializationSpray {

  implicit def jsonWriterToMauSerializer[A: JsonWriter]: MauSerializer[A] = new MauSerializer[A] {
    val jsonWriter = implicitly[JsonWriter[A]]
    def serialize(obj: A): String = jsonWriter.write(obj).compactPrint
  }

  implicit def jsonReaderToMauDeSerializer[A: JsonReader]: MauDeSerializer[A] = new MauDeSerializer[A] {
    val jsonReader = implicitly[JsonReader[A]]
    def deserialize(string: String): A = {
      val json = JsonParser(string)
      jsonReader.read(json)
    }
  }
}
