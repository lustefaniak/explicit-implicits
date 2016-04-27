import org.scalatest.{Matchers, FunSuite}

class DeriveImplicitsCompilationTest extends FunSuite with Matchers {

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

}
