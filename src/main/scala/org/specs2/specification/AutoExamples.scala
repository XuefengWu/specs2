package org.specs2
package specification

import io.FromSource._
import text.Trim._
import text._
import execute._
import matcher.MatchersImplicits._
import matcher._

/**
 * Create example descriptions by reading the corresponding line in the source file.
 *
 *     "1 example"     ^
 *     { 1 must_== 1 } ^
 *                     end
 *                 
 * will be simply reported as
 *     1 example
 *     + 1 must_== 1
 *                 
 * -In some cases, like the case where there is no text beginning the specification, the example code must fit on one line only*
 * 
 * The source dir is assumed to be "src/test/scala/" by default but this can be modified
 * by setting the "specs2.srcDir" System property
 *
 * This trait provides implicit definitions to create examples from:
 *  - boolean expressions
 *  - match results
 *  - results
 * 
 */
private[specs2]
trait AutoExamples extends AutoExamplesLowImplicits { this: FragmentsBuilder =>
  /** specific implicits for datatables */
  implicit def dataTableFragments[T](result: =>DecoratedResult[T]): Fragments = Fragments.create(dataTableExample(result))
  implicit def dataTableExample[T](result: =>execute.DecoratedResult[T]) = exampleFactory.newExample(EmptyMarkup(), result)

  /** this syntax allows to declare auto examples with { ... }.eg in mutable specifications */
  implicit def aDataTableExample[T](expression: =>execute.DecoratedResult[T]): ToDataTableExample[T] = new ToDataTableExample(expression)
  class ToDataTableExample[T](expression: =>DecoratedResult[T]) {
    def eg = dataTableExample(expression)
  }
  /** explicit call */
  def eg[T](expression: =>execute.DecoratedResult[T]): ToDataTableExample[T] = aDataTableExample(expression)

}

private[specs2]
trait AutoExamplesLowImplicits { this: FragmentsBuilder =>

  /** this implicit def is necessary when the expression is at the start of the spec */
  implicit def matchFragmentsFragment(expression: =>MatchResult[_]): MatchResultFragment = {
    new MatchResultFragment(() => createExampleFragment(expression.toResult, 13, -2, -2))
  }

  /** this implicit def is necessary when the expression is at the start of the spec */
  implicit def booleanFragmentsFragment(expression: =>Boolean): BooleanResultFragment =
    new BooleanResultFragment(() => createExampleFragment(toResult(expression), 13, -2, -2))

  /** this implicit def is necessary when the expression is at the start of the spec */
  def resultFragmentsFragment(expression: =>Result): ResultFragment =
    new ResultFragment(() => createExampleFragment(expression, 13, -2, -2))

  /**
   * this implicit def is necessary when the expression is at the start of the spec
   * The startDepth and offsets are special tweakings to make sure we get the right line in that specific case
   */
  implicit def matchFragments(expression: =>MatchResult[_]): Fragments = createExampleFragment(expression.toResult)

  /** this implicit def is necessary when the expression is at the start of the spec */
  implicit def booleanFragments(expression: =>Boolean): Fragments = createExampleFragment(toResult(expression))

  /** this implicit def is necessary when the expression is at the start of the spec */
  implicit def resultFragments(result: =>Result): Fragments = createExampleFragment(result)

  private[specs2] def createExampleFragment(result: =>Result, d: Int = 9, offset1: Int = -1,  offset2: Int = -1) =
    Fragments.create(exampleFactory.newExample(CodeMarkup(getDescription(depth = d, startOffset = offset1, endOffset = offset2)), result))

  /** get the description from the source file */
  private[specs2] def getDescription(depth: Int = 9, startOffset: Int = -1, endOffset: Int = -1) =
    getSourceCode(startDepth = depth, endDepth= depth + 3, startLineOffset = startOffset, endLineOffset = endOffset)

  private[specs2] lazy val exampleDepth = 10

  implicit def matchExample(expression: =>MatchResult[_]) : Example = createExample(expression.toResult)
  implicit def booleanExample(expression: =>Boolean)      : Example = createExample(toResult(expression))
  implicit def resultExample(expression: =>execute.Result): Example = createExample(expression)

  private[specs2] def createExample(expression: =>execute.Result, depth: Int = exampleDepth): Example =
    exampleFactory.newExample(CodeMarkup(getDescription(depth)), expression)

  implicit def aMatchResultExample(expression: =>MatchResult[_]): ToMatchResultExample = new ToMatchResultExample(expression)
  /** this syntax allows to declare auto examples with { ... }.eg in mutable specifications */
  class ToMatchResultExample(expression: =>MatchResult[_]) {
    def eg = createExample(expression.toResult, 11)
  }
  /** explicit call */
  def eg(expression: =>MatchResult[_]): Example = createExample(expression.toResult, 12)

  implicit def aBooleanExample(expression: =>Boolean): ToBooleanExample = new ToBooleanExample(expression)
  class ToBooleanExample(expression: =>Boolean) {
    def eg = createExample(toResult(expression), 11)
  }
  /**
   * explicit call.
   * The result type is different from the eg method to create examples in order to avoid an overloading error
   */
  def eg(expression: =>Boolean): Fragments = createExample(toResult(expression), 12)

