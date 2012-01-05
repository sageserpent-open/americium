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

import tools.DistributedTestRunnerSuite

/**
 * Trait that causes that the tests of any suite it is mixed into to be run in parallel if
 * a <code>Distributor</code> is passed to <code>runTests</code>.
 *
 * <p>
 * ScalaTest's normal approach for running suites of tests in parallel is to run different suites in parallel,
 * but the tests of any one suite sequentially. This approach should provide sufficient distribution of the work load
 * in most cases, but some suites may encapsulate multiple long-running tests. Such suites may dominate the execution
 * time of the run. If so, mixing in this trait into just those suites will allow their long-running tests to run in parallel with each
 * other, thereby helping to reduce the total time required to run an entire run.
 * </p>
 *
 * <p>
 * Because this trait extends <code>OneInstancePerTest</code>,
 * each test will be run its own instance of the suite's class. This trait overrides the 
 * <code>runTests</code> method. If no <code>Distributor</code> is passed to <code>runTests</code>, 
 * this trait's implementation simply invokes its supertrait <code>OneInstancePerTest</code>'s implementation
 * of <code>runTests</code>, which will run each test in its own instance sequentially. If a <code>Distributor</code>
 * is passed, however, this traits' implementation of <code>runTests</code> will, for each test, wrap a new instance of the
 * suite in a special <em>wrapper suite</em> that will invoke just that one test, and passes the wrapper suites to the <code>Distributor</code>.
 * The thread or entity that takes a wrapper suite from the <code>Distributor</code> will invoke <code>run</code>
 * on the wrapper suite, which will run just one test. In this way, different tests of a suite that mixes in
 * <code>ParallelTestExecution</code> will run in parallel.
 * </p>
 *
 * @author Bill Venners
 */
trait ParallelTestExecution extends OneInstancePerTest {

  this: Suite =>

  // Skipping runTests here, but that's OK, because by mixing in ParallelTestExecution, the programmer decided
  // that the super.runTests should be replaced by the one defined in ParallelTestExecution.
  private[scalatest] def runOneTest(testName: String, reporter: Reporter, stopper: Stopper,
                         configMap: Map[String, Any], tracker: Tracker) {

    runTest(testName, reporter, stopper, configMap, tracker)
  }

  /**
   * Run the tests of this suite in parallel.
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


    // testName distributor
    //    None    None      call super, because no distributor
    //    Some    None      call super, because no distributor
    //    None    Some      wrap a newInstance and put it in the distributor
    //    Some    Some      this would be the one where we need to actually run the test, ignore the distributor
    distributor match {
      // If there's no distributor, then just run sequentially, via the regular OneInstancePerTest
      // algorithm
      case None => super.runTests(testName, reporter,stopper, filter, configMap, distributor, tracker)
      case Some(distribute) =>
        testName match {
          // The only way both testName and distributor should be defined is if someone called from the
          // outside and did this. First run is called with testName None and a defined Distributor, it
          // will not get here. So in this case, just do the usual OneInstancePerTest thing.
          // TODO: Make sure it doesn't get back here. Walk through the scenarios.
          case Some(tn) => super.runTests(testName, reporter, stopper, filter, configMap, distributor, tracker)
          case None =>
            for (tn <- testNames) {
              val wrappedInstance =
                new DistributedTestRunnerSuite(
                  newInstance.asInstanceOf[ParallelTestExecution],
                  tn
                )
              distribute(wrappedInstance, tracker.nextTracker)
            }
        }
    }
  }
}
