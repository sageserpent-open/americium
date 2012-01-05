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

// Tag classes used in tests
package mytags {
  object SlowAsMolasses extends Tag("org.scalatest.SlowAsMolasses")
  object FastAsLight extends Tag("org.scalatest.FastAsLight")
  object WeakAsAKitten extends Tag("org.scalatest.WeakAsAKitten")
}

class FunSuiteSuite extends Suite with SharedHelpers {

  def testThatTestMethodsWithNoTagsDontShowUpInTagsMap() {
    
    val a = new FunSuite {
      test("test not in a group") {}
    }
    assert(a.tags.keySet.size === 0)
  }

  def testThatTestFunctionsThatResultInNonUnitAreRegistered() {
    val a = new FunSuite {
      test("test this") { 1 }
      test("test that") { "hi" }
    }
    assert(a.expectedTestCount(Filter()) === 2)
    assert(a.testNames.size === 2)
    assert(a.tags.keySet.size === 0)
  }

  def testThatTestNameCantBeReused() {
    intercept[DuplicateTestNameException] {
      new FunSuite {
        test("test this") {}
        test("test this") {}
      }
    }
    intercept[DuplicateTestNameException] {
      new FunSuite {
        ignore("test this") {}
        test("test this") {}
      }
    }
    intercept[DuplicateTestNameException] {
      new FunSuite {
        test("test this") {}
        ignore("test this") {}
      }
    }
    intercept[DuplicateTestNameException] {
      new FunSuite {
        ignore("test this") {}
        ignore("test this") {}
      }
    }
  }
  
  def testThatIfYouCallTestAfterExecuteYouGetAnTestFailedExceptionAndTheTestDoesntRun() {
    class MySuite extends FunSuite {
      var fromMethodTestExecuted = false
      var fromConstructorTestExecuted = false
      test("from constructor") {
        fromConstructorTestExecuted = true
      }
      def registerOne() {
        test("from method") {
          fromMethodTestExecuted = true
        }
      }
    }
    val a = new MySuite
    a.execute()
    assert(a.fromConstructorTestExecuted)
    assert(!a.fromMethodTestExecuted)
    intercept[TestRegistrationClosedException] {
      a.registerOne()
    }
    a.execute()
    assert(!a.fromMethodTestExecuted)
  }
  
  def testThatInfoInsideATestMethodGetsOutTheDoor() {
    class MyReporter extends Reporter {
      var infoProvidedReceived = false
      var lastEvent: InfoProvided = null
      def apply(event: Event) {
        event match {
          case event: InfoProvided =>
            infoProvidedReceived = true
            lastEvent = event
          case _ =>
        }
      }
    }
    val msg = "hi there, dude"
    class MySuite extends FunSuite {
      test("test this") {
        info(msg)
      }
    }
    val a = new MySuite
    val myRep = new MyReporter
    a.run(None, myRep, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(myRep.infoProvidedReceived)
    assert(myRep.lastEvent.message === msg)
  }
  
  def testThatInfoInTheConstructorGetsOutTheDoor() {
    class MyReporter extends Reporter {
      var infoProvidedReceived = false
      var lastEvent: InfoProvided = null
      def apply(event: Event) {
        event match {
          case event: InfoProvided =>
            infoProvidedReceived = true
            lastEvent = event
          case _ =>
        }
      }
    }
    val msg = "hi there, dude"
    class MySuite extends FunSuite {
      info(msg)
      test("test this") {
      }
    }
    val a = new MySuite
    val myRep = new MyReporter
    a.run(None, myRep, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(myRep.infoProvidedReceived)
    assert(myRep.lastEvent.message === msg)
  }

  def testThatInfoInTheConstructorBeforeATestHappensFirst() {
    var infoProvidedReceived = false
    var infoProvidedReceivedBeforeTest = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case event: InfoProvided =>
            infoProvidedReceived = true
          case _ =>
        }
      }
    }
    val msg = "hi there, dude"
    class MySuite extends FunSuite {
      info(msg)
      test("test this") {
        if (infoProvidedReceived)
          infoProvidedReceivedBeforeTest = true
      }
    }
    val a = new MySuite
    val myRep = new MyReporter
    a.run(None, myRep, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(infoProvidedReceivedBeforeTest)
  }

