package org.specs2
package io
import mutable._
import specification.{SpecificationStructure, Example}
import user.specification._

class FromSourceSpec extends Specification with FromSource {

  "General reading of code".txt
  "An expression can be read from a source file" in {
    examples(spec)(0).desc.toString must contain("1 must_== 1")
  }
  "An expression can be read from a source file even if it spans several lines" in {
    examples(spec)(1).desc.toString must contain("val a") and contain("hello world")
  }
  "Even if the specification name is different from the file name" in {
    examples(new DifferentSpecification)(0).desc.toString must contain("1 must_== 1")
  }
  "If the file is not found, the full path is shown to the user" in {
    other.NotFound.result.toString must be_==("No source file found at src/test/scala/org/specs2/io/other/FromSourceSpec.scala")
  }

  "Special examples cases".txt
  "If there is a function call to an example, the example description should be found" in {
    examples(spec)(2).desc.toString must contain("a call to an example")
  }
  "If the specification doesn't start with a text fragment, the example description should be found" in {
    checkExamples(spec2)
  }
  "If the specification doesn't end with an end fragment, the last example description should be found" in {
    checkExamples(spec3)
  }
  "If the mutable specification has a single expectation in a should block" in {
    checkExamples(spec4)
  }
  "If there is a function call to a ScalaCheck example, the example description should be found" in {
    examples(scalaCheckSpec)(0).desc.toString must contain("a call to an example")
  }
  "A ScalaCheck expression can be read from a source file even if it spans several lines" in {
    examples(scalaCheckSpec)(1).desc.toString must contain("check") and contain("a.size")
  }
  "A mutable specification can have auto-examples by annotating results with `.eg`" in {
    val ex = examples(mutableSpec)
    ex must have size (5)
    ex(0).desc.toString must contain("1 === 1")
    ex(1).desc.toString must contain("2 === 2")
    ex(3).desc.toString must contain("an example")
  }

  val spec           = new UserFromSourceSpecification
  val spec2          = new SpecificationWithNoStartingText
  val spec3          = new SpecificationWithNoStartingTextAndNoEnd
  val spec4          = new SpecificationWithAShouldBlockAndExamples
  val scalaCheckSpec = new UserFromSourceScalaCheckSpecification
  val mutableSpec    = new MutableSpecificationAutoExamples

  def checkExamples(spec: SpecificationStructure) = { (e: (Example, Int)) =>
    val index = e._2 + 1
    e._1 must contain(index+" must_== "+index) ^^ ((_:Example).desc.toString)
  }.forall(examples(spec).zipWithIndex)

  def examples(s: SpecificationStructure) = s.is.examples
}

package other {
  object NotFound extends org.specs2.Specification { def is = ""
    def result = success.desc
  }

}
