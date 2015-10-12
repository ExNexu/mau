mau
===

_This is something I was working on but unfortunately lost the time to complete it. However the proof of concept works and it contains some interesting code working with Scala macros._

*mau is a framework for using key-value databases in Scala. It leverages Scala macros and default behavior to achieve more with less code.*

In the proof of concept I attached it for locally running redis and riak with spray-json. In the following I show some example code taken out of the test suite to show its capabilites. All test examples shown are taken from [here](https://github.com/ExNexu/mau/tree/master/mau-annotation/src/test/scala/mau/annotation).

## Examples

### Basic

```scala
@mauModel("Mau:Test:SimpleAnnotationTest", false)
@sprayJson
  case class Person(
    id: Option[Id],
    name: String,
    age: Int)
```

This generates code to save, get and delete `Person`s as seen here [SimpleAnnotiationTest.scala](https://github.com/ExNexu/mau/blob/master/mau-annotation%2Fsrc%2Ftest%2Fscala%2Fmau%2Fannotation%2FSimpleAnnotationTest.scala)

### Other examples

Other examples can be seen here: https://github.com/ExNexu/mau/tree/master/mau-annotation/src/test/scala/mau/annotation

I've added support for an index of all items of a type, attributes, complex annotiations, complex attributes, compound indexes, custom indexes, indexed fields and more.

## Sample output of generated code

Generated code can be shown by setting it to true in the `mauModel` annotiation, e.g. `@mauModel("Foo:Bar", true)`

```scala
________________________________________________
------------> GENERATED CASE CLASS <------------
¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯
@new sprayJson() case class Person extends Model[Person] with scala.Product with scala.Serializable {
  <caseaccessor> <paramaccessor> val id: Option[Id] = _;
  <caseaccessor> <paramaccessor> val name: String = _;
  <caseaccessor> <paramaccessor> val age: Int = _;
  def <init>(id: Option[Id], name: String, age: Int) = {
    super.<init>();
    ()
  };
  override def withId(id: Id) = copy(id = Some(id))
}
________________________________________________
---------> GENERATED COMPANION OBJECT <---------
¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯
object Person extends scala.AnyRef {
  def <init>() = {
    super.<init>();
    ()
  };
  import spray.json._;
  import spray.json.DefaultJsonProtocol._;
  import mau._;
  import mau.mausprayjson._;
  import mau.mauredis._;
  import akka.actor.ActorSystem;
  import redis.RedisClient;
  import scala.concurrent.Future;
  @new customIndex("FirstLetter") private val firstLetterIndex = CustomIndexDeclaration[Person, Char](keySaveFunction = ((person: Person) => Set(StringContext("firstLetterOfName=", "").s(person.name.headOption.getOrElse("")))), keyGetFunction = ((char: Char) => Set(StringContext("firstLetterOfName=", "").s(char))));
  @new customIndex("FirstLetterAge") private val firstLetterAgeIndex = CustomIndexDeclaration[Person, scala.Tuple2[Char, Int]](keySaveFunction = ((person: Person) => Set(StringContext("firstLetter=", ":age=", "").s(person.name.headOption.getOrElse(""), person.age))), keyGetFunction = ((charAge: scala.Tuple2[Char, Int]) => Set(StringContext("firstLetter=", ":age=", "").s(charAge._1, charAge._2))));
  def apply(name: String, age: Int): Person = new Person(None, name, age);
  implicit val sprayJsonFormat: JsonFormat[Person] = jsonFormat(Person.apply, "id", "name", "age");
  private val mauSerializer = jsonWriterToMauSerializer(sprayJsonFormat);
  private val mauDeSerializer = jsonReaderToMauDeSerializer(sprayJsonFormat);
  private object mauStrategy extends ModifiableMauStrategy[Person] {
    def <init>() = {
      super.<init>();
      ()
    };
    override val typeName = "Person";
    addKeyFunctions(scala.collection.immutable.Set());
    addKeyFunctions(scala.collection.immutable.List(((person: Person) => Set(StringContext("firstLetterOfName=", "").s(person.name.headOption.getOrElse("")))), ((person: Person) => Set(StringContext("firstLetter=", ":age=", "").s(person.name.headOption.getOrElse(""), person.age)))))
  };
  class MauRepository extends scala.AnyRef {
    <paramaccessor> val mauDatabase: MauDatabase = _;
    private[Person] def <init>(mauDatabase: MauDatabase) = {
      super.<init>();
      ()
    };
    import scala.reflect.ClassTag;
    private val classTag = implicitly[ClassTag[Person]];
    def save(obj: Person) = mauDatabase.save(obj)(mauStrategy, mauSerializer, mauDeSerializer, classTag);
    def save(seq: Seq[Person]) = mauDatabase.save(seq)(mauStrategy, mauSerializer, mauDeSerializer, classTag);
    def get(id: Id) = mauDatabase.get(id)(mauStrategy, mauDeSerializer);
    def get(seq: Seq[Id]) = mauDatabase.get(seq)(mauStrategy, mauDeSerializer);
    def delete(id: Id) = mauDatabase.delete(id)(mauStrategy, mauDeSerializer);
    def delete(obj: Person) = mauDatabase.delete(obj)(mauStrategy, mauDeSerializer);
    def delete(seq: Seq[Id]) = mauDatabase.delete(seq)(mauStrategy, mauDeSerializer);
    def delete(seq: Seq[Person])(implicit d: DummyImplicit) = mauDatabase.delete(seq)(mauStrategy, mauDeSerializer, d);
    def findByFirstLetter(char: Char) = {
      val keys = ((char: Char) => Set(StringContext("firstLetterOfName=", "").s(char))).apply(char);
      val keyContents = Future.sequence(keys.toSeq.map(((key) => mauDatabase.getKeyContent[Person](key)(mauStrategy, mauDeSerializer))));
      keyContents.map(((x$1) => x$1.flatten))
    };
    def deleteByFirstLetter(char: Char) = {
      val keys = ((char: Char) => Set(StringContext("firstLetterOfName=", "").s(char))).apply(char);
      val keyContents = Future.sequence(keys.toSeq.map(((key) => mauDatabase.deleteKeyContent[Person](key)(mauStrategy, mauDeSerializer))));
      keyContents.map(((x$2) => x$2.sum))
    };
    def countByFirstLetter(char: Char) = {
      val keys = ((char: Char) => Set(StringContext("firstLetterOfName=", "").s(char))).apply(char);
      val keyContents = Future.sequence(keys.toSeq.map(((key) => mauDatabase.countKeyContent[Person](key)(mauStrategy, mauDeSerializer))));
      keyContents.map(((x$3) => x$3.sum))
    };
    def findByFirstLetterAge(charAge: scala.Tuple2[Char, Int]) = {
      val keys = ((charAge: scala.Tuple2[Char, Int]) => Set(StringContext("firstLetter=", ":age=", "").s(charAge._1, charAge._2))).apply(charAge);
      val keyContents = Future.sequence(keys.toSeq.map(((key) => mauDatabase.getKeyContent[Person](key)(mauStrategy, mauDeSerializer))));
      keyContents.map(((x$4) => x$4.flatten))
    };
    def deleteByFirstLetterAge(charAge: scala.Tuple2[Char, Int]) = {
      val keys = ((charAge: scala.Tuple2[Char, Int]) => Set(StringContext("firstLetter=", ":age=", "").s(charAge._1, charAge._2))).apply(charAge);
      val keyContents = Future.sequence(keys.toSeq.map(((key) => mauDatabase.deleteKeyContent[Person](key)(mauStrategy, mauDeSerializer))));
      keyContents.map(((x$5) => x$5.sum))
    };
    def countByFirstLetterAge(charAge: scala.Tuple2[Char, Int]) = {
      val keys = ((charAge: scala.Tuple2[Char, Int]) => Set(StringContext("firstLetter=", ":age=", "").s(charAge._1, charAge._2))).apply(charAge);
      val keyContents = Future.sequence(keys.toSeq.map(((key) => mauDatabase.countKeyContent[Person](key)(mauStrategy, mauDeSerializer))));
      keyContents.map(((x$6) => x$6.sum))
    }
  };
  private val mauDatabase = MauDatabaseRedis(RedisClient()(ActorSystem("Person-redis-actorSystem")), "Mau:Test:CustomIndexAnnotationTest");
  val mauRepo = new MauRepository(mauDatabase)
}
```
