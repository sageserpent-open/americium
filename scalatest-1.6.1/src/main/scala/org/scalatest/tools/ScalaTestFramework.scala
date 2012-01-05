package org.scalatest.tools

import org.scalatools.testing._
import org.scalatest.tools.Runner.parsePropertiesArgsIntoMap
import org.scalatest.tools.Runner.parseCompoundArgIntoSet
import StringReporter.colorizeLinesIndividually
import org.scalatest.Suite.formatterForSuiteStarting
import org.scalatest.Suite.formatterForSuiteCompleted
import org.scalatest.Suite.formatterForSuiteAborted
import org.scalatest.events.SuiteStarting
import org.scalatest.events.SuiteCompleted
import org.scalatest.events.SuiteAborted

/**
 * Class that makes ScalaTest tests visible to sbt.
 *
 * <p>
 * To use ScalaTest from within sbt, simply add a line like this to your project file, replacing 1.5 with whatever version you desire:
 * </p>
 *
 * <pre class="stExamples">
 * val scalatest = "org.scalatest" % "scalatest_2.8.1" % "1.5"
 * </pre>
 *
 * <p>
 * You can configure the output shown when running with sbt in four ways: 1) turn off color, 2) show
 * short stack traces, 3) full stack traces, and 4) show durations for everything. To do that
 * you need to add test options, like this:
 * </p>
 *
 * <pre class="stExamples">
 * override def testOptions = super.testOptions ++
 *   Seq(TestArgument(TestFrameworks.ScalaTest, "-oD"))
 * </pre>
 *
 * <p>
 * After the -o, place any combination of:
 * </p>
 *
 * <ul>
 * <li>D - show durations</li>
 * <li>S - show short stack traces</li>
 * <li>F - show full stack traces</li>
 * <li>W - without color</li>
 * </ul>
 *
 * <p>
 * For example, "-oDF" would show full stack traces and durations (the amount
 * of time spent in each test).
 * </p>
 *
 * @author Bill Venners
 * @author Josh Cough
 */
class ScalaTestFramework extends Framework {

  /**
   * Returns <code>"ScalaTest"</code>, the human readable name for this test framework.
   */
  def name = "ScalaTest"

  /**
   * Returns an array containing one <code>org.scalatools.testing.TestFingerprint</code> object, whose superclass name is <code>org.scalatest.Suite</code>
   * and <code>isModule</code> value is false.
   */
  def tests =
    Array(
      new org.scalatools.testing.TestFingerprint {
        def superClassName = "org.scalatest.Suite"
        def isModule = false
      }
    )

  /**
   * Returns an <code>org.scalatools.testing.Runner</code> that will load test classes via the passed <code>testLoader</code>
   * and direct output from running the tests to the passed array of <code>Logger</code>s.
   */
  def testRunner(testLoader: ClassLoader, loggers: Array[Logger]) = {
    new ScalaTestRunner(testLoader, loggers)
  }

