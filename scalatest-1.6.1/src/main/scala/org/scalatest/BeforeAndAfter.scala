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

import java.util.concurrent.atomic.AtomicReference

/**
 * Trait that can be mixed into suites that need code executed before and after
 * running each test. This trait facilitates a style of testing in which mutable
 * fixture objects held in instance variables are replaced or reinitialized before each test or
 * suite. Here's an example:
 *
 * <pre class="stHighlight">
 * import org.scalatest._
 * import scala.collection.mutable.ListBuffer
 *
 * class MySuite extends FunSuite with BeforeAndAfter {
 *
 *   // Fixtures as reassignable variables and mutable objects
 *   var sb: StringBuilder = _
 *   val lb = new ListBuffer[String]
 *
 *   before {
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
 * run each test, you will need to mix it in after a core suite trait to get the desired behavior. For example, this won't
 * compile, because <code>BeforeAndAfter</code> is "super" to </code>FunSuite</code>:
 * </p>
 * <pre class="stHighlight">
 * class MySuite extends BeforeAndAfter with FunSuite 
 * </pre>
 * <p>
 * You'd need to turn it around, so that <code>FunSuite</code> is "super" to <code>BeforeAndAfter</code>, like this:
 * </p>
 * <pre class="stHighlight">
 * class MySuite extends FunSuite with BeforeAndAfter
 * </pre>
 *
 * <p>
 * The <code>before</code> and <code>after</code> methods can each only be called once per <code>Suite</code>,
 * and cannot be invoked after <code>run</code> has been invoked.
 * </p>
 *
 * <p>
 * Note: The advantage this trait has over <code>BeforeAndAfterEach</code> is that its syntax is more concise. 
 * The main disadvantage is that it is not stackable, whereas <code>BeforeAndAfterEach</code> is. <em>I.e.</em>, 
 * you can write several traits that extend <code>BeforeAndAfterEach</code> and provide <code>beforeEach</code> methods
 * that include a call to <code>super.beforeEach</code>, and mix them together in various combinations. By contrast,
 * only one call to the <code>before</code> registration function is allowed in a suite or spec that mixes
 * in <code>BeforeAndAfter</code>. In addition, <code>BeforeAndAfterEach</code> allows you to access
 * the config map in its <code>beforeEach</code> and <code>afterEach</code> methods, whereas <code>BeforeAndAfter</code>
 * gives you no access to the config map.
 * </p>
 *
 * @author Bill Venners
 */
trait BeforeAndAfter extends AbstractSuite {

  this: Suite =>

  private val beforeFunctionAtomic = new AtomicReference[Option[() => Any]](None)
  private val afterFunctionAtomic = new AtomicReference[Option[() => Any]](None)
  @volatile private var runHasBeenInvoked = false

  /**
   * Registers code to be executed before each of this suite's tests.
   *
   * <p>
   * This trait's implementation
   * of <code>runTest</code> executes the code passed to this method before running
   * each test. Thus the code passed to this method can be used to set up a test fixture
   * needed by each test.
   * </p>
   *
   * @throws NotAllowedException if invoked more than once on the same <code>Suite</code> or if
   *                             invoked after <code>run</code> has been invoked on the <code>Suite</code>
   */
  protected def before(fun: => Any) {
    if (runHasBeenInvoked)
      throw new NotAllowedException("You cannot call before after run has been invoked (such as, from within a test). It is probably best to move it to the top level of the Suite class so it is executed during object construction.", 0)
    val success = beforeFunctionAtomic.compareAndSet(None, Some(() => fun))
    if (!success)
      throw new NotAllowedException("You are only allowed to call before once in each Suite that mixes in BeforeAndAfter.", 0)
  }

  /**
   * Registers code to be executed after each of this suite's tests.
   *
   * <p>
   * This trait's implementation of <code>runTest</code> executes the code passed to this method after running
   * each test. Thus the code passed to this method can be used to tear down a test fixture
   * needed by each test.
   * </p>
   *
   * @throws NotAllowedException if invoked more than once on the same <code>Suite</code> or if
   *                             invoked after <code>run</code> has been invoked on the <code>Suite</code>
   */
  protected def after(fun: => Any) {
    if (runHasBeenInvoked)
      throw new NotAllowedException("You cannot call after after run has been invoked (such as, from within a test. It is probably best to move it to the top level of the Suite class so it is executed during object construction.", 0)
    val success = afterFunctionAtomic.compareAndSet(None, Some(() => fun))
    if (!success)
      throw new NotAllowedException("You are only allowed to call after once in each Suite that mixes in BeforeAndAfter.", 0)
  }

  /**
   * Run a test surrounded by calls to the code passed to <code>before</code> and <code>after</code>, if any.
   *
   * <p>
   * This trait's implementation of this method ("this method") invokes
   * the function registered with <code>before</code>, if any,
   * before running each test and the function registered with <code>after</code>, if any,
   * after running each test. It runs each test by invoking <code>super.runTest</code>, passing along
   * the five parameters passed to it.
   * </p>
   * 
   * <p>
   * If any invocation of the function registered with <code>before</code> completes abruptly with an exception, this
   * method will complete abruptly with the same exception. If any call to
   * <code>super.runTest</code> completes abruptly with an exception, this method
   * will complete abruptly with the same exception, however, before doing so, it will
   * invoke the function registered with <code>after</code>, if any. If the function registered with <code>after</code>
   * <em>also</em> completes abruptly with an exception, this
   * method will nevertheless complete abruptly with the exception previously thrown by <code>super.runTest</code>.
   * If <code>super.runTest</code> returns normally, but the function registered with <code>after</code> completes abruptly with an
   * exception, this method will complete abruptly with the exception thrown by the function registered with <code>after</code>.
   * </p>
  */
  abstract protected override def runTest(testName: String, reporter: Reporter, stopper: Stopper, configMap: Map[String, Any], tracker: Tracker) {

    var thrownException: Option[Throwable] = None

    beforeFunctionAtomic.get match {
      case Some(fun) => fun()
      case None =>
    }

    try {
      super.runTest(testName, reporter, stopper, configMap, tracker)
    }
    catch {
      case e: Exception => thrownException = Some(e)
    }
    finally {
      try {
        // Make sure that afterEach is called even if runTest completes abruptly.
        afterFunctionAtomic.get match {
          case Some(fun) => fun()
          case None =>
        }

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

  /**
   * This trait's implementation of run sets a flag indicating run has been invoked, after which
   * any invocation to <code>before</code> or <code>after</code> will complete abruptly
   * with a <code>NotAllowedException</code>.
   */
  abstract override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
    configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

    runHasBeenInvoked = true
    super.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
  }
}
