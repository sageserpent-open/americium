/*
 * Copyright 2001-2009 Artima, Inc.
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
package org.scalatest

/**
 * Trait that facilitates a style of testing in which each test is run in its own instance
 * of the suite class to isolate each test from the side effects of the other tests in the
 * suite.
 *
 * <p>
 * If you mix this trait into a <code>Suite</code>, you can initialize shared reassignable
 * fixture variables as well as shared mutable fixture objects in the constructor of the
 * class. Because each test will run in its own instance of the class, each test will
 * get a fresh copy of the instance variables. This is the approach to test isolation taken,
 * for example, by the JUnit framework.
 * </p>
 *
 * <p>
 * Here's an example of <code>OneInstancePerTest</code> being used in a <code>FunSuite</code>:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.FunSuite
 * import org.scalatest.OneInstancePerTest
 * import collection.mutable.ListBuffer
 * 
 * class MySuite extends FunSuite with OneInstancePerTest {
 * 
 *   val builder = new StringBuilder("ScalaTest is ")
 *   val buffer = new ListBuffer[String]
 * 
 *   test("easy") {
 *     builder.append("easy!")
 *     assert(builder.toString === "ScalaTest is easy!")
 *     assert(buffer.isEmpty)
 *     buffer += "sweet"
 *   }
 * 
 *   test("fun") {
 *     builder.append("fun!")
 *     assert(builder.toString === "ScalaTest is fun!")
 *     assert(buffer.isEmpty)
 *   }
 * }
 * </pre>
 *
 * @author Bill Venners
 */
trait OneInstancePerTest extends AbstractSuite {
  
  this: Suite =>

  /**
   * Run this <code>Suite's</code> tests each in their own instance of this <code>Suite</code>'s class.
   *
   * <p>
   * If the passed <code>testName</code> is <code>None</code>, this trait's implementation of this
   * method will for each test name returned by <code>testNames</code>, invoke <code>newInstance</code>
   * to get a new instance of this <code>Suite</code>, and call <code>run</code> on it, passing
   * in the test name wrapped in a <code>Some</code>. If the passed <code>testName</code> is defined,
   * this trait's implementation of this method will simply forward all passed parameters
   * to <code>super.run</code>. If the invocation of either <code>newInstance</code> on this
   * <code>Suite</code> or <code>run</code> on a newly created instance of this <code>Suite</code>
   * completes abruptly with an exception, then this <code>runTests</code> method will complete
   * abruptly with the same exception.
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
   * @throws NullPointerException if any of the passed parameters is <code>null</code>.
   * @throws IllegalArgumentException if <code>testName</code> is defined, but no test with the specified test name
   *     exists in this <code>Suite</code>
   */
  protected abstract override def runTests(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
                             configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
    testName match {
      case Some(tn) => super.runTests(testName, reporter, stopper, filter, configMap, None, tracker)
      case None =>
        for (tn <- testNames) {
          val oneInstance = newInstance
          oneInstance.run(Some(tn), reporter, stopper, filter, configMap, None, tracker)
        }
    }
  }
  
  /**
   * Construct a new instance of this <code>Suite</code>.
   *
   * <p>
   * This trait's implementation of <code>runTests</code> invokes this method to create
   * a new instance of this <code>Suite</code> for each test. This trait's implementation
   * of this method uses reflection to call <code>this.getClass.newInstance</code>. This
   * approach will succeed only if this <code>Suite</code>'s class has a public, no-arg
   * constructor. In most cases this is likely to be true, because to be instantiated
   * by ScalaTest's <code>Runner</code> a <code>Suite</code> needs a public, no-arg
   * constructor. However, this will not be true of any <code>Suite</code> defined as
   * an inner class of another class or trait, because every constructor of an inner
   * class type takes a reference to the enclosing instance. In such cases, and in
   * cases where a <code>Suite</code> class is explicitly defined without a public,
   * no-arg constructor, you will need to override this method to construct a new
   * instance of the <code>Suite</code> in some other way.
   * </p>
   *
   * <p>
   * Here's an example of how you could override <code>newInstance</code> to construct
   * a new instance of an inner class:
   * </p>
   *
   * <pre class="stHighlight">
   * import org.scalatest.Suite
   *
   * class Outer {
   *   class InnerSuite extends Suite with OneInstancePerTest {
   *     def testOne() {}
   *     def testTwo() {}
   *     override def newInstance = new InnerSuite
   *   }
   * }
   * </pre>
   */
  def newInstance = this.getClass.newInstance.asInstanceOf[Suite]
}
