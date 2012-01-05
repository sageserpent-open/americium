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
 * <b>Trait <code>BeforeAndAfterEachFunctions</code> has been deprecated and will be removed in a future version of ScalaTest. Please
 * use trait <code>BeforeAndAfter</code> instead.</b>
 *
 * <p>
 * Note: The reasons this was deprecated is 1) <code>BeforeAndAfter</code> is more concise, both the trait name and the
 * <code>before</code>/<code>after</code> method names, and 2) because its <code>beforeEach</code> and <code>afterEach</code>
 * methods have the same name and number of arguments as corresponding methods in <code>BeforeAndAfterEach</code>, some confusion
 * could potentially result in which method form is being invoked when both traits are mixed together.
 * </p>
 */
@deprecated("Use BeforeAndAfter instead." /*, "ScalaTest 1.5.1/1.6.1"*/)
trait BeforeAndAfterEachFunctions extends AbstractSuite {

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
  protected def beforeEach(fun: => Any) {
    if (runHasBeenInvoked)
      throw new NotAllowedException("You cannot call beforeEach after run has been invoked (such as, from within a test). It is probably best to move it to the top level of the Suite class so it is executed during object construction.", 0)
    val success = beforeFunctionAtomic.compareAndSet(None, Some(() => fun))
    if (!success)
      throw new NotAllowedException("You are only allowed to call beforeEach once in each Suite that mixes in BeforeAndAfterEachFunctions.", 0)
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
  protected def afterEach(fun: => Any) {
    if (runHasBeenInvoked)
      throw new NotAllowedException("You cannot call afterEach after run has been invoked (such as, from within a test. It is probably best to move it to the top level of the Suite class so it is executed during object construction.", 0)
    val success = afterFunctionAtomic.compareAndSet(None, Some(() => fun))
    if (!success)
      throw new NotAllowedException("You are only allowed to call beforeEach once in each Suite that mixes in BeforeAndAfterEachFunctions.", 0)
  }

  /**
   * Run a test surrounded by calls to the code passed to <code>beforeEach</code> and <code>afterEach</code>, if any.
   *
   * <p>
   * This trait's implementation of this method ("this method") invokes
   * the function registered with <code>beforeEach</code>, if any,
   * before running each test and the function registered with <code>afterEach</code>, if any,
   * after running each test. It runs each test by invoking <code>super.runTest</code>, passing along
   * the five parameters passed to it.
   * </p>
   * 
   * <p>
   * If any invocation of the function registered with <code>beforeEach</code> completes abruptly with an exception, this
   * method will complete abruptly with the same exception. If any call to
   * <code>super.runTest</code> completes abruptly with an exception, this method
   * will complete abruptly with the same exception, however, before doing so, it will
   * invoke the function registered with <code>afterEach</code>, if any. If the function registered with <cod>afterEach</code>
   * <em>also</em> completes abruptly with an exception, this
   * method will nevertheless complete abruptly with the exception previously thrown by <code>super.runTest</code>.
   * If <code>super.runTest</code> returns normally, but the function registered with <code>afterEach</code> completes abruptly with an
   * exception, this method will complete abruptly with the exception thrown by the function registered with <code>afterEach</code>.
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
   * any invocation to <code>beforeEach</code> or <code>afterEach</code> will complete abruptly
   * with a <code>NotAllowedException</code>.
   */
  abstract override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
    configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

    runHasBeenInvoked = true
    super.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
  }
}
