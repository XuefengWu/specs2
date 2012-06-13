package org.specs2
package runner
import io._
import mock.Mockito
import matcher.DataTables
import reporter._
import org.scalatools.testing._
import main.{ArgumentsArgs, Arguments}

class TestInterfaceRunnerSpec extends Specification { def is =
                                                                                                                        """
  A TestInterfaceRunner is responsible for instantiating Specification classes found by
  sbt and executing them using a TestInterfaceReporter

                                                                                                                        """^
  "if the specification class is missing"                                                                               ^
    "there must be an error logged"                                                                                     ! missing().e1^
    "a stacktrace must be logged"                                                                                       ! missing().e2^
                                                                                                                        p^
  "if the specification instance cannot be created"                                                                     ^
    "a stacktrace for the exception must be logged"                                                                     ! instance().e1^
    "the cause stacktrace must also be logged if there is one"                                                          ! instance().e2^
    "the cause stacktrace must be nicely separated from the top exception"                                              ! instance().e3^
                                                                                                                        end^
  "if the specification instance can be created it must be passed to TestInterfaceReporter"                             ! reporting().e1^
  "Additional report types can be passed on the command line"                                                           ! reporting().e2^
  "A custom notifier can be specified on the command line with 'notifier <class name>'"                                 ! reporting().e3^
  "A custom exporter can be specified on the command line with 'exporter <class name>'"                                 ! reporting().e4^
  "A custom notifier can be specified in the specification with 'args.report(exporter=<class name>)'"                   ! reporting().e5^
  "A custom exporter can be specified in the specification with 'args.report(exporter=<class name>)'"                   ! reporting().e6^
                                                                                                                        end

  case class missing() {
    object run extends MockLogger {
	    val runner = new TestInterfaceRunner(getClass.getClassLoader, Array(logger))
	    runner.run("missing", mock[TestFingerprint], mock[EventHandler], Array(""))
    }
	  def e1 = run.logger.messages must contain("error: Could not create an instance of missing\n")
	  def e2 = run.logger.messages must contain("error:   caused by java.lang.ClassNotFoundException: missing")
  }

  case class instance() {
    object run extends MockLogger {
      val runner = new TestInterfaceRunner(getClass.getClassLoader, Array(logger))
	    runner.run("org.specs2.runner.SpecificationForSbtWithException", mock[TestFingerprint], mock[EventHandler], Array(""))
    }	  
	  def e1 = run.logger.messages must 
	           contain("error: Could not create an instance of org.specs2.runner.SpecificationForSbtWithException\n")
	  def e2 = run.logger.messages must 
	           contain("error:   caused by java.lang.IllegalArgumentException: cause")
	  def e3 = run.logger.messages must 
	           contain("error:   caused by java.lang.Exception: fail")
  }

}
case class reporting() extends Mockito with MockLogger with DataTables with matcher.MustMatchers with ArgumentsArgs {
  val outer = this
  val reporter = mock[Reporter]
  val handler = mock[EventHandler]

  val runner = new TestInterfaceRunner(getClass.getClassLoader, Array(logger)) {
    override def reporter(handler: EventHandler)(args: Array[String]): Reporter = outer.reporter
  }

  def reportSpec(args: Array[String] = Array("")) =
    runner.run("org.specs2.runner.SpecificationForSbt", mock[TestFingerprint], mock[EventHandler], args)

  def e1 = {
    reportSpec()
    there was one(reporter).report(any[specification.SpecificationStructure])(any[Arguments])
  }

  def e2 = {
    implicit val args = Arguments()

    def export(condition: Boolean, e: String) = if (condition) Some(e) else None
    def selectedExporters(c: Boolean, h: Boolean, m: Boolean, j: Boolean) =
      Seq(export(c, "TestInterfaceReporter"), export(h, "HtmlExporting$"), export(m, "MarkupExporting$"), export(j, "JUnitXmlExporting$")).flatten

    "args"                                || "console" | "html" | "markup" | "junitxml" |
    "junitxml"                            !! false     ! false  ! false    ! true       |
    "junitxml,console"                    !! true      ! false  ! false    ! true       |
    "junitxml,html,console"               !! true      ! true   ! false     ! true       |
    "junitxml,markup,console"             !! true      ! false   ! true     ! true       |> { (arguments, c, h, m, j) =>
      runner.exporters(arguments.split(","), handler).map(_.getClass.getSimpleName) must containAllOf(selectedExporters(c, h, m, j))
    }

  }

  def e3 = {
    val args = Array("notifier", "user.reporter.CustomNotifier")
    atLeastOnce(runner.exporters(args, handler)(Arguments(args:_*))) { e =>
      e must haveInterface[NotifierExporting]
    }
  }

  def e4 = {
    val args = Array("exporter", "user.reporter.CustomExporter")
    atLeastOnce(runner.exporters(args, handler)(Arguments(args:_*))) { e =>
      e must haveInterface[Exporter]
    }
  }

  def e5 = {
    atLeastOnce(runner.exporters(Array[String](), handler)(args.report(notifier="user.reporter.CustomNotifier"))) { e =>
      e must haveInterface[NotifierExporting]
    }
  }

  def e6 = {
    atLeastOnce(runner.exporters(Array[String](), handler)(args.report(exporter="user.reporter.CustomExporter"))) { e =>
      e must haveInterface[Exporter]
    }
  }
}


trait MockLogger extends matcher.MustExpectations with Mockito {
  val logger = new Logger with MockOutput {
	  override def ansiCodesSupported = false
	  override def error(message: String) = println("error: " + message)
	  override def info(message: String)  = println("info: " + message)
	  override def warn(message: String)  = println("warn: " + message)
	  override def debug(message: String) = println("debug: " + message)
	  override def trace(t: Throwable)    = println("trace: " + t)
  }

}
class SpecificationForSbtWithException extends Specification {
  val cause = new IllegalArgumentException("cause")
  throw new Exception("fail", cause)
  def is = "ex1" ! success ^ end
}
class SpecificationForSbt extends Specification {
  def is = "ex1" ! success ^ end
}