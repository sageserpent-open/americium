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
package org.scalatest.junit;

import collection.immutable.TreeSet
import java.lang.reflect.{Method, Modifier}
import org.junit.runner.{Request, JUnitCore, Description, Result}
import org.scalatest._
import org.scalatest.Suite
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.Failure
import org.scalatest.events._

/**
 * A suite of tests that can be run with either JUnit or ScalaTest. This trait allows you to write JUnit 4 tests
 * with ScalaTest's more concise assertion syntax as well as JUnit's assertions (<code>assertEquals</code>, etc.).
 * You create tests by defining methods that are annotated with <code>Test</code>, and can create fixtures with
 * methods annotated with <code>Before</code> and <code>After</code>. For example:
 *
 * <p><b>BECAUSE OF A SCALADOC BUG IN SCALA 2.8, I HAD TO PUT A SPACE AFTER THE AT SIGN IN ANNOTATION EXAMPLES. IF YOU
 * WANT TO COPY AND PASTE FROM THESE EXAMPLES, YOU'LL NEED TO REMOVE THE SPACE BY HAND, OR COPY FROM
 * THE <a href="http://www.scalatest.org/scaladoc/doc-1.1/org/scalatest/junit/JUnitSuite.html">JUNITSUITE SCALADOC FOR VERSION 1.1</a> INSTEAD, WHICH IS ALSO VALID FOR 1.3. - Bill Venners</b></p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.junit.JUnitSuite
 * import scala.collection.mutable.ListBuffer
 * import _root_.org.junit.Test
 * import _root_.org.junit.Before
 *
 * class TwoSuite extends JUnitSuite {
 *
 *   var sb: StringBuilder = _
 *   var lb: ListBuffer[String] = _
 *
 *   @ Before def initialize() {
 *     sb = new StringBuilder("ScalaTest is ")
 *     lb = new ListBuffer[String]
 *   }
 *
 *   @ Test def verifyEasy() {
 *     sb.append("easy!")
 *     assert(sb.toString === "ScalaTest is easy!")
 *     assert(lb.isEmpty)
 *     lb += "sweet"
 *   }
 *
 *   @ Test def verifyFun() {
 *     sb.append("fun!")
 *     assert(sb.toString === "ScalaTest is fun!")
 *     assert(lb.isEmpty)
 *   }
 * }
 * </pre>
 *
 * <p>
 * To execute <code>JUnitSuite</code>s with ScalaTest's <code>Runner</code>, you must include JUnit's jar file on the class path or runpath.
 * This version of <code>JUnitSuite</code> was tested with JUnit version 4.4.
 * </p>
 *
 * <p>
 * Instances of this trait are not thread safe.
 * </p>
 *
 * <p>
 * See also: <a href="http://www.scalatest.org/getting_started_with_junit_4" target="_blank">Getting started with JUnit 4 and ScalaTest.</a>
 * </p>
 * 
 * @author Bill Venners
 * @author Daniel Watson
 * @author Joel Neely
 */
trait JUnitSuite extends Suite with AssertionsForJUnit { thisSuite =>
                 
  // This is volatile, because who knows what Thread JUnit will fire through this.
  @volatile private var theTracker = new Tracker

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
   * trait, given this trait's <code>run</code> method delegates to JUnit to run
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
   * trait, given this trait's <code>run</code> method delegates to JUnit to run
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
   * trait, given this traits's <code>run</code> method delegates to JUnit to run
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

    // TODO: Check to see if JUnit discovers static methods, private methods, etc.
    // Also, JUnit has something about test methods that can be parameterized. Will
    // eventually need to find those here too. What a pain.
    def isTestMethod(m: Method) = {

      val isInstanceMethod = !Modifier.isStatic(m.getModifiers())

      val paramTypes = m.getParameterTypes
      val hasNoParams = paramTypes.length == 0
      // val hasVoidReturnType = m.getReturnType == Void.TYPE
      val hasTestAnnotation = m.getAnnotation(classOf[org.junit.Test]) != null

      isInstanceMethod && hasNoParams && hasTestAnnotation
    }

    val testNameArray =
      for (m <- getClass.getMethods; if isTestMethod(m))
      yield m.getName

    TreeSet[String]() ++ testNameArray
  }

  /**
   * Returns the number of tests expected to be run by JUnit when <code>run</code> is invoked
   * on this <code>JUnitSuite</code>.
   *
   * <p>
   * If <code>tagsToInclude</code> in the passed <code>Filter</code> is defined, this class's
   * implementation of this method returns 0. Else this class's implementation of this method
   * returns the size of the set returned by <code>testNames</code> on the current instance,
   * less the number of tests that were annotated with <code>org.junit.Ignore</code>.
   * </p>
   */
  override def expectedTestCount(filter: Filter) =
    if (filter.tagsToInclude.isDefined) 0 else (testNames.size - tags.size)

  // Returns just tests that have org.junit.Ignore on them, but calls it org.scalatest.Ignore!
  override def tags: Map[String, Set[String]] = {

    def getMethodForJUnitTestName(testName: String) =
      getClass.getMethod(testName, new Array[Class[_]](0): _*)

    def hasIgnoreTag(testName: String) = getMethodForJUnitTestName(testName).getAnnotation(classOf[org.junit.Ignore]) != null

    val elements =
      for (testName <- testNames; if hasIgnoreTag(testName))
        yield testName -> Set("org.scalatest.Ignore")

    Map() ++ elements
  }

  override def run(testName: Option[String], report: Reporter, stopper: Stopper,
      filter: Filter, configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

    theTracker = tracker

    if (!filter.tagsToInclude.isDefined) {
      val jUnitCore = new JUnitCore
      jUnitCore.addListener(new MyRunListener(report, configMap, tracker))
      val myClass = getClass
      testName match {
        case None => jUnitCore.run(myClass)
        case Some(tn) =>
          if (!testNames.contains(tn))
            throw new IllegalArgumentException(Resources("testNotFound", testName))
          jUnitCore.run(Request.method(myClass, tn))
      }
    }
  }

// verifySomething(org.scalatest.junit.helpers.HappySuite)
// Description.displayName of a test report has the form <testMethodName>(<suiteClassName>)
}
