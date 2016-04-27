

object Main extends App {

  import explicitImplicits._

  sealed trait H

  case class H1(name: String) extends H

  case class H2(otherName: String) extends H

  //case class H3(absolutellyOtherName:Long) extends H

  trait JsonFormat[T] {
    def write(t: T, i: Int): String
  }

  implicit val h1Writer = new JsonFormat[H1] {
    override def write(t: H1, i: Int): String = t.name
  }

  implicit val h2Writer = new JsonFormat[H2] {
    override def write(t: H2, i: Int): String = t.otherName
  }

  val h1 = H1("name")
  val h2 = H2("otherName")

  val jsonFormat: JsonFormat[H] = deriveImplicits[H, JsonFormat]

  assert(jsonFormat.write(h1, 1) == h1.name, "Writer should use implicit h1Writer")
  assert(jsonFormat.write(h2, 2) == h2.otherName, "Writer should use implicit h2Writer")

}
