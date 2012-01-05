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

import org.scalatest.events._

trait SharedHelpers extends Assertions {

  object SilentReporter extends Reporter {
    def apply(event: Event) = ()  
  }

  class TestDurationReporter extends Reporter {
    var testSucceededWasFiredAndHadADuration = false
    var testFailedWasFiredAndHadADuration = false
    override def apply(event: Event) {
      event match {
        case event: TestSucceeded => testSucceededWasFiredAndHadADuration = event.duration.isDefined
        case event: TestFailed => testFailedWasFiredAndHadADuration = event.duration.isDefined
        case _ =>
      }
    }
  }

  class SuiteDurationReporter extends Reporter {
    var suiteCompletedWasFiredAndHadADuration = false
    var suiteAbortedWasFiredAndHadADuration = false
    override def apply(event: Event) {
      event match {
        case event: SuiteCompleted => suiteCompletedWasFiredAndHadADuration = event.duration.isDefined
        case event: SuiteAborted => suiteAbortedWasFiredAndHadADuration = event.duration.isDefined
        case _ =>
      }
    }
  }

  class PendingReporter extends Reporter {
    var testPendingWasFired = false
    override def apply(event: Event) {
      event match {
        case _: TestPending => testPendingWasFired = true
        case _ =>
      }
    }
  }

  class EventRecordingReporter extends Reporter {
    private var eventList: List[Event] = List()
    def eventsReceived = eventList.reverse
    def testSucceededEventsReceived: List[TestSucceeded] = {
      eventsReceived filter {
        case event: TestSucceeded => true
        case _ => false
      } map {
        case event: TestSucceeded => event
        case _ => throw new RuntimeException("should never happen")
      }
    }
    // Why doesn't this work:
    // for (event: TestSucceeded <- eventsReceived) yield event
    def infoProvidedEventsReceived: List[InfoProvided] = {
      eventsReceived filter {
        case event: InfoProvided => true
        case _ => false
      } map {
        case event: InfoProvided => event
        case _ => throw new RuntimeException("should never happen")
      }
    }
    def testPendingEventsReceived: List[TestPending] = {
      eventsReceived filter {
        case event: TestPending => true
        case _ => false
      } map {
        case event: TestPending => event
        case _ => throw new RuntimeException("should never happen")
      }
    }
    def testFailedEventsReceived: List[TestFailed] = {
      eventsReceived filter {
        case event: TestFailed => true
        case _ => false
      } map {
        case event: TestFailed => event
        case _ => throw new RuntimeException("should never happen")
      }
    }
    def testIgnoredEventsReceived: List[TestIgnored] = {
      eventsReceived filter {
        case event: TestIgnored => true
        case _ => false
      } map {
        case event: TestIgnored => event
        case _ => throw new RuntimeException("should never happen")
      }
    }
    def apply(event: Event) {
      eventList ::= event
    }
  }

  def getIndexesForInformerEventOrderTests(suite: Suite, testName: String, infoMsg: String): (Int, Int, Int) = {

    val myRep = new EventRecordingReporter
    suite.run(None, myRep, new Stopper {}, Filter(), Map(), None, new Tracker)

    val indexedList = myRep.eventsReceived.zipWithIndex

    val testStartingOption = indexedList.find(_._1.isInstanceOf[TestStarting])
    val infoProvidedOption = indexedList.find(_._1.isInstanceOf[InfoProvided])
    val testSucceededOption = indexedList.find(_._1.isInstanceOf[TestSucceeded])

    assert(testStartingOption.isDefined)
    assert(infoProvidedOption.isDefined)
    assert(testSucceededOption.isDefined)

    val testStartingIndex = testStartingOption.get._2
    val infoProvidedIndex = infoProvidedOption.get._2
    val testSucceededIndex = testSucceededOption.get._2

    val testStarting = testStartingOption.get._1.asInstanceOf[TestStarting]
    val infoProvided = infoProvidedOption.get._1.asInstanceOf[InfoProvided]
    val testSucceeded = testSucceededOption.get._1.asInstanceOf[TestSucceeded]

    assert(testStarting.testName === testName)
    assert(infoProvided.message === infoMsg)
    assert(testSucceeded.testName === testName)

    (infoProvidedIndex, testStartingIndex, testSucceededIndex)
  }

  def getIndentedTextFromInfoProvided(suite: Suite): IndentedText = {

    val myRep = new EventRecordingReporter
    suite.run(None, myRep, new Stopper {}, Filter(), Map(), None, new Tracker)

    val infoProvidedOption = myRep.eventsReceived.find(_.isInstanceOf[InfoProvided])

    infoProvidedOption match {
      case Some(infoProvided: InfoProvided) =>
        infoProvided.formatter match {
          case Some(indentedText: IndentedText) => indentedText
          case _ => fail("An InfoProvided was received that didn't include an IndentedText formatter: " + infoProvided.formatter)
        }
      case _ => fail("No InfoProvided was received by the Reporter during the run.")
    }
  }

  def ensureTestFailedEventReceived(suite: Suite, testName: String) {
    val reporter = new EventRecordingReporter
    suite.run(None, reporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    val testFailedEvent = reporter.eventsReceived.find(_.isInstanceOf[TestFailed])
    assert(testFailedEvent.isDefined)
    assert(testFailedEvent.get.asInstanceOf[TestFailed].testName === testName)
  }

  class TestIgnoredTrackingReporter extends Reporter {
    var testIgnoredReceived = false
    var lastEvent: Option[TestIgnored] = None
    def apply(event: Event) {
      event match {
        case event: TestIgnored =>
          testIgnoredReceived = true
          lastEvent = Some(event)
        case _ =>
      }
    }
  }
}

// Selfless trait pattern
object SharedHelpers extends SharedHelpers

