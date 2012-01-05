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

/**
 * An enumeration of the 13 possible confiuration options accepted
 * the Runner for Reporters.
 *
 * @author Bill Venners
 */
private[tools] sealed abstract class EventToPresent

private[tools] case object PresentRunStarting extends EventToPresent
private[tools] case object PresentTestStarting extends EventToPresent
private[tools] case object PresentTestFailed extends EventToPresent
private[tools] case object PresentTestSucceeded extends EventToPresent
private[tools] case object PresentTestIgnored extends EventToPresent
private[tools] case object PresentTestPending extends EventToPresent
private[tools] case object PresentSuiteStarting extends EventToPresent
private[tools] case object PresentSuiteAborted extends EventToPresent
private[tools] case object PresentSuiteCompleted extends EventToPresent
private[tools] case object PresentInfoProvided extends EventToPresent
private[tools] case object PresentRunStopped extends EventToPresent
private[tools] case object PresentRunAborted extends EventToPresent
private[tools] case object PresentRunCompleted extends EventToPresent

private[tools] object EventToPresent {

  val allEventsToPresent: Set[EventToPresent] =
    Set(
      PresentRunStarting,
      PresentTestStarting,
      PresentTestSucceeded,
      PresentTestFailed,
      PresentTestIgnored,
      PresentTestPending,
      PresentSuiteStarting,
      PresentSuiteCompleted,
      PresentSuiteAborted,
      PresentInfoProvided,
      PresentRunStopped,
      PresentRunCompleted,
      PresentRunAborted
    )

  def eventToEventToPresent(event: org.scalatest.events.Event): EventToPresent =
    event match {
      case _: RunStarting => PresentRunStarting
      case _: TestStarting => PresentTestStarting
      case _: TestSucceeded => PresentTestSucceeded
      case _: TestFailed => PresentTestFailed
      case _: TestIgnored => PresentTestIgnored
      case _: TestPending => PresentTestPending
      case _: SuiteStarting => PresentSuiteStarting
      case _: SuiteCompleted => PresentSuiteCompleted
      case _: SuiteAborted => PresentSuiteAborted
      case _: InfoProvided => PresentInfoProvided
      case _: RunStopped => PresentRunStopped
      case _: RunCompleted => PresentRunCompleted
      case _: RunAborted => PresentRunAborted
    }
}
