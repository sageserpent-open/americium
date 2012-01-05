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
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import org.scalatest.events._
import Suite.formatterForSuiteStarting
import Suite.formatterForSuiteCompleted
import Suite.formatterForSuiteAborted

private[scalatest] class SuiteRunner(suite: Suite, dispatch: DispatchReporter, stopper: Stopper, filter: Filter,
    propertiesMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) extends Runnable {

  private val stopRequested = stopper

  def run() {

    if (!stopRequested()) {
      // Create a Rerunner if the Suite has a no-arg constructor
      val hasPublicNoArgConstructor: Boolean =
        try {
          val constructor: Constructor[_] = suite.getClass.getConstructor(Array[java.lang.Class[_]](): _*)
          Modifier.isPublic(constructor.getModifiers())
        }
        catch {
          case nsme: NoSuchMethodException => false
        }
  
      val rerunnable: Option[Rerunner] =
        if (hasPublicNoArgConstructor)
          Some(new SuiteRerunner(suite.getClass.getName))
        else
          None
  
      val rawString = Resources("suiteExecutionStarting")
      val formatter = formatterForSuiteStarting(suite)
  
      val suiteStartTime = System.currentTimeMillis

      if (!suite.isInstanceOf[DistributedTestRunnerSuite])
        dispatch(SuiteStarting(tracker.nextOrdinal(), suite.suiteName, Some(suite.getClass.getName), formatter, rerunnable))
  
      try {
        suite.run(None, dispatch, stopRequested, filter, propertiesMap, distributor, tracker)
  
        val rawString2 = Resources("suiteCompletedNormally")
        val formatter = formatterForSuiteCompleted(suite)

        val duration = System.currentTimeMillis - suiteStartTime
        if (!suite.isInstanceOf[DistributedTestRunnerSuite])
          dispatch(SuiteCompleted(tracker.nextOrdinal(), suite.suiteName, Some(suite.getClass.getName), Some(duration), formatter, rerunnable))
      }
      catch {
        case e: RuntimeException => { // Do fire SuiteAborted even if a DistributedTestRunnerSuite 

          val rawString3 = Resources("executeException")
          val formatter3 = formatterForSuiteAborted(suite, rawString3)

          val duration = System.currentTimeMillis - suiteStartTime
          dispatch(SuiteAborted(tracker.nextOrdinal(), rawString3, suite.suiteName, Some(suite.getClass.getName), Some(e), Some(duration), formatter3, rerunnable))
        }
      }
    }
  }
}
