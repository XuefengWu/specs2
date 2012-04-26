package org.specs2
package reporter

import org.specs2.internal.scalaz.{Tree, Reducer, Scalaz}
import  Scalaz._
import collection.Iterablex._
import html._
import data.Trees._
import data.Tuples._
import TableOfContents._
import main.Arguments
import specification._
import Statistics._
import Levels._
import SpecsArguments._
import scala.xml.NodeSeq

/**
 * The Html printer is used to create an Html report of an executed specification.
 * 
 * To do this, it uses a reducer to prepare print blocks with:
 * 
 * - the text to print
 * - the indentation level
 * - the statistics
 * - the current arguments to use
 *
 */
trait HtmlPrinter {

  /**
   * print a sequence of executed fragments for a given specification class into a html file
   * the name of the html file is the full class name
   */
  def print(spec: ExecutedSpecification)(implicit args: Arguments) = {
    val htmlFiles = createHtmlLinesFiles(spec)
    val toc = createToc(htmlFiles)
    htmlFiles.flatten map printHtml(toc, output)
  }

  /**
   * map the executed fragments to HtmlLines and sort them by destination file, one file per specification
   *
   * @return a Tree of HtmlLinesFile where the root is the parent specification and children are the included specifications
   */
  def createHtmlLinesFiles(spec: ExecutedSpecification): Tree[HtmlLinesFile] =
    reduce(spec) |> sortByFile(spec.name, parentLink = HtmlLink(spec.name, "", spec.name.name))

  /**
   * a function printing html lines to a file given:
   *
   * - the table of contents for the full document
   * - an output object responsible for printing each HtmlLine as xhtml
   */
  def printHtml(toc: TreeToc, output: =>HtmlReportOutput): HtmlLinesFile => HtmlFile = (file: HtmlLinesFile) => {
    HtmlFile(file.link.url, printHtml(output, file, toc.toTree(file.specId)))
  }

  /** @return a new HtmlReportOutput object creating html elements */
  def output: HtmlReportOutput = new HtmlResultOutput

  /**
   * @return a global toc from the Tree of html files
   */
  def createToc(htmlFiles: Tree[HtmlLinesFile])(implicit args: Arguments) = {
    def tocItems(tree: Tree[HtmlLinesFile]): NodeSeq = {
      val root = tree.rootLabel
      tocItemList(body    = root.printLines(output).xml,
                  url     = root.link.url,
                  id      = root.specId,
                  subTocs = Map(tree.subForest.map(subSpec => (subSpec.rootLabel.specId, tocItems(subSpec))):_*))
    }
    TreeToc(htmlFiles.rootLabel.specId, tocItems(htmlFiles))
  }

  /**
   * @return an HtmlReportOutput object containing all the html corresponding to the
   *         html lines to print  
   */  
  def printHtml(output: =>HtmlReportOutput, file: HtmlLinesFile, toc: NodeSeq): NodeSeq = file.print(output, toc).xml

  /**
   * Organize the fragments into blocks of html lines to print, grouping all the fragments found after a link
   * into a single block that will be reported on a different html page
   *
   * This works by using a List of HtmlLines as a stack where the head of the list is the current block of lines
   *
   * @return the HtmlLines to print
   */
  def reduce(spec: ExecutedSpecification): Seq[HtmlLine] = flatten(spec.fragments.reduceWith(reducer))

  /**
   * Sort HtmlLines into a Tree of HtmlLinesFile object where the tree represents the tree of included specifications
   *
   * The goal is to create a file per included specification and to use the Tree of files to create a table of contents for the root specification
   */
  def sortByFile(specName: SpecName, parentLink: HtmlLink) = (lines: Seq[HtmlLine]) => {
    lazy val start = HtmlLinesFile(specName, parentLink, Vector())
    lines.foldLeft (leaf(start).loc) { (res, cur) =>
      val updated = res.updateLabel(_.add(cur))
      // html lines for an included specification are placed into HtmlSpecStart and HtmlSpecEnd fragments
      cur match {
        case start @ HtmlSpecStart(s, st, l, a) if start.isIncludeLink =>
          updated.insertDownLast(leaf(HtmlLinesFile(s.specName, start.link.getOrElse(parentLink), List(start.unlink))))
        case HtmlSpecEnd(e, _, _, _) if e.specName == res.getLabel.specName => updated.getParent
        case other                                                          => updated
      }
    }.root.tree
  }

  /** flatten the results of the reduction to a seq of Html lines */
  private def flatten(results: (((Seq[HtmlLine], SpecStats), Levels[ExecutedFragment]), SpecsArguments[ExecutedFragment])): Seq[HtmlLine] = {
    val (prints, stats, levels, args) = results.flatten
    (prints zip stats.stats zip levels.levels zip args.nestedArguments) map {
      case (((t, s), l), a) => t.set(s, l.level, a)
    }
  }  
  
  private  val reducer = 
    HtmlReducer &&& 
    StatsReducer &&&
    LevelsReducer  &&&
    SpecsArgumentsReducer

  implicit object HtmlReducer extends Reducer[ExecutedFragment, Seq[HtmlLine]] {
    implicit override def unit(fragment: ExecutedFragment) = Seq(print(fragment))
    /** print an ExecutedFragment and its associated statistics */
    def print(fragment: ExecutedFragment): HtmlLine = fragment match {
      case start @ ExecutedSpecStart(_,_,_)       => HtmlSpecStart(start)
      case result @ ExecutedResult(_,_,_,_,_)     => HtmlResult(result)
      case text @ ExecutedText(s, _)              => HtmlText(text)
      case par @ ExecutedBr(_)                    => HtmlBr()
      case end @ ExecutedSpecEnd(_,_,s)           => HtmlSpecEnd(end, s)
      case fragment                               => HtmlOther(fragment)
    }
  }

}

case class HtmlFile(url: String, xml: NodeSeq) {
  def nonEmpty = xml.nonEmpty
}

/**
 * Table of contents, represented as a NodeSeq
 */
case class TreeToc(rootCode: SpecId, toc: NodeSeq) {
  /** @return a "tree" div to be used with jstree, focusing on the current section */
  def toTree = (currentCode: SpecId) =>
    <div id="tree">
      <ul>{toc}</ul>
      <script>{"""$(function () {	$('#tree').jstree({'core':{'initially_open':['"""+rootCode+"','"+currentCode+"""'], 'animation':200}, 'plugins':['themes', 'html_data']}); });"""}</script>
    </div>

}