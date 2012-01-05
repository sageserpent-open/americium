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

import scala.collection.mutable.ListBuffer
import org.scalatest.events.Event
import org.scalatest.events.Ordinal
import org.scalatest.SharedHelpers.SilentReporter

class BeforeAndAfterFunctionsSuite extends FunSuite {

  class TheSuper extends Suite {
    var runTestWasCalled = false
    var runWasCalled = false
    protected override def runTest(testName: String, reporter: Reporter, stopper: Stopper, properties: Map[String, Any], tracker: Tracker) {
      runTestWasCalled = true
      super.runTest(testName, reporter, stopper, properties, tracker)
    }
    override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
                         properties: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
      runWasCalled = true
      super.run(testName, reporter, stopper, filter, properties, distributor, tracker)
    }
  }
  
  class MySuite extends TheSuper with BeforeAndAfterEachFunctions with BeforeAndAfterAllFunctions {

    var beforeEachCalledBeforeRunTest = false
    var afterEachCalledAfterRunTest = false
    var beforeAllCalledBeforeExecute = false
    var afterAllCalledAfterExecute = false

    beforeAll {
      if (!runWasCalled)
        beforeAllCalledBeforeExecute = true
    }
    beforeEach {
      if (!runTestWasCalled)
        beforeEachCalledBeforeRunTest = true
    }
    def testSomething() = ()
    afterEach {
      if (runTestWasCalled)
        afterEachCalledAfterRunTest = true
    }
    afterAll {
      if (runWasCalled)
        afterAllCalledAfterExecute = true
    }
  }

  test("super's runTest must be called") {
    val a = new MySuite
    a.run(None, SilentReporter, new Stopper {}, Filter(), Map("hi" -> "there"), None, new Tracker)
    assert(a.runTestWasCalled)
  }

  test("super's run must be called") {
    val a = new MySuite
    a.run(None, SilentReporter, new Stopper {}, Filter(), Map("hi" -> "there"), None, new Tracker)
    assert(a.runWasCalled)
  }

  test("beforeEach gets called before runTest") {
    val a = new MySuite
    a.run(None, SilentReporter, new Stopper {}, Filter(), Map("hi" -> "there"), None, new Tracker)
    assert(a.beforeEachCalledBeforeRunTest)
  }

  test("afterEach gets called after runTest") {
    val a = new MySuite
    a.run(None, SilentReporter, new Stopper {}, Filter(), Map("hi" -> "there"), None, new Tracker)
    assert(a.afterEachCalledAfterRunTest)
  }

  test("beforeAll gets called before run") {
    val a = new MySuite
    a.run(None, SilentReporter, new Stopper {}, Filter(), Map("hi" -> "there"), None, new Tracker)
    assert(a.beforeAllCalledBeforeExecute)
  }
  
  test("afterAll gets called after run") {
    val a = new MySuite
    a.run(None, SilentReporter, new Stopper {}, Filter(), Map("hi" -> "there"), None, new Tracker)
    assert(a.afterAllCalledAfterExecute)
  }
  
  // test exceptions with runTest
  test("If any invocation of beforeEach completes abruptly with an exception, runTest " +
    "will complete abruptly with the same exception.") {
    
    class MySuite extends Suite with BeforeAndAfterEachFunctions with BeforeAndAfterAllFunctions {
      beforeEach { throw new NumberFormatException } 
    }
    intercept[NumberFormatException] {
      val a = new MySuite
      a.run(Some("july"), StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    }
  }
  
  test("If any call to super.runTest completes abruptly with an exception, runTest " +
    "will complete abruptly with the same exception, however, before doing so, it will invoke afterEach") {
    trait FunkySuite extends Suite {
      protected override def runTest(testName: String, reporter: Reporter, stopper: Stopper, properties: Map[String, Any], tracker: Tracker) {
        throw new NumberFormatException
      }
    }
    class MySuite extends FunkySuite with BeforeAndAfterEachFunctions with BeforeAndAfterAllFunctions {
      var afterEachCalled = false
      afterEach {
        afterEachCalled = true
      }
    }
    val a = new MySuite
    intercept[NumberFormatException] {
      a.run(Some("july"), StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    }
    assert(a.afterEachCalled)
  }
  
  test("If both super.runTest and afterEach complete abruptly with an exception, runTest " + 
    "will complete abruptly with the exception thrown by super.runTest.") {
    trait FunkySuite extends Suite {
      protected override def runTest(testName: String, reporter: Reporter, stopper: Stopper, properties: Map[String, Any], tracker: Tracker) {
        throw new NumberFormatException
      }
    }
    class MySuite extends FunkySuite with BeforeAndAfterEachFunctions with BeforeAndAfterAllFunctions {
      var afterEachCalled = false
      afterEach {
        afterEachCalled = true
        throw new IllegalArgumentException
      }
    }
    val a = new MySuite
    intercept[NumberFormatException] {
      a.run(Some("july"), StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    }
    assert(a.afterEachCalled)
  }
  
  test("If super.runTest returns normally, but afterEach completes abruptly with an " +
    "exception, runTest will complete abruptly with the same exception.") {

    class MySuite extends Suite with BeforeAndAfterEachFunctions with BeforeAndAfterAllFunctions {
      afterEach { throw new NumberFormatException }
      def testJuly() = ()
    }
    intercept[NumberFormatException] {
      val a = new MySuite
      a.run(Some("testJuly"), StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    }
  }
 
  // test exceptions with run
  test("If any invocation of beforeAll completes abruptly with an exception, run " +
    "will complete abruptly with the same exception.") {
    
    class MySuite extends Suite with BeforeAndAfterEachFunctions with BeforeAndAfterAllFunctions {
      beforeAll { throw new NumberFormatException }
      def testJuly() = ()
    }
    intercept[NumberFormatException] {
      val a = new MySuite
      a.run(None, StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    }
  }
 
  test("If any call to super.run completes abruptly with an exception, run " +
    "will complete abruptly with the same exception, however, before doing so, it will invoke afterAll") {
    trait FunkySuite extends Suite {
      override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
                           properties: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
        throw new NumberFormatException
      }
    }
    class MySuite extends FunkySuite with BeforeAndAfterEachFunctions with BeforeAndAfterAllFunctions {
      var afterAllCalled = false
      afterAll {
        afterAllCalled = true
      }
    }
    val a = new MySuite
    intercept[NumberFormatException] {
      a.run(None, StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    }
    assert(a.afterAllCalled)
  }
   
  test("If both super.run and afterAll complete abruptly with an exception, run " + 
    "will complete abruptly with the exception thrown by super.run.") {
    trait FunkySuite extends Suite {
      override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
                           properties: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
        throw new NumberFormatException
      }
    }
    class MySuite extends FunkySuite with BeforeAndAfterEachFunctions with BeforeAndAfterAllFunctions {
      var afterAllCalled = false
      afterAll {
        afterAllCalled = true
        throw new IllegalArgumentException
      }
    }
    val a = new MySuite
    intercept[NumberFormatException] {
      a.run(None, StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    }
    assert(a.afterAllCalled)
  }
  
  test("If super.run returns normally, but afterAll completes abruptly with an " +
    "exception, run will complete abruptly with the same exception.") {
       
    class MySuite extends Suite with BeforeAndAfterEachFunctions with BeforeAndAfterAllFunctions {
      afterAll { throw new NumberFormatException }
      def testJuly() = ()
    }
    intercept[NumberFormatException] {
      val a = new MySuite
      a.run(None, StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    }
  }

  test("If beforeEach is called twice, the second invocation should produce NotAllowedException") {
    var beforeEachRegisteredFirstTime = false
    var beforeEachRegisteredSecondTime = false
    class MySuite extends Suite with BeforeAndAfterEachFunctions {
      var s = "zero"
      beforeEach {
        s = "one"
      }
      beforeEachRegisteredFirstTime = true
      beforeEach {
        s = "two"
      }
      beforeEachRegisteredSecondTime = true
    }
    intercept[NotAllowedException] {
      new MySuite
    }
    assert(beforeEachRegisteredFirstTime)
    assert(!beforeEachRegisteredSecondTime)
  }

  test("If beforeEach is called after run is invoked, the test should fail with NotAllowedException") {
    var beforeEachRegisteredFirstTime = false
    var beforeEachRegisteredSecondTime = false
    class MySuite extends FunSuite with BeforeAndAfterEachFunctions {
      var s = "zero"
      var notAllowedExceptionThrown = false
      test("this one should fail") {
        try {
          beforeEach {
            s = "one"
          }
        }
        catch {
          case _: NotAllowedException => notAllowedExceptionThrown = true
          case e => throw e
        }
      }
    }
    val a = new MySuite
    a.run(None, StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(a.notAllowedExceptionThrown)
  }

  test("If afterEach is called twice, the second invocation should produce NotAllowedException") {
    var afterEachRegisteredFirstTime = false
    var afterEachRegisteredSecondTime = false
    class MySuite extends Suite with BeforeAndAfterEachFunctions {
      var s = "zero"
      afterEach {
        s = "one"
      }
      afterEachRegisteredFirstTime = true
      afterEach {
        s = "two"
      }
      afterEachRegisteredSecondTime = true
    }
    intercept[NotAllowedException] {
      new MySuite
    }
    assert(afterEachRegisteredFirstTime)
    assert(!afterEachRegisteredSecondTime)
  }

  test("If afterEach is called after run is invoked, the test should fail with NotAllowedException") {
    var afterEachRegisteredFirstTime = false
    var afterEachRegisteredSecondTime = false
    class MySuite extends FunSuite with BeforeAndAfterEachFunctions {
      var s = "zero"
      var notAllowedExceptionThrown = false
      test("this one should fail") {
        try {
          afterEach {
            s = "one"
          }
        }
        catch {
          case _: NotAllowedException => notAllowedExceptionThrown = true
          case e => throw e
        }
      }
    }
    val a = new MySuite
    a.run(None, StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(a.notAllowedExceptionThrown)
  }

// All stuff
  test("If beforeAll is called twice, the second invocation should produce NotAllowedException") {
    var beforeAllRegisteredFirstTime = false
    var beforeAllRegisteredSecondTime = false
    class MySuite extends Suite with BeforeAndAfterAllFunctions {
      var s = "zero"
      beforeAll {
        s = "one"
      }
      beforeAllRegisteredFirstTime = true
      beforeAll {
        s = "two"
      }
      beforeAllRegisteredSecondTime = true
    }
    intercept[NotAllowedException] {
      new MySuite
    }
    assert(beforeAllRegisteredFirstTime)
    assert(!beforeAllRegisteredSecondTime)
  }

  test("If beforeAll is called after run is invoked, the test should fail with NotAllowedException") {
    class MySuite extends FunSuite with BeforeAndAfterAllFunctions {
      var s = "zero"
      var notAllowedExceptionThrown = false
      test("this one should fail") {
        try {
          beforeAll {
            s = "one"
          }
        }
        catch {
          case _: NotAllowedException => notAllowedExceptionThrown = true
          case e => throw e
        }
      }
    }
    val a = new MySuite
    a.run(None, StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(a.notAllowedExceptionThrown)
  }

  test("If afterAll is called twice, the second invocation should produce NotAllowedException") {
    var afterAllRegisteredFirstTime = false
    var afterAllRegisteredSecondTime = false
    class MySuite extends Suite with BeforeAndAfterAllFunctions {
      var s = "zero"
      afterAll {
        s = "one"
      }
      afterAllRegisteredFirstTime = true
      afterAll {
        s = "two"
      }
      afterAllRegisteredSecondTime = true
    }
    intercept[NotAllowedException] {
      new MySuite
    }
    assert(afterAllRegisteredFirstTime)
    assert(!afterAllRegisteredSecondTime)
  }

  test("If afterAll is called after run is invoked, the test should fail with NotAllowedException") {
    class MySuite extends FunSuite with BeforeAndAfterAllFunctions {
      var s = "zero"
      var notAllowedExceptionThrown = false
      test("this one should fail") {
        try {
          afterAll {
            s = "one"
          }
        }
        catch {
          case _: NotAllowedException => notAllowedExceptionThrown = true
          case e => throw e
        }
      }
    }
    val a = new MySuite
    a.run(None, StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(a.notAllowedExceptionThrown)
  }
}

class BeforeAndAfterFunctionsExtendingSuite extends Suite with BeforeAndAfterEachFunctions with BeforeAndAfterAllFunctions {

  var sb: StringBuilder = _
  val lb = new ListBuffer[String]

  beforeEach {
    sb = new StringBuilder("ScalaTest is ")
    lb.clear()
  }

  def testEasy() {
    sb.append("easy!")
    assert(sb.toString === "ScalaTest is easy!")
    assert(lb.isEmpty)
    lb += "sweet"
  }

  def testFun() {
    sb.append("fun!")
    assert(sb.toString === "ScalaTest is fun!")
    assert(lb.isEmpty)
  }
}

class BeforeAndAfterFunctionsExtendingFunSuite extends FunSuite with BeforeAndAfterEachFunctions with BeforeAndAfterAllFunctions {

  var sb: StringBuilder = _
  val lb = new ListBuffer[String]

  beforeEach {
    sb = new StringBuilder("ScalaTest is ")
    lb.clear()
  }

  test("easy") {
    sb.append("easy!")
    assert(sb.toString === "ScalaTest is easy!")
    assert(lb.isEmpty)
    lb += "sweet"
  }

  test("fun") {
    sb.append("fun!")
    assert(sb.toString === "ScalaTest is fun!")
    assert(lb.isEmpty)
  }
}


