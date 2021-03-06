# explicit-implicits
[![Build Status](https://travis-ci.org/lustefaniak/explicit-implicits.svg?branch=master)](https://travis-ci.org/lustefaniak/explicit-implicits)
[ ![Download](https://api.bintray.com/packages/lustefaniak/maven/explicit-implicits/images/download.svg) ](https://bintray.com/lustefaniak/maven/explicit-implicits/_latestVersion)

Macro to turn multiple implicits of type `M[T1 <: T]` into single `M[T]` when `T` is sealed.

Requirement is, that all methods of `M[T]` need to have exactly one discriminator parameter of type `T`. 
 
```scala
trait M[T] {
 def do(t: T): String
 def doI(i: Int, t: T):String
 def doS(s: String)(t: T):String
} 
```

Macro would generate for you pattern matches using type of that discriminator parameter, and delegate execution to one of available in scope implicits `M[T1 <: T]`.
  
## Example Usage
```scala
sealed trait Product {
  def name: String
  def unitPrice: BigDecimal
}

case class Phone(name: String, unitPrice: BigDecimal) extends Product
case class Accessory(name: String, unitPrice: BigDecimal) extends Product

trait Pricer[T] {
  def calculatePrice(t: T, quantity: Int): BigDecimal
}

object StandardPrices {
  implicit val phonePricer = new Pricer[Phone] {
    def calculatePrice(p: Phone, quantity: Int): BigDecimal = p.unitPrice * quantity
  }
  implicit val accessoryPricer = new Pricer[Accessory] {
    def calculatePrice(p: Accessory, quantity: Int): BigDecimal = p.unitPrice * quantity
  }
}

object SecondAccessoryFor1Cent {
  implicit val accessoryPricer = new Pricer[Accessory] {
    def calculatePrice(p: Accessory, quantity: Int): BigDecimal = {
      val normalPriced = Math.ceil(quantity / 2.0).toInt
      val for1centPriced = quantity - normalPriced
      p.unitPrice * normalPriced + BigDecimal(0.01) * for1centPriced
    }
  }
}

object Main extends App {

  val standardPricer: Pricer[Product] = {
    import explicitImplicits._
    import StandardPrices.phonePricer
    import StandardPrices.accessoryPricer
    deriveFromImplicits[Product, Pricer]
  }

  val promotionalPricer: Pricer[Product] = {
    import explicitImplicits._
    import StandardPrices.phonePricer
    import SecondAccessoryFor1Cent.accessoryPricer
    deriveFromImplicits[Product, Pricer]
  }

  val shoppingCart: Map[Product, Int] = Map(
    Phone("Nexus 5", 100.0) -> 1,
    Accessory("Qi charger", 30) -> 3
  )

  val standardCartValues = shoppingCart.map(standardPricer.calculatePrice _ tupled)
  // standardCartValues: collection.immutable.Iterable[BigDecimal] = List(100.0, 90)
  val promotionalCartValues = shoppingCart.map(promotionalPricer.calculatePrice _ tupled)
  // promotionalCartValues: collection.immutable.Iterable[BigDecimal] = List(100.0, 60.01)

}
```

## Getting Started
  ```scala
  resolvers += Resolver.bintrayRepo("lustefaniak", "maven")
  
  libraryDependencies += "com.lustefaniak.explicitimplicits" %% "explicit-implicits" % "0.1.1"
  ```
  
 Or use [demo project](https://github.com/lustefaniak/explicit-implicits-demo)
 
 