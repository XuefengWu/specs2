package org.specs2
package execute

import Result.ResultFailureMonoid
import text.Quote._
import text.Plural._
import internal.scalaz._
import Scalaz._
/**
 * This trait adds some implicits to easily fold sequences of results
 */
trait ResultImplicits {

  implicit def verifyResultFunction[T, R : Executable](t: T => R) = new ResultFunctionVerification(t)

  class ResultFunctionVerification[T, R](t: T => R)(implicit toResult: R => Result) {

    /** apply the function to the value and convert to a Result */
    def apply(value: T) = toResult(t(value))

    /** @return the "and" of all results, stopping after the first failure */
    def forall[S <: Traversable[T]](seq: S) = {
      if (seq.isEmpty) StandardResults.success
      else {
        val (index, r) = seq.drop(1).foldLeft((0, apply(seq.head))) { case ((i, res), cur) =>
          if (res.isSuccess) (i + 1, apply(cur))
          else               (i, res)
        }
        lazy val failingElementMessage = "In the sequence "+q(seq.mkString(", "))+", the "+(index+1).th+" element is failing: "+r.message

        if (r.isSuccess) Success("All elements of "+q(seq.mkString(", "))+" are successful")
        else             Failure(failingElementMessage)
      }
    }

    /**
     * @return the aggregation of all results
     */
    def foreach[S <: Traversable[T]](seq: S) = {
      if (seq.isEmpty) StandardResults.success
      else             seq.drop(1).foldLeft(apply(seq.head)) { (res, cur) => res |+| apply(cur) }
    }

    /**
     * @return success if at least one result is a success
     */
    def atLeastOnce[S <: Traversable[T]](seq: S) = {
      if (seq.isEmpty) Failure("no result")
      else seq.drop(1).foldLeft(apply(seq.head)) { (res, cur) => if (res.isSuccess) res else apply(cur) }
    }
  }

}

object ResultImplicits extends ResultImplicits