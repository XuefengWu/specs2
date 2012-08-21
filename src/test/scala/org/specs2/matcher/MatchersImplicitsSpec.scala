package org.specs2
package matcher

import mutable.Specification
import execute._

class MatchersImplicitsSpec extends Specification with ResultMatchers {
  "A sequence of match results can be implictly transformed to a single result" >> {
    ((List.empty[MatchResult[Any]]): Result) must beSuccessful
    ResultExecution.execute(Seq(1 === 1, 2 === 3): Result) must beFailing
  }
}
