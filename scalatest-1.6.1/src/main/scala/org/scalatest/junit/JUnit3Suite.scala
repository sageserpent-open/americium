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
package org.scalatest.junit

import collection.immutable.TreeSet
import java.lang.reflect.{Method, Modifier}
import org.scalatest._
import _root_.junit.framework.TestCase
import _root_.junit.framework.TestResult
import _root_.junit.framework.TestSuite
import _root_.junit.framework.TestListener
import _root_.junit.framework.Test
import _root_.junit.framework.AssertionFailedError
import scala.collection.mutable.HashSet
import org.scalatest.events.TestStarting
import org.scalatest.events.TestSucceeded
import org.scalatest.events.TestFailed
import org.scalatest.events.MotionToSuppress
import Suite.getIndentedText

/**
 * A <code>Suite</code> that is also a <code>junit.framework.TestCase</code>. 
 *
 * <p>
 * A <code>JUnit3Suite</code> may be run by either JUnit 3 (such as JUnit 3.8) or ScalaTest's runner. You write it the way
 * you write a JUnit 3 <code>TestCase</code>. Tests are methods that start with <code>test</code>, take no parameters, and
 * have a <code>Unit</code> return type. You manage fixtures with methods <code>setUp</code> and <code>tearDown</code>.
 * Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.junit.JUnit3Suite
 * import scala.collection.mutable.ListBuffer
 *
 * class BlastFromThePastSuite extends JUnit3Suite {
 *
 *   var sb: StringBuilder = _
 *   var lb: ListBuffer[String] = _
 *
 *   override def setUp() {
 *     sb = new StringBuilder("ScalaTest is ")
 *     lb = new ListBuffer[String]
 *   }
 *
 *   def testEasy() { // Uses JUnit-style assertions
 *     sb.append("easy!")
 *     assertEquals("ScalaTest is easy!", sb.toString)
 *     assertTrue(lb.isEmpty)
 *     lb += "sweet"
 *   }
 *
 *   def testFun() { // Uses ScalaTest assertions
 *     sb.append("fun!")
 *     assert(sb.toString === "ScalaTest is fun!")
 *     assert(lb.isEmpty)
 *   }
 * }
 * </pre>
 * 
 * <p>
 * You can use either JUnit's assertions, inherited from <code>TestCase</code>, or ScalaTest's, inherited from <code>AssertionsForJUnit</code>.
 * You can also mix in <code>ShouldMatchersForJUnit</code> or <code>MustMatchersForJUnit</code> if you want to use ScalaTests's matchers DSL.
 * Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.junit.JUnit3Suite
 * import org.scalatest.junit.MustMatchersForJUnit
 * import scala.collection.mutable.ListBuffer
 *
 * class BlastFromThePastSuite extends JUnit3Suite with MustMatchersForJUnit {
 *
 *   var stringBuilder: StringBuilder = _
 *   var listBuffer: ListBuffer[String] = _
 *
 *   override def setUp() {
 *     stringBuilder = new StringBuilder("ScalaTest is ")
 *     listBuffer = new ListBuffer[String]
 *   }
 *
 *   def testEasy() {
 *     stringBuilder.append("easy!")
 *     stringBuilder.toString must be ("ScalaTest is easy!")
 *     listBuffer must be ('empty)
 *     listBuffer += "sweet"
 *   }
 *
 *   def testFun() {
 *     stringBuilder.append("fun!")
 *     stringBuilder.toString must be ("ScalaTest is fun!")
 *     listBuffer must be ('empty)
 *   }
 * }
 * </pre>
 * 
 * <p>
 * The reason you would ordinarily want to mix in <code>MustMatchersForJUnit</code> or <code>ShouldMatchersForJUnit</code> rather than <code>MustMatchers</code>
 * or <code>ShouldMatchers</code> is that <code>MustMatchersForJUnit</code> and <code>ShouldMatchersForJUnit</code> throw
 * <code>junit.framework.AssertionFailedError</code>s, which JUnit 3 will report as failures, not errors.
 * </p>
 *
 * <p>
 * When writing JUnit 3 tests in Scala, you should keep in mind that JUnit 3 will not run tests that have a return type other than
 * <code>Unit</code>. Thus it is best to leave off the equals sign before the curly braces of the body of the test, like this:
 * </p>
 * 
 * <pre class="stHighlight">
 * def testGoodIdea() { // result type will be Unit
 *   // ...
 * }
 * </pre>
 *
 * <p>
 * Instead of this:
 * </p>
 *
 * <pre class="stHighlight">
 * def testBadIdea() = { // result type will be inferred
 *   // ...
 * }
 * </pre>
 *
 * <p>
 * If the <code>testBadIdea</code> method ends in an expression that has a result type other than <code>Unit</code>, the Scala
 * compiler will infer a result type to the <code>testBadIdea</code> method to be the same non-<code>Unit</code> type. As a "result,"
 * JUnit 3 will not discover or run the <code>testBadIdea</code> method at all.
 * </p>
 *
 * <p>
 * See also: <a href="http://www.scalatest.org/getting_started_with_junit_3" target="_blank">Getting started with JUnit 3 and ScalaTest.</a>
 * </p>
 * 
 * @author Bill Venners
 */
class JUnit3Suite extends TestCase with Suite with AssertionsForJUnit {

  // This is volatile, because who knows what Thread JUnit will fire through this.
  @volatile private var theTracker = new Tracker

