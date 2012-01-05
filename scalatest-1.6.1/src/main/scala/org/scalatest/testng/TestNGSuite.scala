/*
 * Copyright 2001-2008 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatest.testng

import org.scalatest._
import org.scalatest.Suite
import org.scalatest.TestRerunner
import org.scalatest.events._
import Suite.getIndentedText
import Suite.formatterForSuiteAborted
import Suite.formatterForSuiteStarting
import Suite.formatterForSuiteCompleted
import events.MotionToSuppress

import org.testng.TestNG
import org.testng.TestListenerAdapter

/**
 * A suite of tests that can be run with either TestNG or ScalaTest. This trait allows you to mark any
 * method as a test using TestNG's <code>@Test</code> annotation, and supports all other TestNG annotations.
 * Here's an example:
 *
 * <p><b>BECAUSE OF A SCALADOC BUG IN SCALA 2.8, I HAD TO PUT A SPACE AFTER THE AT SIGN IN ANNOTATION EXAMPLES. IF YOU
 * WANT TO COPY AND PASTE FROM THESE EXAMPLES, YOU'LL NEED TO REMOVE THE SPACE BY HAND, OR COPY FROM
 * THE <a href="http://www.scalatest.org/scaladoc/doc-1.1/org/scalatest/testng/TestNGSuite.html">TESTNGSUITE SCALADOC FOR VERSION 1.1</a> INSTEAD, WHICH IS ALSO VALID FOR 1.3. - Bill Venners</b></p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.testng.TestNGSuite
 * import org.testng.annotations.Test
 * import org.testng.annotations.Configuration
 * import scala.collection.mutable.ListBuffer
 * 
 * class MySuite extends TestNGSuite {
 * 
 *   var sb: StringBuilder = _
 *   var lb: ListBuffer[String] = _
 * 
 *   @ Configuration(beforeTestMethod = true)
 *   def setUpFixture() {
 *     sb = new StringBuilder("ScalaTest is ")
 *     lb = new ListBuffer[String]
 *   }
 * 
 *   @ Test(invocationCount = 3)
 *   def easyTest() {
 *     sb.append("easy!")
 *     assert(sb.toString === "ScalaTest is easy!")
 *     assert(lb.isEmpty)
 *     lb += "sweet"
 *   }
 * 
 *   @ Test(groups = Array("com.mycompany.groups.SlowTest"))
 *   def funTest() {
 *     sb.append("fun!")
 *     assert(sb.toString === "ScalaTest is fun!")
 *     assert(lb.isEmpty)
 *   }
 * }
 * </pre>
 *
 * <p>
 * To execute <code>TestNGSuite</code>s with ScalaTest's <code>Runner</code>, you must include TestNG's jar file on the class path or runpath.
 * This version of <code>TestNGSuite</code> was tested with TestNG version 5.7.
 * </p>
 *
 * <p>
 * See also: <a href="http://www.scalatest.org/getting_started_with_testng" target="_blank">Getting started with TestNG and ScalaTest.</a>
 * </p>
 * 
 * @author Josh Cough
 * @author Bill Venners
 */
trait TestNGSuite extends Suite { thisSuite =>

  /**
   * Execute this <code>TestNGSuite</code>.
   * 
   * @param testName an optional name of one test to execute. If <code>None</code>, this class will execute all relevant tests.
   *                 I.e., <code>None</code> acts like a wildcard that means execute all relevant tests in this <code>TestNGSuite</code>.
   * @param   reporter	 The reporter to be notified of test events (success, failure, etc).
   * @param   groupsToInclude	Contains the names of groups to run. Only tests in these groups will be executed.
   * @param   groupsToExclude	Tests in groups in this Set will not be executed.
   *
   * @param stopper the <code>Stopper</code> may be used to request an early termination of a suite of tests. However, because TestNG does
   *                not support the notion of aborting a run early, this class ignores this parameter.
   * @param   properties         a <code>Map</code> of properties that can be used by the executing <code>Suite</code> of tests. This class
   *                      does not use this parameter.
   * @param distributor an optional <code>Distributor</code>, into which nested <code>Suite</code>s could be put to be executed
   *              by another entity, such as concurrently by a pool of threads. If <code>None</code>, nested <code>Suite</code>s will be executed sequentially.
   *              Because TestNG handles its own concurrency, this class ignores this parameter.
   * <br><br>
   */
  override def run(testName: Option[String], reporter: Reporter, stopper: Stopper,
      filter: Filter, properties: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
    
    runTestNG(testName, reporter, filter, tracker)
  }

  /**
   * Runs TestNG with no test name, no groups. All tests in the class will be executed.
   * @param   reporter   the reporter to be notified of test events (success, failure, etc)
   */
  private[testng] def runTestNG(reporter: Reporter, tracker: Tracker) {
    runTestNG(None, reporter, Filter(), tracker)
  }

