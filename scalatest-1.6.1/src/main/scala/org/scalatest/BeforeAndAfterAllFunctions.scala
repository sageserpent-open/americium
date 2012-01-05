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

import java.util.concurrent.atomic.AtomicReference

/**
 * <b>Trait <code>BeforeAndAfterAllFunctions</code> has been deprecated and will be removed in a future version of ScalaTest. Please
 * use trait <code>BeforeAndAfterAll</code> instead.</b>
 *
 * <p>
 * Note: The reason this was deprecated is because its <code>beforeAll</code> and <code>afterAll</code>
 * methods have the same name and number of arguments as corresponding methods in <code>BeforeAndAfterAll</code>, some confusion
 * could potentially result in which method form is being invoked when both traits are mixed together.
 * </p>
 */
@deprecated("Use BeforeAndAfterAll instead."/*, "ScalaTest 1.5.1/1.6.1"*/)
trait BeforeAndAfterAllFunctions extends AbstractSuite {

  this: Suite =>

  private val beforeFunctionAtomic = new AtomicReference[Option[() => Any]](None)
  private val afterFunctionAtomic = new AtomicReference[Option[() => Any]](None)
  @volatile private var runHasBeenInvoked = false

  /**
   * Registers code to be executed before any of this suite's tests or nested suites are run.
   *
   * <p>
   * This trait's implementation
   * of <code>runTest</code> executes the code passed to this method before running
   * any tests or nested suites. Thus the code passed to this method can be used to set up a test fixture
   * needed by the entire suite.
   * </p>
   *
   * @throws NotAllowedException if invoked more than once on the same <code>Suite</code> or if
   *                             invoked after <code>run</code> has been invoked on the <code>Suite</code>
   */
  protected def beforeAll(fun: => Any) {
    if (runHasBeenInvoked)
      throw new NotAllowedException("You cannot call beforeEach after run has been invoked (such as, from within a test). It is probably best to move it to the top level of the Suite class so it is executed during object construction.", 0)
    val success = beforeFunctionAtomic.compareAndSet(None, Some(() => fun))
    if (!success)
      throw new NotAllowedException("You are only allowed to call beforeEach once in each Suite that mixes in BeforeAndAfterEachFunctions.", 0)
  }

  protected def afterAll() = ()
  /**
   * Registers code to be executed after all of this suite's tests and nested suites have
   * been run.
   *
   * <p>
   * This trait's implementation of <code>runTest</code> executes the code passed to this method after running
   * each test. Thus the code passed to this method can be used to tear down a test fixture
   * needed by the entire suite.
   * </p>
   *
   * @throws NotAllowedException if invoked more than once on the same <code>Suite</code> or if
   *                             invoked after <code>run</code> has been invoked on the <code>Suite</code>
   */
  protected def afterAll(fun: => Any) {
    if (runHasBeenInvoked)
      throw new NotAllowedException("You cannot call afterEach after run has been invoked (such as, from within a test. It is probably best to move it to the top level of the Suite class so it is executed during object construction.", 0)
    val success = afterFunctionAtomic.compareAndSet(None, Some(() => fun))
    if (!success)
      throw new NotAllowedException("You are only allowed to call beforeEach once in each Suite that mixes in BeforeAndAfterEachFunctions.", 0)
  }

  /**
   * Execute a suite surrounded by calls to <code>beforeAll</code> and <code>afterAll</code>.
   *
   * <p>
   * This trait's implementation of this method ("this method") invokes <code>beforeAll(Map[String, Any])</code>
   * before executing any tests or nested suites and <code>afterAll(Map[String, Any])</code>
   * after executing all tests and nested suites. It runs the suite by invoking <code>super.run</code>, passing along
   * the seven parameters passed to it.
   * </p>
   *
   * <p>
   * If any invocation of <code>beforeAll</code> completes abruptly with an exception, this
   * method will complete abruptly with the same exception. If any call to
   * <code>super.run</code> completes abruptly with an exception, this method
   * will complete abruptly with the same exception, however, before doing so, it will
   * invoke <code>afterAll</code>. If <cod>afterAll</code> <em>also</em> completes abruptly with an exception, this
   * method will nevertheless complete abruptly with the exception previously thrown by <code>super.run</code>.
   * If <code>super.run</code> returns normally, but <code>afterAll</code> completes abruptly with an
   * exception, this method will complete abruptly with the same exception.
   * </p>
  */
  abstract override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
                       configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

    runHasBeenInvoked = true

    var thrownException: Option[Throwable] = None

    beforeFunctionAtomic.get match {
      case Some(fun) => fun()
      case None =>
    }

    try {
      super.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
    }
    catch {
      case e: Exception => thrownException = Some(e)
    }
    finally {
      try {
        // Make sure that code passed to afterAll, if any, is called even if run completes abruptly.
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
}