  /**The test runner for ScalaTest suites. It is compiled in a second step after the rest of sbt.*/
  private[tools] class ScalaTestRunner(val testLoader: ClassLoader, val loggers: Array[Logger]) extends org.scalatools.testing.Runner {

    import org.scalatest._

    /* 
      test-only FredSuite -- -A -B -C -d  all things to right of == come in as a separate string in the array
 the other way is to set up the options and when I say test it always comes in that way

 new wqay, if one framework

testOptions in Test += Tests.Arguments("-d", "-g")

so each of those would come in as one separate string in the aray

testOptions in Test += Tests.Arguments(TestFrameworks.ScalaTest, "-d", "-g")

Remember:

maybe add a distributor like thing to run
maybe add some event things like pending, ignored as well skipped
maybe a call back for the summary

st look at wiki on xsbt

tasks & commands. commands have full control over everything.
tasks are more integrated, don't need to know as much.
write a sbt plugin to deploy the task.

     */
    def run(testClassName: String, fingerprint: TestFingerprint, eventHandler: EventHandler, args: Array[String]) {
      val suiteClass = Class.forName(testClassName, true, testLoader).asSubclass(classOf[Suite])

      // println("sbt args: " + args.toList)
      if (isAccessibleSuite(suiteClass)) {

        val (propertiesArgsList, includesArgsList,
        excludesArgsList, repoArg) = parsePropsAndTags(args.filter(!_.equals("")))
        val configMap: Map[String, String] = parsePropertiesArgsIntoMap(propertiesArgsList)
        val tagsToInclude: Set[String] = parseCompoundArgIntoSet(includesArgsList, "-n")
        val tagsToExclude: Set[String] = parseCompoundArgIntoSet(excludesArgsList, "-l")
        val filter = org.scalatest.Filter(if (tagsToInclude.isEmpty) None else Some(tagsToInclude), tagsToExclude)

        val (presentAllDurations, presentInColor, presentShortStackTraces, presentFullStackTraces) =
          repoArg match {
            case Some(arg) => (
              arg contains 'D',
              !(arg contains 'W'),
              arg contains 'S',
              arg contains 'F'
             )
             case None => (false, true, false, false)
          }

        val report = new ScalaTestReporter(eventHandler, presentAllDurations, presentInColor, presentShortStackTraces, presentFullStackTraces)

        val tracker = new Tracker
        val suiteStartTime = System.currentTimeMillis

        val suite = suiteClass.newInstance

        val formatter = formatterForSuiteStarting(suite)

        report(SuiteStarting(tracker.nextOrdinal(), suite.suiteName, Some(suiteClass.getName), formatter, None))

        try {
          suite.run(None, report, new Stopper {}, filter, configMap, None, tracker)

          val formatter = formatterForSuiteCompleted(suite)

          val duration = System.currentTimeMillis - suiteStartTime
          report(SuiteCompleted(tracker.nextOrdinal(), suite.suiteName, Some(suiteClass.getName), Some(duration), formatter, None))
        }
        catch {       
          case e: Exception => {

            // TODO: Could not get this from Resources. Got:
            // java.util.MissingResourceException: Can't find bundle for base name org.scalatest.ScalaTestBundle, locale en_US
            val rawString = "Exception encountered when attempting to run a suite with class name: " + suiteClass.getName
            val formatter = formatterForSuiteAborted(suite, rawString)

            val duration = System.currentTimeMillis - suiteStartTime
            report(SuiteAborted(tracker.nextOrdinal(), rawString, suite.suiteName, Some(suiteClass.getName), Some(e), Some(duration), formatter, None))
          }
        }
      }
      else throw new IllegalArgumentException("Class is not an accessible org.scalatest.Suite: " + testClassName)
    }

    private val emptyClassArray = new Array[java.lang.Class[T] forSome {type T}](0)

    private def isAccessibleSuite(clazz: java.lang.Class[_]): Boolean = {
      import java.lang.reflect.Modifier

      try {
        classOf[Suite].isAssignableFrom(clazz) &&
                Modifier.isPublic(clazz.getModifiers) &&
                !Modifier.isAbstract(clazz.getModifiers) &&
                Modifier.isPublic(clazz.getConstructor(emptyClassArray: _*).getModifiers)
      } catch {
        case nsme: NoSuchMethodException => false
        case se: SecurityException => false
      }
    }

    private class ScalaTestReporter(eventHandler: EventHandler, presentAllDurations: Boolean,
        presentInColor: Boolean, presentShortStackTraces: Boolean, presentFullStackTraces: Boolean) extends StringReporter(
        presentAllDurations, presentInColor, presentShortStackTraces, presentFullStackTraces) {

      import org.scalatest.events._

      protected def printPossiblyInColor(text: String, ansiColor: String) {
        import PrintReporter.ansiReset
        loggers.foreach { logger =>
          logger.info(if (logger.ansiCodesSupported && presentInColor) colorizeLinesIndividually(text, ansiColor) else text)
        }
      }

      def dispose() = ()

      def fireEvent(tn: String, r: Result, e: Option[Throwable]) = {
        eventHandler.handle(
          new org.scalatools.testing.Event {
            def testName = tn
            def description = tn
            def result = r
            def error = e getOrElse null
          }
        )
      }

      override def apply(event: Event) {

        // Superclass will call printPossiblyInColor
        super.apply(event)

        // Logging done, all I need to do now is fire events
        event match {
          // the results of running an actual test
          case t: TestPending => fireEvent(t.testName, Result.Skipped, None)
          case t: TestFailed => fireEvent(t.testName, Result.Failure, t.throwable)
          case t: TestSucceeded => fireEvent(t.testName, Result.Success, None)
          case t: TestIgnored => fireEvent(t.testName, Result.Skipped, None)
          case _ => 
        }
      }
    }

    private[scalatest] def parsePropsAndTags(args: Array[String]) = {

      import collection.mutable.ListBuffer

      val props = new ListBuffer[String]()
      val includes = new ListBuffer[String]()
      val excludes = new ListBuffer[String]()
      var repoArg: Option[String] = None

      val it = args.iterator
      while (it.hasNext) {

        val s = it.next

        if (s.startsWith("-D")) {
          props += s
        }
        else if (s.startsWith("-n")) {
          includes += s
          if (it.hasNext)
            includes += it.next
        }
        else if (s.startsWith("-l")) {
          excludes += s
          if (it.hasNext)
            excludes += it.next
        }
        else if (s.startsWith("-o")) {
          if (repoArg.isEmpty) // Just use first one. Ignore any others.
            repoArg = Some(s)
        }
        //      else if (s.startsWith("-t")) {
        //
        //        testNGXMLFiles += s
        //        if (it.hasNext)
        //          testNGXMLFiles += it.next
        //      }
        else {
          throw new IllegalArgumentException("Unrecognized argument: " + s)
        }
      }
      (props.toList, includes.toList, excludes.toList, repoArg)
    }
  }
}
