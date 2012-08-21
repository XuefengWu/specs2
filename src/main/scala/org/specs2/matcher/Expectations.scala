package org.specs2
package matcher

import execute._

/**
 * This trait provides implicit definitions to transform any value into an Expectable
 */
trait Expectations extends CanBeEqual {
  /** describe a value with the aka method */
  implicit def describe[T](t: =>T): Descriptible[T] = new Descriptible(t)
  class Descriptible[T](value: =>T) {
    /**
     * @return an expectable with its toString method as an alias description
     *         this is useful to preserve the original value when the matcher using
     *         it is adapting the value
     */
    def aka: Expectable[T] = aka(value.toString)
    /** @return an expectable with an alias description */
    def aka(alias: =>String): Expectable[T] = createExpectable(value, alias)
    /** @return an expectable with an alias description, after the value string */
    def post(alias: =>String): Expectable[T] = as((_:String)+" "+alias)
    /** @return an expectable with an alias description, after the value string */
    def as(alias: String => String): Expectable[T] = createExpectable(value, alias)
    /** @return an expectable with a function to show the element T */
    def showAs(implicit show: T => String): Expectable[T] = {
      lazy val v = value
      createExpectableWithShowAs(v, show(v))
    }
  }

  /** @return an Expectable */
  def createExpectable[T](t: =>T): Expectable[T] = createExpectable(t, None)
  /** @return an Expectable with a description */
  def createExpectable[T](t: =>T, alias: =>String): Expectable[T] = createExpectable(t, Some(Expectable.aliasDisplay(alias)))
  /** @return an Expectable with a description function */
  def createExpectable[T](t: =>T, alias: String => String): Expectable[T] = createExpectable(t, Some(alias))
  /** @return an Expectable with a description function */
  def createExpectable[T](t: =>T, alias: Option[String => String]): Expectable[T] = Expectable(t, alias)
  /** @return an Expectable with a function to show the element T */
  def createExpectableWithShowAs[T](t: =>T, showAs: =>String): Expectable[T] = Expectable.createWithShowAs(t, showAs)

  /** this method can be overriden to throw exceptions when checking the match result */
  protected def checkFailure[T](m: MatchResult[T]) = {
    checkResultFailure(matchResultToResult(m))
    m
  }
  /** this method can be overriden to intercept the transformation of a MatchResult to a Result */
  protected def matchResultToResult[T](m: MatchResult[T]): Result = m.toResult
  /** this method can be overriden to throw exceptions when checking the result */
  protected def checkResultFailure(r: Result): Result = r
}
