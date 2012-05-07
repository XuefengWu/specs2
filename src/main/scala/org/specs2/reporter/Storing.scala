package org.specs2
package reporter

import org.specs2.internal.scalaz._
import Scalaz._
import specification._
import NestedBlocks._
import collection.Iterablex._
import main.Arguments

/**
* This trait stores the results of an executed specification
* It also updates the statistics of:
*
*  - SpecStart fragments from SpecEnd fragments
*/
private[specs2]
trait Storing {
  /** @return a function storing ExecutedFragments */
  def store(implicit args: Arguments): ExecutingSpecification => ExecutingSpecification

}

private[specs2]
trait DefaultStoring extends Storing with Statistics with WithDefaultStatisticsRepository {

  def store(implicit args: Arguments): ExecutingSpecification => ExecutingSpecification = (spec: ExecutingSpecification) => {
    if (args.store.reset) repository.resetStatistics
    val fragmentsWithSpecStartUpdatedWithStatistics =
      associateStartEnd(statisticsTotals(spec.execute.fragments), updateStatsOnSpecStart) |> storeStatistics

    ExecutingSpecification.create(spec.name, fragmentsWithSpecStartUpdatedWithStatistics)
  }

  private def statisticsTotals(fragments: Seq[ExecutedFragment])(implicit args: Arguments) = {
    val totals = fragments zip fragments.reduceWith(StatisticsReducer).totals
    totals map (setStatsOnSpecEndFragments andThen executedFragmentsToSpecBlock)
  }

  /**
   * set the statistics on SpecEndFragments after they've been computed by the StatisticsReducer
   * Those statistics are updated from previously executed statistics to calculate trends
   */
  def setStatsOnSpecEndFragments(implicit args: Arguments) = (fs: (ExecutedFragment, Stats)) => fs match {
    case (ExecutedSpecEnd(n, l, s), stats) if !args.store.never => ExecutedSpecEnd(n, l, stats.updateFrom(repository.getStatistics(n.specName)))
    case (other, s)                                             => other
  }

  /**
   *  "associate" function to set up the statistics on the SpecStart fragments after they've been computed and set on SpecEnd fragments.
   */
  private def updateStatsOnSpecStart = (start: ExecutedFragment, end: ExecutedFragment) => {
    (start, end) match {
      case (ExecutedSpecStart(ns, ss, ls), ExecutedSpecEnd(ne, se, le)) => (ExecutedSpecStart(ns, se, le), ExecutedSpecEnd(ne, se, le))
      case other                                                        => (start, end)
    }
  }

  private def storeStatistics(implicit args: Arguments) = (fragments: Seq[ExecutedFragment]) => {
    if (args.store.never) fragments
    else {
      val fn = SpecsArguments.foldAll(fragments).fragmentAndSpecNames
      val results = fn collect { case (r @ ExecutedResult(_, _, _, _, _), name) => (name, r) } groupBy (_._1)
      results map storeResults
      // toList is called to "force" the view
      // the result of storeStats is returned because SpecStarts might have been updated from the repository
      fn.toList map storeStats
    }
  }
  /**
   * store the statistics:
   * - for SpecEnd -> put the stats in the repository
   * - for a SpecStart that's a link -> read the status of the previous execution
   */
  protected def storeStats = (fn: (ExecutedFragment, SpecName)) => {
    fn match {
      case (ExecutedSpecStart(start @ SpecStart(_,_,_), loc, st), _) if start.isSeeOnlyLink =>
        ExecutedSpecStart(start, loc, repository.getStatistics(start.specName).getOrElse(st))

      case (f @ ExecutedSpecEnd(end @ SpecEnd(_), loc, st), _) => repository.storeStatistics(end.specName, st); f
      case (other, name)                                       => other
    }
  }
  /**
   * store the results by spec name
   */
  protected def storeResults = (r: (SpecName, Seq[(SpecName, ExecutedResult)])) => {
    val (name, results) = r
    repository.storeResults(name, results.map(_._2))
  }
}