  /**
   * Runs TestNG, running only the test method with the given name. 
   * @param   testName   the name of the method to run
   * @param   reporter   the reporter to be notified of test events (success, failure, etc)
   */
  private[testng] def runTestNG(testName: String, reporter: Reporter, tracker: Tracker) {
    runTestNG(Some(testName), reporter, Filter(), tracker)
  }
  
  /**
   * Runs TestNG. The meat and potatoes. 
   *
   * @param   testName   if present (Some), then only the method with the supplied name is executed and groups will be ignored
   * @param   reporter   the reporter to be notified of test events (success, failure, etc)
   * @param   groupsToInclude    contains the names of groups to run. only tests in these groups will be executed
   * @param   groupsToExclude    tests in groups in this Set will not be executed
   */  
  private[testng] def runTestNG(testName: Option[String], reporter: Reporter,
      filter: Filter, tracker: Tracker) {
    
    val tagsToInclude =
      filter.tagsToInclude match {
        case None => Set[String]()
        case Some(tti) => tti
      }
    val tagsToExclude = filter.tagsToExclude

    val testng = new TestNG()
    
    // only run the test methods in this class
    testng.setTestClasses(Array(this.getClass))
    
    // if testName is supplied, ignore groups.
    testName match {
      case Some(tn) => setupTestNGToRunSingleMethod(tn, testng)
      case None => handleGroups(tagsToInclude, tagsToExclude, testng)
    }

    this.run(testng, reporter, tracker)
  }
  
  /**
   * Runs the TestNG object which calls back to the given Reporter.
   */
  private[testng] def run(testng: TestNG, reporter: Reporter, tracker: Tracker) {
    
    // setup the callback mechanism
    val tla = new MyTestListenerAdapter(reporter, tracker)
    testng.addListener(tla)
    
    // finally, run TestNG
    testng.run()
  }
  
  /**
   * Tells TestNG which groups to include and exclude, which is directly a one-to-one mapping.
   */
  private[testng] def handleGroups(groupsToInclude: Set[String], groupsToExclude: Set[String], testng: TestNG) {
    testng.setGroups(groupsToInclude.mkString(","))
    testng.setExcludedGroups(groupsToExclude.mkString(","))
  }
  
  /**
   * This method ensures that TestNG will only run the test method whos name matches testName.
   * 
   * The approach is a bit odd however because TestNG doesn't have an easy API for
   * running a single method. To get around that we chose to use an AnnotationTransformer 
   * to add a secret group to the test method's annotation. We then set up TestNG to run only that group.
   *
   * NOTE: There was another option - we could TestNG's XmlSuites to specify which method to run.
   * This approach was about as much work, offered no clear benefits, and no additional problems either.
   * 
   * @param    testName    the name of the test method to be executed
   */
  private def setupTestNGToRunSingleMethod(testName: String, testng: TestNG) = {
    
    import org.testng.internal.annotations.IAnnotationTransformer
    import org.testng.internal.annotations.ITest
    import java.lang.reflect.Method
    import java.lang.reflect.Constructor
    
    class MyTransformer extends IAnnotationTransformer {
      override def transform( annotation: ITest, testClass: java.lang.Class[_], testConstructor: Constructor[_], testMethod: Method){
        if (testName.equals(testMethod.getName)) {
          annotation.setGroups(Array("org.scalatest.testng.singlemethodrun.methodname"))  
        }
      }
    }
    testng.setGroups("org.scalatest.testng.singlemethodrun.methodname")
    testng.setAnnotationTransformer(new MyTransformer())
  }
  
