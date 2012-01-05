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
package org.scalatest

/**
 * Trait that can be mixed into suites that need methods invoked before and after
 * running each test. This trait facilitates a style of testing in which mutable
 * fixture objects held in instance variables are replaced or reinitialized before each test or
 * suite. Here's an example:
 *
 * <pre class="stHighlight">
 * import org.scalatest._
 * import scala.collection.mutable.ListBuffer
 *
 * class MySuite extends BeforeAndAfterEach {
 *
 *   // Fixtures as reassignable variables and mutable objects
 *   var sb: StringBuilder = _
 *   val lb = new ListBuffer[String]
 *
 *   override def beforeEach() {
 *     sb = new StringBuilder("ScalaTest is ")
 *     lb.clear()
 *   }
 *
 *   def testEasy() {
 *     sb.append("easy!")
 *     assert(sb.toString === "ScalaTest is easy!")
 *     assert(lb.isEmpty)
 *     lb += "sweet"
 *   }
 *
 *   def testFun() {
 *     sb.append("fun!")
 *     assert(sb.toString === "ScalaTest is fun!")
 *     assert(lb.isEmpty)
 *   }
 * }
 * </pre>
 *
 * <p>
 * Because this trait invokes <code>super.runTest</code> to
 * run each test, you may need to mix this trait in last to get the desired behavior. For example, this won't
 * work, because <code>BeforeAndAfterEach</code> is "super" to </code>FunSuite</code>:
 * </p>
 * <pre class="stHighlight">
 * class MySuite extends BeforeAndAfterEach with FunSuite 
 * </pre>
 * <p>
 * You'd need to turn it around, so that <code>FunSuite</code> is "super" to <code>BeforeAndAfterEach</code>, like this:
 * </p>
 * <pre class="stHighlight">
 * class MySuite extends FunSuite with BeforeAndAfterEach
 * </pre>
 *
 * @author Bill Venners
 */
trait BeforeAndAfterEach extends AbstractSuite {

  this: Suite =>

  /**
   * Defines a method to be run before each of this suite's tests.
   *
   * <p>
   * This trait's implementation
   * of <code>runTest</code> invokes the overloaded form of this method that
   * takes a <code>configMap</code> before running
   * each test. This trait's implementation of that <code>beforeEach(Map[String, Any])</code> method simply invokes this
   * <code>beforeEach()</code> method. Thus this method can be used to set up a test fixture
   * needed by each test, when you don't need anything from the <code>configMap</code>.
   * This trait's implementation of this method does nothing.
   * </p>
   */
  protected def beforeEach() = ()

  /**
   * Defines a method (that takes a <code>configMap</code>) to be run before
   * each of this suite's tests.
   *
   * <p>
   * This trait's implementation
   * of <code>runTest</code> invokes this method before running
   * each test (passing in the <code>configMap</code> passed to it), thus this
   * method can be used to set up a test fixture
   * needed by each test. This trait's implementation of this method invokes the
   * overloaded form of <code>beforeEach</code> that takes no <code>configMap</code>.
   * </p>
   */
  protected def beforeEach(configMap: Map[String, Any]) {
    beforeEach()
  }

  /**
   * Defines a method to be run after each of this suite's tests.
   *
   * <p>
   * This trait's implementation
   * of <code>runTest</code> invokes the overloaded form of this method that
   * takes a <code>configMap</code> map after running
   * each test. This trait's implementation of that <code>afterEach(Map[String, Any])</code> method simply invokes this
   * <code>afterEach()</code> method. Thus this method can be used to tear down a test fixture
   * needed by each test, when you don't need anything from the <code>configMap</code>.
   * This trait's implementation of this method does nothing.
   * </p>
   */
  protected def afterEach() = ()

  /**
   * Defines a method (that takes a <code>configMap</code>) to be run after
   * each of this suite's tests.
   *
   * <p>
   * This trait's implementation
   * of <code>runTest</code> invokes this method after running
   * each test (passing in the <code>configMap</code> passed to it), thus this
   * method can be used to tear down a test fixture
   * needed by each test. This trait's implementation of this method invokes the
   * overloaded form of <code>afterEach</code> that takes no <code>configMap</code>.
   * </p>
   */
  protected def afterEach(configMap: Map[String, Any]) {
    afterEach()
  }

  /**
   * Run a test surrounded by calls to <code>beforeEach</code> and <code>afterEach</code>.
   *
   * <p>
   * This trait's implementation of this method ("this method") invokes
   * <code>beforeEach(configMap)</code>
   * before running each test and <code>afterEach(configMap)</code>
   * after running each test. It runs each test by invoking <code>super.runTest</code>, passing along
   * the five parameters passed to it.
   * </p>
   * 
   * <p>
   * If any invocation of <code>beforeEach</code> completes abruptly with an exception, this
   * method will complete abruptly with the same exception. If any call to
   * <code>super.runTest</code> completes abruptly with an exception, this method
   * will complete abruptly with the same exception, however, before doing so, it will
   * invoke <code>afterEach</code>. If <cod>afterEach</code> <em>also</em> completes abruptly with an exception, this
   * method will nevertheless complete abruptly with the exception previously thrown by <code>super.runTest</code>.
   * If <code>super.runTest</code> returns normally, but <code>afterEach</code> completes abruptly with an
   * exception, this method will complete abruptly with the exception thrown by <code>afterEach</code>.
   * </p>
  */
  abstract protected override def runTest(testName: String, reporter: Reporter, stopper: Stopper, configMap: Map[String, Any], tracker: Tracker) {

    var thrownException: Option[Throwable] = None

    beforeEach(configMap)
    try {
      super.runTest(testName, reporter, stopper, configMap, tracker)
    }
    catch {
      case e: Exception => thrownException = Some(e)
    }
    finally {
      try {
        afterEach(configMap) // Make sure that afterEach is called even if runTest completes abruptly.
        thrownException match {
          case Some(e) => throw e
          case None =>
        }
      }
      catch {
        case laterException: Exception =>
          thrownException match { // If both run and afterAll throw an exception, report the test exception
            case Some(earlierException) => throw earlierException
            case None => throw laterException
          }
      }
    }
  }
}
