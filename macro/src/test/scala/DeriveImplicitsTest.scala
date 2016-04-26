import org.scalatest.{Matchers, FunSuite}

class DeriveImplicitsTest extends FunSuite with Matchers {

  test("Can derive implicit") {
    """
      |  import explicitImplicits._
      |
      |  sealed trait H
      |  case class H1(name: String) extends H
      |  case class H2(otherName: String) extends H
      |
      |
      |  trait JsonFormat[T] {
      |    def write(t: T): String
      |  }
      |  implicit val h1Writer = new JsonFormat[H1] {
      |    override def write(t: H1): String = t.name
      |  }
      |  implicit val h2Writer = new JsonFormat[H2] {
      |    override def write(t: H2): String = t.otherName
      |  }
      |
      |  val h1 = H1("name")
      |  val h2 = H2("otherName")
      |
      |  val jsonFormat: JsonFormat[H] = deriveImplicits[H, JsonFormat]
      |
      |  assert(jsonFormat.write(h1) == h1.name, "Writer should use implicit h1Writer")
      |  assert(jsonFormat.write(h2) == h2.otherName, "Writer should use implicit h2Writer")
      |
      |  val hs: List[H] = List(h1,h2)
      |  val labels = hs.map(jsonFormat.write)
      |
      |  assert(labels == List(h1.name, h2.otherName), "Writer should use both implicit writers")
    """.stripMargin should compile

  }

  test("If one implicit is missing it doesn't compile") {
    """
      |  import explicitImplicits._
      |
      |  sealed trait H
      |  case class H1(name: String) extends H
      |  case class H2(otherName: String) extends H
      |
      |  trait JsonFormat[T] {
      |    def write(t: T): String
      |  }
      |  implicit val h1Writer = new JsonFormat[H1] {
      |    override def write(t: H1): String = t.name
      |  }
      |
      |  val jsonFormat: JsonFormat[H] = deriveImplicits[H, JsonFormat]
      |
    """.stripMargin shouldNot compile
  }

  test("It works when there is only one instance of trait") {
    """
      |  import explicitImplicits._
      |
      |  sealed trait H
      |  case class H1(name: String) extends H
      |
      |  trait JsonFormat[T] {
      |    def write(t: T): String
      |  }
      |  implicit val h1Writer = new JsonFormat[H1] {
      |    override def write(t: H1): String = t.name
      |  }
      |
      |  val jsonFormat: JsonFormat[H] = deriveImplicits[H, JsonFormat]
      |
    """.stripMargin should compile
  }

  test("It works for method returning sealed type too") {
    pendingUntilFixed {

      """
        |  import explicitImplicits._
        |
        |  sealed trait H
        |  case class H1(name: String) extends H
        |
        |  trait JsonFormat[T] {
        |    def read(json:String): T
        |  }
        |  implicit val h1Writer = new JsonFormat[H1] {
        |    override def read(json:String): T = H1(json)
        |  }
        |
        |  val jsonFormat: JsonFormat[H] = deriveImplicits[H, JsonFormat]
        |
      """.stripMargin should compile
    }
  }
}
