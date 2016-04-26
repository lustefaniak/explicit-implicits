# explicit-implicits

```
  import explicitImplicits._
  
  sealed trait H

  case class H1(name: String) extends H

  case class H2(otherName: String) extends H

  trait Writer[T] {
    def write(t: T): String
  }

  implicit val h1Writer = new Writer[H1] {
    override def write(t: H1): String = t.name
  }

  implicit val h2Writer = new Writer[H2] {
    override def write(t: H2): String = t.otherName
  }

  val h1 = H1("name")
  val h2 = H2("otherName")


  val writer: Writer[H] = deriveImplicits[H, Writer]

  assert(writer.write(h1) == h1.name, "Writer should use implicit h1Writer")
  assert(writer.write(h2) == h2.otherName, "Writer should use implicit h2Writer")
```  