  /**
   * Returns the set of test names that will be executed by JUnit when <code>run</code> is invoked
   * on an instance of this class, or the instance is passed directly to JUnit for running.
   *
   * <p>
   * The iterator obtained by invoking <code>elements</code> on this
   * returned <code>Set</code> will produce the test names in their <em>natural order</em>, as determined by <code>String</code>'s
   * <code>compareTo</code> method. Nevertheless, this method is not consulted by JUnit when it
   * runs the tests, and JUnit may run the tests in any order.
   * </p>
   */
  override def testNames: Set[String] = {

    def isTestMethod(m: Method) = {

      val isInstanceMethod = !Modifier.isStatic(m.getModifiers())

      // name must have at least 4 chars (minimum is "test")
      val simpleName = m.getName
      val firstFour = if (simpleName.length >= 4) simpleName.substring(0, 4) else ""

      val paramTypes = m.getParameterTypes
      val hasNoParams = paramTypes.length == 0
      val hasVoidReturnType = m.getReturnType == Void.TYPE

      // won't discover testNames because it has a non-Unit return type
      isInstanceMethod && (firstFour == "test") && hasNoParams && hasVoidReturnType
    }

    val testNameArray =
      for (m <- getClass.getMethods; if isTestMethod(m))
      yield m.getName

    TreeSet[String]() ++ testNameArray
  }

  /**
   * Returns an empty <code>Map</code>, because tags are not supported by JUnit 3.
   */
  override def tags = Map()

  /**
   * Returns the number of tests expected to be run by JUnit when <code>run</code> is invoked
   * on this <code>Suite</code>.
   *
   * <p>
   * If <code>tagsToInclude</code> in the passed <code>Filter</code> is defined, this class's
   * implementation of this method returns 0. Else this class's implementation of this method
   * returns the size of the set returned by <code>testNames</code> on the current instance.
   * </p>
   */
  override def expectedTestCount(filter: Filter) =
    if (filter.tagsToInclude.isDefined) 0 else testNames.size

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
   * class, given this class's <code>run</code> method delegates to JUnit to run
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
   * class, given this class's <code>run</code> method delegates to JUnit to run
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
   * class, given this class's <code>run</code> method delegates to JUnit to run
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

  override def run(testName: Option[String], reporter: Reporter, stopper: Stopper,
      filter: Filter, configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

    theTracker = tracker

    if (!filter.tagsToInclude.isDefined) {
      val testResult = new TestResult
      testResult.addListener(new MyTestListener(reporter, tracker))
      testName match {
        case None => new TestSuite(this.getClass).run(testResult)
        case Some(tn) =>
          if (!testNames.contains(tn))
            throw new IllegalArgumentException(Resources("testNotFound", testName))
          setName(tn)
          run(testResult)
      }
    }
  }
}

private[scalatest] class MyTestListener(report: Reporter, tracker: Tracker) extends TestListener {

  // TODO: worry about threading
  private val failedTestsSet = scala.collection.mutable.Set[Test]()

  private def getSuiteNameForTestCase(testCase: Test) =
    testCase match {
      case junit3Suite: JUnit3Suite => junit3Suite.suiteName
      case _ => Suite.getSimpleNameOfAnObjectsClass(testCase) // Should never happen, but just in case
    }

  def getMessageGivenThrowable(throwable: Throwable, isAssertionFailedError: Boolean) =
    if (throwable.getMessage == null)
      "A JUnit3Suite test failed with an " + (if (isAssertionFailedError) "AssertionFailedError" else "exception") // Hopefully will never happen
    else
      throwable.getMessage

  // The Test passed to these methods is an instance of the JUnit3Suite class, Calling
  // test.getClass.getName on it gets the fully qualified name of the class
  // test.asInstanceOf[TestCase].getName gives you the name of the test method, without any parens
  // Calling test.toSring gives you testError(org.scalatestexamples.junit.JUnit3ExampleSuite)
  // So that's that old JUnit-style test name thing.
  def startTest(testCase: Test) {
    if (testCase == null)
      throw new NullPointerException("testCase was null")
    report(TestStarting(tracker.nextOrdinal(), getSuiteNameForTestCase(testCase), Some(testCase.getClass.getName), testCase.toString, Some(MotionToSuppress), None))
  }
  
  def addError(testCase: Test, throwable: Throwable) {

    if (testCase == null)
      throw new NullPointerException("testCase was null")
    if (throwable == null)
      throw new NullPointerException("throwable was null")

    val formatter = getIndentedText(testCase.toString, 1, true)
    report(TestFailed(tracker.nextOrdinal(), getMessageGivenThrowable(throwable, false), getSuiteNameForTestCase(testCase), Some(testCase.getClass.getName), testCase.toString, Some(throwable), None, Some(formatter), None))

    failedTestsSet += testCase
  }

  def addFailure(testCase: Test, assertionFailedError: AssertionFailedError) {

    if (testCase == null)
      throw new NullPointerException("testCase was null")
    if (assertionFailedError == null)
      throw new NullPointerException("throwable was null")

    val formatter = getIndentedText(testCase.toString, 1, true)
    report(TestFailed(tracker.nextOrdinal(), getMessageGivenThrowable(assertionFailedError, true), getSuiteNameForTestCase(testCase), Some(testCase.getClass.getName), testCase.toString, Some(assertionFailedError), None, Some(formatter), None))

    failedTestsSet += testCase
  }

  def endTest(testCase: Test) {

    val testHadFailed = failedTestsSet.contains(testCase)

    if (!testHadFailed) {
      if (testCase == null)
        throw new NullPointerException("testCase was null")
      val formatter = getIndentedText(testCase.toString, 1, true)
      report(TestSucceeded(tracker.nextOrdinal(), getSuiteNameForTestCase(testCase), Some(testCase.getClass.getName), testCase.toString, None, Some(formatter), None))
    }
    else {
      failedTestsSet -= testCase  
    }
  }

}

