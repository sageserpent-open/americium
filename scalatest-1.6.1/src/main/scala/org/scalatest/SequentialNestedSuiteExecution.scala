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
 * Trait that causes that the nested suites of any suite it is mixed into to be run sequentially even if
 * a <code>Distributor</code> is passed to <code>runNestedSuites</code>. This trait overrides the 
 * <code>runNestedSuites</code> method and fowards every parameter passed to it to a superclass invocation
 * of <code>runNestedSuites</code>, except it always passes <code>None</code> for the <code>Distributor</code>.
 * Mix in this trait into any suite whose nested suites need to be run sequentially even with the rest of the
 * run is being executed concurrently.
 */
trait SequentialNestedSuiteExecution extends AbstractSuite { this: Suite =>

  /**
   * This trait's implementation of <code>runNestedSuites</code>s invokes <code>runNestedSuites</code> on <code>super</code>,
   * passing in <code>None</code> for the <code>Distributor</code>.
   *
   * @param reporter the <code>Reporter</code> to which results will be reported
   * @param stopper the <code>Stopper</code> that will be consulted to determine whether to stop execution early.
   * @param filter a <code>Filter</code> with which to filter tests based on their tags
   * @param configMap a <code>Map</code> of key-value pairs that can be used by the executing <code>Suite</code> of tests.
   * @param distributor an optional <code>Distributor</code>, into which to put nested <code>Suite</code>s to be run
   *              by another entity, such as concurrently by a pool of threads. If <code>None</code>, nested <code>Suite</code>s will be run sequentially.
   * @param tracker a <code>Tracker</code> tracking <code>Ordinal</code>s being fired by the current thread.
   *         
   * @throws NullPointerException if any passed parameter is <code>null</code>.
   */
  abstract override protected def runNestedSuites(
    reporter: Reporter,
    stopper: Stopper,
    filter: Filter,
    configMap: Map[String, Any],
    distributor: Option[Distributor],
    tracker: Tracker
  ) {
    if (reporter == null)
      throw new NullPointerException("reporter was null")
    if (stopper == null)
      throw new NullPointerException("stopper was null")
    if (filter == null)
      throw new NullPointerException("filter was null")
    if (configMap == null)
      throw new NullPointerException("configMap was null")
    if (distributor == null)
      throw new NullPointerException("distributor was null")
    if (tracker == null)
      throw new NullPointerException("tracker was null")

    super.runNestedSuites(reporter, stopper, filter, configMap, None, tracker)
  }
}
