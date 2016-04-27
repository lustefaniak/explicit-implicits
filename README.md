# explicit-implicits
[![Build Status](https://travis-ci.org/lustefaniak/explicit-implicits.svg?branch=master)](https://travis-ci.org/lustefaniak/explicit-implicits)

Macro to turn multiple implicits of `M[T1 <: T]` into single `M[T]` when `T` is sealed.

Requirement is, that all methods of `M[T]` need to have exactly one discriminator parameter of type `T`. 
 
```
trait M[T] {
 def do(t: T): String
 def doI(i: Int, t: T):String
 def doS(s: String)(t: T):String
} 
```

Macro would generate for you pattern matches using type of that discriminator parameter, and delegate execution to one of available in scope implicits `M[T1 <: T]`.

## Getting Started
  ```
  resolvers += Resolver.bintrayRepo("lustefaniak", "maven")
  
  libraryDependencies += "com.lustefaniak.com.lustefaniak.explicitimplicits" %% "explicit-implicits" % "0.1.0"
  ```
## Example Usage

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