  /**
   * This class hooks TestNG's callback mechanism (TestListenerAdapter) to ScalaTest's
   * reporting mechanism. TestNG has many different callback points which are a near one-to-one
   * mapping with ScalaTest. At each callback point, this class simply creates ScalaTest 
   * reports and calls the appropriate method on the Reporter.
   * 
   * TODO: 
   * (12:02:27 AM) bvenners: onTestFailedButWithinSuccessPercentage(ITestResult tr) 
   * (12:02:34 AM) bvenners: maybe a TestSucceeded with some extra info in the report
   */
  private[testng] class MyTestListenerAdapter(reporter: Reporter, tracker: Tracker) extends TestListenerAdapter {
    
    // TODO: Put the tracker in an atomic, because TestNG can go multithreaded?

    val report = reporter

    import org.testng.ITestContext
    import org.testng.ITestResult
    
    private val className = TestNGSuite.this.getClass.getName

    /**
     * This method is called when TestNG starts, and maps to ScalaTest's suiteStarting. 
     * @TODO TestNG doesn't seem to know how many tests are going to be executed.
     * We are currently telling ScalaTest that 0 tests are about to be run. Investigate
     * and/or chat with Cedric to determine if its possible to get this number from TestNG.
     */
    override def onStart(itc: ITestContext) = {
      val formatter = formatterForSuiteStarting(thisSuite)
      report(SuiteStarting(tracker.nextOrdinal(), thisSuite.suiteName, Some(thisSuite.getClass.getName), formatter))
    }

    /**
     * TestNG's onFinish maps cleanly to suiteCompleted.
     * TODO: TestNG does have some extra info here. One thing we could do is map the info
     * in the ITestContext object into ScalaTest Reports and fire InfoProvided
     */
    override def onFinish(itc: ITestContext) = {
      val formatter = formatterForSuiteCompleted(thisSuite)
      report(SuiteCompleted(tracker.nextOrdinal(), thisSuite.suiteName, Some(thisSuite.getClass.getName), None, formatter))
    }
    
    /**
     * TestNG's onTestStart maps cleanly to TestStarting. Simply build a report 
     * and pass it to the Reporter.
     */
    override def onTestStart(result: ITestResult) = {
      report(TestStarting(tracker.nextOrdinal(), thisSuite.suiteName, Some(thisSuite.getClass.getName), result.getName + params(result),
          Some(MotionToSuppress), Some(new TestRerunner(className, result.getName))))
    }

    /**
     * TestNG's onTestSuccess maps cleanly to TestSucceeded. Again, simply build
     * a report and pass it to the Reporter.
     */
    override def onTestSuccess(result: ITestResult) = {
      val testName = result.getName + params(result)
      val formatter = getIndentedText(testName, 1, true)
      report(TestSucceeded(tracker.nextOrdinal(), thisSuite.suiteName, Some(thisSuite.getClass.getName), testName,
          None, Some(formatter), Some(new TestRerunner(className, result.getName)))) // Can I add a duration?
    }

    /**
     * TestNG's onTestSkipped maps cleanly to TestIgnored. Again, simply build
     * a report and pass it to the Reporter.
     */
    override def onTestSkipped(result: ITestResult) = {
      val testName = result.getName + params(result)
      val formatter = getIndentedText(testName, 1, true)
      report(TestIgnored(tracker.nextOrdinal(), thisSuite.suiteName, Some(thisSuite.getClass.getName), testName, Some(formatter)))
    }

    /**
     * TestNG's onTestFailure maps cleanly to TestFailed.
     */
    override def onTestFailure(result: ITestResult) = {
      val throwableOrNull = result.getThrowable
      val throwable = if (throwableOrNull != null) Some(throwableOrNull) else None
      val message = if (throwableOrNull != null && throwableOrNull.getMessage != null) throwableOrNull.getMessage else Resources("testNGConfigFailed")
      val testName = result.getName + params(result)
      val formatter = getIndentedText(testName, 1, true)
      report(TestFailed(tracker.nextOrdinal(), message, thisSuite.suiteName, Some(thisSuite.getClass.getName), testName, throwable, None, Some(formatter), Some(new TestRerunner(className, result.getName)))) // Can I add a duration?
    }

    /**
     * A TestNG setup method resulted in an exception, and a test method will later fail to run. 
     * This TestNG callback method has the exception that caused the problem, as well
     * as the name of the method that failed. Create a Report with the method name and the
     * exception and call reporter(SuiteAborted).
     */
    override def onConfigurationFailure(result: ITestResult) = {
      val throwableOrNull = result.getThrowable
      val throwable = if (throwableOrNull != null) Some(throwableOrNull) else None
      val message = if (throwableOrNull != null && throwableOrNull.getMessage != null) throwableOrNull.getMessage else Resources("testNGConfigFailed")
      val formatter = formatterForSuiteAborted(thisSuite, message)
      report(SuiteAborted(tracker.nextOrdinal(), message, thisSuite.suiteName, Some(thisSuite.getClass.getName), throwable, None, formatter))
    }

    /**
     * TestNG's onConfigurationSuccess doesn't have a clean mapping in ScalaTest.
     * Simply create a Report and fire InfoProvided. This works well
     * because there may be a large number of setup methods and InfoProvided doesn't 
     * show up in your face on the UI, and so doesn't clutter the UI. 
     */
    override def onConfigurationSuccess(result: ITestResult) = { // TODO: Work on this report
      // For now don't print anything. Succeed with silence. Is adding clutter.
      // report(InfoProvided(tracker.nextOrdinal(), result.getName, Some(NameInfo(thisSuite.suiteName, Some(thisSuite.getClass.getName), None))))
    }

    private def params(itr: ITestResult): String = {
      itr.getParameters match {   
        case Array() => ""
        case _ => "(" + itr.getParameters.mkString(",") + ")"
      }
    }
  }
  