  def testThatInfoInTheConstructorAfterATestHappensSecond() {
    var infoProvidedReceived = false
    var infoProvidedReceivedAfterTest = true
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case event: InfoProvided =>
            infoProvidedReceived = true
          case _ =>
        }
      }
    }
    val msg = "hi there, dude"
    class MySuite extends FunSuite {
      test("test this") {
        if (infoProvidedReceived)
          infoProvidedReceivedAfterTest = false
      }
      info(msg)
    }
    val a = new MySuite
    val myRep = new MyReporter
    a.run(None, myRep, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(infoProvidedReceivedAfterTest)
    assert(infoProvidedReceived)
  }

  def callingTestFromWithinATestClauseResultsInATestFailedErrorAtRuntime() {

    var testFailedAsExpected = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case event: TestFailed =>
            if (event.testName.indexOf("this test should blow up") != -1)
              testFailedAsExpected = true
          case _ =>
        }
      }
    }

    class MySuite extends FunSuite {
      test("this test should blow up") {
        test("is in the wrong place also") {
          assert(1 === 1)
        }
      }
    }

    val a = new MySuite
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testFailedAsExpected)
  }

  def callingTestFromWithinATestWithTagsClauseResultsInATestFailedErrorAtRuntime() {
    
    var testFailedAsExpected = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case event: TestFailed =>
          if (event.testName.indexOf("this test should blow up") != -1)
            testFailedAsExpected = true
          case _ =>
        }
      }
    }

    class MySuite extends FunSuite {
      test("this test should blow up") {
        test("is in the wrong place also", new Tag("SlowAsMolasses")) {
          assert(1 === 1)
        }
      }
    }

    val a = new MySuite
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testFailedAsExpected)
  }

  def callingIgnoreFromWithinATestClauseResultsInATestFailedErrorAtRuntime() {
    
    var testFailedAsExpected = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case event: TestFailed =>
            if (event.testName.indexOf("this test should blow up") != -1)
              testFailedAsExpected = true
          case _ =>
        }
      }
    }

    class MySuite extends FunSuite {
      test("this test should blow up") {
        ignore("is in the wrong place also") {
          assert(1 === 1)
        }
      }
    }

    val a = new MySuite
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testFailedAsExpected)
  }

  def callingIgnoreWithTagsFromWithinATestClauseResultsInATestFailedErrorAtRuntime() {
    
    var testFailedAsExpected = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case event: TestFailed =>
            if (event.testName.indexOf("this test should blow up") != -1)
              testFailedAsExpected = true
          case _ =>
        }
      }
    }

    class MySuite extends FunSuite {
      test("this test should blow up") {
        ignore("is in the wrong place also", mytags.SlowAsMolasses) {
          assert(1 === 1)
        }
      }
    }

    val a = new MySuite
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testFailedAsExpected)
  }

  def testThatTestDurationsAreIncludedInTestFailedAndTestSucceededEventsFiredFromFunSuite() {

    class MyFunSuite extends FunSuite {
      test("that it succeeds") {}
      test("that it fails") { fail() }
    }

    val myFunSuite = new MyFunSuite
    val myReporter = new TestDurationReporter
    myFunSuite.run(None, myReporter, new Stopper {}, Filter(), Map(), None, new Tracker(new Ordinal(99)))
    assert(myReporter.testSucceededWasFiredAndHadADuration)
    assert(myReporter.testFailedWasFiredAndHadADuration)
  }

  def testThatSuiteDurationsAreIncludedInSuiteCompletedEventsFiredFromFunSuite() {

    class MyFunSuite extends FunSuite {
      override def nestedSuites = List(new Suite {})
    }

    val myFunSuite = new MyFunSuite
    val myReporter = new SuiteDurationReporter
    myFunSuite.run(None, myReporter, new Stopper {}, Filter(), Map(), None, new Tracker(new Ordinal(99)))
    assert(myReporter.suiteCompletedWasFiredAndHadADuration)
  }

  def testThatSuiteDurationsAreIncludedInSuiteAbortedEventsFiredFromFunSuite() {

    class SuiteThatAborts extends Suite {
      override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
              config: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
        throw new RuntimeException("Aborting for testing purposes")
      }
    }

    class MyFunSuite extends FunSuite {
      override def nestedSuites = List(new SuiteThatAborts {})
    }

    val myFunSuite = new MyFunSuite
    val myReporter = new SuiteDurationReporter
    myFunSuite.run(None, myReporter, new Stopper {}, Filter(), Map(), None, new Tracker(new Ordinal(99)))
    assert(myReporter.suiteAbortedWasFiredAndHadADuration)
  }

  def testPendingWorksInFunSuite() {

    class MyFunSuite extends FunSuite {
      test("this test is pending") (pending)
    }

    val mySuite = new MyFunSuite
    val myReporter = new PendingReporter
    mySuite.run(None, myReporter, new Stopper {}, Filter(), Map(), None, new Tracker(new Ordinal(99)))
    assert(myReporter.testPendingWasFired)
  }
}

