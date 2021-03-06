package org.specs2
package reporter

import _root_.org.scalatools.testing.{ EventHandler, Logger, Event, Result }
import main.Arguments
import text._
import Trim._
import time._
import AnsiColors._
import execute.{ Success, Failure, Error, Skipped, Pending, DecoratedResult }
import specification._

/**
 * Reporter for the test interface defined for sbt
 * 
 * It prints out the result to the output defined by the sbt loggers
 * and publishes events to sbt event handlers
 */
class TestInterfaceReporter(val handler: EventHandler, val loggers: Array[Logger]) extends ConsoleReporter
  with HandlerEvents {

  override def textOutput = new TestInterfaceResultOutput(loggers)

  override def export(implicit args: Arguments): ExecutingSpecification => ExecutedSpecification = (spec: ExecutingSpecification) => {
    super.export(args)(spec)
    val executed = spec.execute
    executed.fragments foreach handleFragment(args)
    executed
  }

  protected def handleFragment(implicit args: Arguments): ExecutedFragment => ExecutedFragment = (f: ExecutedFragment) => {
    f match {
      case ExecutedResult(text: FormattedString, result: org.specs2.execute.Result, timer: SimpleTimer, _, _) => {
        def handleResult(res: org.specs2.execute.Result) {
          res match {
            case Success(text,_)             => handler.handle(succeeded(text))
            case r @ Failure(text, e, st, d) => handler.handle(failure(text, args.traceFilter(r.exception)))
            case r @ Error(text, e)          => handler.handle(error(text, args.traceFilter(r.exception)))
            case Skipped(text, _)            => handler.handle(skipped(text))
            case Pending(text)               => handler.handle(skipped(text))
            case DecoratedResult(t, r)       => handleResult(r)
          }
        }
        handleResult(result)
        f
      }
      case _                                 => f
    }
  }
}

class TestInterfaceResultOutput(val loggers: Array[Logger]) extends TextResultOutput with TestLoggers {
  private val buffer = new StringBuilder

  private var loggerNewLines = 0

  private def info(msg: String)(implicit args: Arguments) {
    val message = offset(msg)
    // if a newline has already been added by the logger, remove the first newline
    if (message.isEmpty) {
      logInfo(" ")
      loggerNewLines += 1
    }
    else if (message.startsWith("\n") && loggerNewLines > 0) {
      buffer.append(message.removeFirst("\n"))
      loggerNewLines = 0
    }
    else if (!message.isEmpty) {
      val all = buffer.toString + message
      val splitted = all.split("\n")
      buffer.clear

      // if the characters after the last newline are only whitespace
      // buffer them and only display what comes before
      splitted.lastOption.filter(_.forall(_ == ' ')).map { last =>
        if (splitted.dropRight(1).nonEmpty) {
          buffer.append(last)
          splitted.dropRight(1).foreach(logInfo)
        } else logInfo(all)
      }.getOrElse(logInfo(all))
      loggerNewLines += 1
    }
  }

  private def flushInfo(implicit args: Arguments) = {
    // only flush the buffer if it is non empty, otherwise that would create an unnecessary newline
    if (buffer.nonEmpty) logInfo(buffer.toString)
  }

  override def printSpecStartName(message: String, stats: Stats)(implicit args: Arguments)  = {
    info(message)
    flushInfo
  }
  override def printSpecStartTitle(message: String, stats: Stats)(implicit args: Arguments) = {
    info(message)
    flushInfo
  }
  override def printSeeLink(message: String, stats: Stats)(implicit args: Arguments) = {
    info(status(stats.result)+args.textColor(message))
  }

  override def printFailure(message: String)(implicit args: Arguments)                      = {
    logFailure(offset(message))
  }
  override def printError(message: String)(implicit args: Arguments)                        = {
    logError(offset(message))
  }
  override def printSuccess(message: String)(implicit args: Arguments)                      = {
    info(message)
  }
  override def printSkipped(message: String)(implicit args: Arguments)                      = {
    info(message)
    flushInfo
  }
  override def printPending(message: String)(implicit args: Arguments)                      = {
    info(message)
  }
  override def printStats(message: String)(implicit args: Arguments)                        = {
    flushInfo
    info(message)
  }
  override def printLine(message: String)(implicit args: Arguments)                         = {
    info(message)
  }
  override def printText(message: String)(implicit args: Arguments)                         = {
    info(message)
  }
}

/**
 * Specific events which can be notified to sbt
 */
trait HandlerEvents {
  class NamedEvent(name: String) extends Event {
    def testName = name
    def description = ""
    def result = Result.Success
    def error: Throwable = null
  }
  def succeeded(name: String) = new NamedEvent(name)
  def failure(name: String, e: Throwable) = new NamedEvent(name) {
    override def result = Result.Failure
    override def error = e
  }
  def error(name: String, e: Throwable) = new NamedEvent(name) {
    override def result = Result.Error
    override def error = e
  }
  def skipped(name: String) = new NamedEvent(name) {
    override def result = Result.Skipped
    override def error = null
  }
  def result(r: execute.Result): NamedEvent = r match {
    case s @ execute.Success(_, _)             => succeeded(r.message)
    case f @ execute.Failure(_,_,_,_)          => failure(r.message, f.exception)
    case e @ execute.Error(_,_)                => error(r.message, e.exception)
    case p @ execute.Pending(_)                => skipped(r.message)
    case k @ execute.Skipped(_,_)              => skipped(r.message)
    case d @ execute.DecoratedResult(dec, res) => result(res)
  }
}
object HandlerEvents extends HandlerEvents

trait TestLoggers {
  val loggers: Array[Logger]
  def logFailure(message: String) = loggers.foreach { logger =>
    logger.error(removeColors(message, !logger.ansiCodesSupported))
  }
  def logError(message: String) = loggers.foreach { logger =>
    logger.error(removeColors(message, !logger.ansiCodesSupported))
  }
  def logInfo(message: String) = loggers.foreach { logger =>
    logger.info(removeColors(message, !logger.ansiCodesSupported))
  }
}