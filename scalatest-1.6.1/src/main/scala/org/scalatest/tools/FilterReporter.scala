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
package org.scalatest.tools

import org.scalatest._
import org.scalatest.events._
import DispatchReporter.propagateDispose

/**
 * FiterReporter catches exceptions that may be thrown by custom reporters, and doesn't forward
 * reports that were not selected by the passed configuration.
 *
 * @author Bill Venners
 */
private[tools] class FilterReporter(reporter: Reporter, configSet: Set[ReporterConfigParam]) extends ResourcefulReporter {

  // def reFilter(configSet: EventToPresent.Set32) = new FilterReporter(reporter, configSet)

  override def apply(event: Event) {
    val report = reporter
    event match {
      case event: RunStarting => report(event)
      case event: RunCompleted => report(event)
      case event: RunAborted => report(event)
      case event: RunStopped => report(event)
      case event: SuiteAborted => report(event)
      case event: TestFailed =>report(event)
      case event: SuiteCompleted => if (!configSet.contains(FilterSuiteCompleted)) report(event)
      case event: SuiteStarting => if (!configSet.contains(FilterSuiteStarting)) report(event)
      case event: TestStarting => if (!configSet.contains(FilterTestStarting)) report(event)
      case event: TestSucceeded => if (!configSet.contains(FilterTestSucceeded)) report(event)
      case event: TestIgnored => if (!configSet.contains(FilterTestIgnored)) report(event)
      case event: TestPending => if (!configSet.contains(FilterTestPending)) report(event)
      case event: InfoProvided => if (!configSet.contains(FilterInfoProvided)) report(event)
    }
  }

  override def dispose() = propagateDispose(reporter)
}
// Have some methods that translate chars & strings to Opts things, and vice versa?