  implicit def aResultExample(expression: =>execute.Result): ToResultExample = new ToResultExample(expression)
  class ToResultExample(expression: =>execute.Result) {
    def eg = createExample(expression, 11)
  }
  /**
   * explicit call
   * The result type is different from the eg method to create examples in order to avoid an overloading error
   */
  def eg(expression: =>execute.Result): Fragment = createExample(expression, 12)

  private[specs2] def getSourceCode(startDepth: Int = 9, endDepth: Int = 12, startLineOffset: Int = -1, endLineOffset: Int = -1): String = {
    val firstTry = getCodeFromTo(startDepth, endDepth, startLineOffset, endLineOffset)
    val code = firstTry match {
      case Right(c) => c
      case Left(e)  =>
        getCodeFromTo(startDepth, startDepth, startLineOffset, endLineOffset) match {
          case Right(r) => r
          case Left(l)  => e
        }
    }
    trimCode(code)
  }

  private[specs2] def trimCode(code: String) = {
    List("^", "bt", "t", "endp", "br", "end", "p", "^").foldLeft(code)(_.trim trimEnd _).
    trimEnclosing("{", "}").
    trimEnclosing("`", "`").
    removeFirst("`\\(.*\\)").trimFirst("`")
  }

  /**
   * This class is especially created when the first fragment of a specification is a match result (no text before)
   * The startDepth and offsets are special tweakings to make sure we get the right line in that specific case
   */
  class MatchResultFragment(val fs: () => Fragments) extends FragmentsFragment(fs()) with ExampleFragment

  /**
   * This class is especially created when the first fragment of a specification is a boolean result (no text before)
   * The startDepth and offsets are special tweakings to make sure we get the right line in that specific case
   */
  class BooleanResultFragment(val fs: () => Fragments) extends FragmentsFragment(fs()) with ExampleFragment

  /**
   * This class is especially created when the first fragment of a specification is a Result (no text before)
   * The startDepth and offsets are special tweakings to make sure we get the right line in that specific case
   */
  class ResultFragment(val fs: () => Fragments) extends FragmentsFragment(fs()) with ExampleFragment

  trait ExampleFragment {
    def fs: () => Fragments
    def ^[T](result: =>T)(implicit toResult: T => Result) = {
      new FragmentsFragment(fs().add(exampleFactory.newExample(CodeMarkup(getDescription(depth = 9)), toResult(result))))
    }
  }
}

/**
 * This trait can be used to deactivate the Boolean conversions to fragments and examples
 */
trait NoBooleanAutoExamples extends AutoExamples { this: FragmentsBuilder =>
  override def booleanFragmentsFragment(expression: =>Boolean): BooleanResultFragment = super.booleanFragmentsFragment(expression)
  override def booleanFragments(expression: =>Boolean) = super.booleanFragments(expression)
  override def booleanExample(expression: =>Boolean) = super.booleanExample(expression)
  override def aBooleanExample(expression: =>Boolean) = super.aBooleanExample(expression)
}

/**
 * This trait can be used to deactivate the Result conversions to fragments and examples
 */
trait NoResultAutoExamples extends AutoExamples { this: FragmentsBuilder =>
  override def resultFragmentsFragment(expression: =>execute.Result) = super.resultFragmentsFragment(expression)
  override def resultFragments(expression:         =>execute.Result) = super.resultFragments(expression)
  override def resultExample(expression:           =>execute.Result) = super.resultExample(expression)
  override def aResultExample(expression:          =>execute.Result) = super.aResultExample(expression)
}

/**
 * This trait can be used to deactivate the MatchResult conversions to fragments and examples
 */
trait NoMatchResultAutoExamples extends AutoExamples { this: FragmentsBuilder =>
  override def matchFragmentsFragment(expression: =>MatchResult[_]) = super.matchFragmentsFragment(expression)
  override def matchFragments(expression:         =>MatchResult[_]) = super.matchFragments(expression)
  override def matchExample(expression:           =>MatchResult[_]) = super.matchExample(expression)
  override def aMatchResultExample(expression:    =>MatchResult[_]) = super.aMatchResultExample(expression)
}

/**
 * This trait can be used to deactivate the DataTable conversions to fragments and examples
 */
trait NoDataTableAutoExamples extends AutoExamples { this: FragmentsBuilder =>
  override def dataTableFragments[T](result: =>DecoratedResult[T])            = super.dataTableFragments(result)
  override def dataTableExample[T](result: =>execute.DecoratedResult[T])      = super.dataTableExample(result)
  override def aDataTableExample[T](expression: =>execute.DecoratedResult[T]) = super.aDataTableExample(expression)
}

/**
 * This trait ca be used to deactivate all automatic conversions to examples
 */
trait NoAutoExamples extends NoBooleanAutoExamples with NoResultAutoExamples with NoMatchResultAutoExamples with NoDataTableAutoExamples {
  this: FragmentsBuilder =>
}