  /**
     TODO
    (12:02:27 AM) bvenners: onTestFailedButWithinSuccessPercentage(ITestResult tr)
    (12:02:34 AM) bvenners: maybe a TestSucceeded with some extra info in the report
  **/

  /**
   * Throws <code>UnsupportedOperationException</code>, because this method is unused by this
   * class, given this class's <code>run</code> method delegates to JUnit to run
   * its tests.
   *
   * <p>
   * The main purpose of this method implementation is to render a compiler error an attempt
   * to mix in a trait that overrides <code>withFixture</code>. Because this
   * trait does not actually use <code>withFixture</code>, the attempt to mix
   * in behavior would very likely not work.
   * </p>
   *
   *
   * @param test the no-arg test function to run with a fixture
   */
  override final protected def withFixture(test: NoArgTest) {
     throw new UnsupportedOperationException
  }

  /**
   * Throws <code>UnsupportedOperationException</code>, because this method is unused by this
   * trait, given this trait's <code>run</code> method delegates to TestNG to run
   * its tests.
   *
   * <p>
   * The main purpose of this method implementation is to render a compiler error an attempt
   * to mix in a trait that overrides <code>runNestedSuites</code>. Because this
   * trait does not actually use <code>runNestedSuites</code>, the attempt to mix
   * in behavior would very likely not work.
   * </p>
   *
   * @param reporter the <code>Reporter</code> to which results will be reported
   * @param stopper the <code>Stopper</code> that will be consulted to determine whether to stop execution early.
   * @param filter a <code>Filter</code> with which to filter tests based on their tags
   * @param configMap a <code>Map</code> of key-value pairs that can be used by the executing <code>Suite</code> of tests.
   * @param distributor an optional <code>Distributor</code>, into which to put nested <code>Suite</code>s to be run
   *              by another entity, such as concurrently by a pool of threads. If <code>None</code>, nested <code>Suite</code>s will be run sequentially.
   * @param tracker a <code>Tracker</code> tracking <code>Ordinal</code>s being fired by the current thread.
   *
   * @throws UnsupportedOperationException always.
   */
  override final protected def runNestedSuites(reporter: Reporter, stopper: Stopper, filter: Filter,
                                configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

    throw new UnsupportedOperationException
  }

  /**
   * Throws <code>UnsupportedOperationException</code>, because this method is unused by this
   * trait, given this trait's <code>run</code> method delegates to TestNG to run
   * its tests.
   *
   * <p>
   * The main purpose of this method implementation is to render a compiler error an attempt
   * to mix in a trait that overrides <code>runTests</code>. Because this
   * trait does not actually use <code>runTests</code>, the attempt to mix
   * in behavior would very likely not work.
   * </p>
   *
   * @param testName an optional name of one test to run. If <code>None</code>, all relevant tests should be run.
   *                 I.e., <code>None</code> acts like a wildcard that means run all relevant tests in this <code>Suite</code>.
   * @param reporter the <code>Reporter</code> to which results will be reported
   * @param stopper the <code>Stopper</code> that will be consulted to determine whether to stop execution early.
   * @param filter a <code>Filter</code> with which to filter tests based on their tags
   * @param configMap a <code>Map</code> of key-value pairs that can be used by the executing <code>Suite</code> of tests.
   * @param distributor an optional <code>Distributor</code>, into which to put nested <code>Suite</code>s to be run
   *              by another entity, such as concurrently by a pool of threads. If <code>None</code>, nested <code>Suite</code>s will be run sequentially.
   * @param tracker a <code>Tracker</code> tracking <code>Ordinal</code>s being fired by the current thread.
   * @throws UnsupportedOperationException always.
   */
  override protected final def runTests(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
                            configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
    throw new UnsupportedOperationException
  }

  /**
   * Throws <code>UnsupportedOperationException</code>, because this method is unused by this
   * trait, given this trait's <code>run</code> method delegates to TestNG to run
   * its tests.
   *
   * <p>
   * The main purpose of this method implementation is to render a compiler error an attempt
   * to mix in a trait that overrides <code>runTest</code>. Because this
   * trait does not actually use <code>runTest</code>, the attempt to mix
   * in behavior would very likely not work.
   * </p>
   *
   * @param testName the name of one test to run.
   * @param reporter the <code>Reporter</code> to which results will be reported
   * @param stopper the <code>Stopper</code> that will be consulted to determine whether to stop execution early.
   * @param configMap a <code>Map</code> of key-value pairs that can be used by the executing <code>Suite</code> of tests.
   * @param tracker a <code>Tracker</code> tracking <code>Ordinal</code>s being fired by the current thread.
   * @throws UnsupportedOperationException always.
   */
  override protected final def runTest(testName: String, reporter: Reporter, stopper: Stopper, configMap: Map[String, Any], tracker: Tracker) {

        throw new UnsupportedOperationException
  }

}
