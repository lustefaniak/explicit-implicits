import org.scalatest.{Matchers, FunSuite}

class ShoppingCartDemoTest extends FunSuite with Matchers {

  test("Example Usage of Shopping Cart works") {
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

    val standardCartValue = shoppingCart.map(standardPricer.calculatePrice _ tupled)
    val promotionalCartValue = shoppingCart.map(promotionalPricer.calculatePrice _ tupled)

    assert(standardCartValue === List[BigDecimal](100, 90))
    assert(promotionalCartValue === List[BigDecimal](100, 60.01))

  }

}
