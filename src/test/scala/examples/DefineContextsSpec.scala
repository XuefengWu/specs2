package examples

import org.specs2._
import specification.{Before, BeforeExample, Context, Outside}
import org.scalacheck.{Prop, Gen}

/**
 * This specification shows various ways to setup contexts for examples.
 *
 * For more details @see org.specs2.guide.Structure
 */
class DefineContextsSpec extends Specification {

  /**
   * This specification uses a context class extending the `Before` trait.
   * It is also creating "fresh" variables for each example
   */
  class BeforeSpecification extends Specification { def is =
    "This is a list of examples"                                     ^
      "example1"                                                     ! clean().e1^
      "example2"                                                     ! clean().e2^
                                                                     end

    case class clean() extends Before {
      val aNewSystem = "a fresh value"
      def before = println("clean up before each example")

      def e1 = this { aNewSystem must_== "a fresh value" }
      def e2 = this { aNewSystem must_== "a fresh value" }
    }
  }

  /**
   * This specification uses an implicit context for each example
   */
  class BeforeWithImplicitContextSpecification extends Specification { def is = sequential^
    "This is a list of examples" ^
      "example1" ! { i += 1; i must_== 1 } ^
      "example2" ! { i += 1; i must_== 1 } ^
                                           end

    var i = 0
    implicit val before: Context = new Before { def before = i = 0 }
  }

  /**
   * This specification uses an implicit Outside context for each example
   */
  class OutsideWithImplicitContextSpecification extends Specification { def is =

    "This is a list of examples"                                     ^
      "example1"                                                     ! e1^
      "example2"                                                     ! e2^
                                                                     end

    implicit val outside: Outside[Int] = new Outside[Int] { def outside = 1 }

    def e1 = (i: Int) => i must_== 1
    def e2 = (i: Int) => i must_== 1
  }

  /**
   * This specification uses an implicit Outside context for each example and ScalaCheck properties
   */
  class OutsideWithImplicitScalaCheckContextSpecification extends Specification with ScalaCheck { def is =

    "This is a list of examples"                                     ^
      "example1"                                                     ! e1^
      "example2"                                                     ! e2^
      end

    implicit val outside: Outside[Int] = new Outside[Int] { def outside = 1 }

    def e1 = (i: Int) => Prop.forAll(Gen.choose(1, 10)) { (n: Int) => n must be_>=(i) }
    def e2 = (i: Int) => Prop.forAll(Gen.choose(1, 10)) { (n: Int) => n must be_>=(i) }
  }
  /**
   * Same thing as above for a mutable specification
   */
  class BeforeMutableSpecification extends mutable.Specification {
    "This is a list of examples" >> {
      "example1" >> new clean {
        aNewSystem must_== "a fresh value"
      }
      "example2" >> new clean {
        aNewSystem must_== "a fresh value"
      }
    }

    /** here we need a trait extending mutable.Before because the example body will be executed as a "delayed init"  section*/
    trait clean extends mutable.Before {
      lazy val aNewSystem = "a fresh value"
      def before = println("clean up before each example")
    }
  }

  /**
   * This specification uses the `BeforeExample` trait to execute some code before each example
   * by simply defining a `before` method
   */
  class BeforeExampleSpecification extends Specification with BeforeExample { def is =
    "This is a list of examples"                                     ^
      "example1"                                                     ! success^
      "example2"                                                     ! success^
                                                                     end
    def before = println("clean up before each example")
  }

  /**
   * This mutable specification also uses the `BeforeExample` trait
   */
  class BeforeExampleMutableSpecification extends mutable.Specification with BeforeExample {
    "This is a list of examples" >> {
      "example1"                 >> success
      "example2"                 >> success
    }

    def before = println("clean up before each example")
  }

  def println(s: String) = s // change this definition to see messages in the console

  def is = sequential^
           new BeforeSpecification ^
           new BeforeWithImplicitContextSpecification ^
           new OutsideWithImplicitContextSpecification ^
           new OutsideWithImplicitScalaCheckContextSpecification ^
           new BeforeMutableSpecification ^
           new BeforeExampleMutableSpecification ^
           new BeforeExampleSpecification
}
