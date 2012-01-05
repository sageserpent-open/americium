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

import collection.immutable.TreeSet
import org.scalatest.events._

class SuiteSpec extends Spec with PrivateMethodTester with SharedHelpers {

  describe("The simpleNameForTest method") {
    it("should return the correct test simple name with or without Informer") {
      val simpleNameForTest = PrivateMethod[String]('simpleNameForTest)
      assert((Suite invokePrivate simpleNameForTest("testThis")) === "testThis")
      assert((Suite invokePrivate simpleNameForTest("testThis(Informer)")) === "testThis")
      assert((Suite invokePrivate simpleNameForTest("test(Informer)")) === "test")
      assert((Suite invokePrivate simpleNameForTest("test")) === "test")
    }
  }

  describe("A Suite") {
    it("should return the test names in alphabetical order from testNames") {
      val a = new Suite {
        def testThis() {}
        def testThat() {}
      }

      expect(List("testThat", "testThis")) {
        a.testNames.iterator.toList
      }

      val b = new Suite {}

      expect(List[String]()) {
        b.testNames.iterator.toList
      }

      val c = new Suite {
        def testThat() {}
        def testThis() {}
      }

      expect(List("testThat", "testThis")) {
        c.testNames.iterator.toList
      }
    }
    
    it("should return the proper testNames for test methods whether or not they take an Informer") {

      val a = new Suite {
        def testThis() = ()
        def testThat(info: Informer) = ()
      }
      assert(a.testNames === TreeSet("testThat(Informer)", "testThis"))

      val b = new Suite {}
      assert(b.testNames === TreeSet[String]())
    }

    it("should return a correct tags map from the tags method") {

      val a = new Suite {
        @Ignore
        def testThis() = ()
        def testThat(info: Informer) = ()
      }

      assert(a.tags === Map("testThis" -> Set("org.scalatest.Ignore")))

      val b = new Suite {
        def testThis() = ()
        @Ignore
        def testThat(info: Informer) = ()
      }

      assert(b.tags === Map("testThat(Informer)" -> Set("org.scalatest.Ignore")))

      val c = new Suite {
        @Ignore
        def testThis() = ()
        @Ignore
        def testThat(info: Informer) = ()
      }

      assert(c.tags === Map("testThis" -> Set("org.scalatest.Ignore"), "testThat(Informer)" -> Set("org.scalatest.Ignore")))

      val d = new Suite {
        @SlowAsMolasses
        def testThis() = ()
        @SlowAsMolasses
        @Ignore
        def testThat(info: Informer) = ()
      }

      assert(d.tags === Map("testThis" -> Set("org.scalatest.SlowAsMolasses"), "testThat(Informer)" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses")))

      val e = new Suite {}
      assert(e.tags === Map())
    }

    class TestWasCalledSuite extends Suite {
      var theTestThisCalled = false
      var theTestThatCalled = false
      def testThis() { theTestThisCalled = true }
      def testThat() { theTestThatCalled = true }
    }

    it("should execute all tests when run is called with testName None") {

      val b = new TestWasCalledSuite
      b.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(b.theTestThisCalled)
      assert(b.theTestThatCalled)
    }

    it("should execute one test when run is called with a defined testName") {

      val a = new TestWasCalledSuite
      a.run(Some("testThis"), SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(a.theTestThisCalled)
      assert(!a.theTestThatCalled)
    }

    it("should report as ignored, ant not run, tests marked ignored") {

      val a = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        def testThis() { theTestThisCalled = true }
        def testThat(info: Informer) { theTestThatCalled = true }
      }

      val repA = new TestIgnoredTrackingReporter
      a.run(None, repA, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repA.testIgnoredReceived)
      assert(a.theTestThisCalled)
      assert(a.theTestThatCalled)

      val b = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        @Ignore
        def testThis() { theTestThisCalled = true }
        def testThat(info: Informer) { theTestThatCalled = true }
      }

      val repB = new TestIgnoredTrackingReporter
      b.run(None, repB, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(repB.testIgnoredReceived)
      assert(repB.lastEvent.isDefined)
      assert(repB.lastEvent.get.testName endsWith "testThis")
      assert(!b.theTestThisCalled)
      assert(b.theTestThatCalled)

      val c = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        def testThis() { theTestThisCalled = true }
        @Ignore
        def testThat(info: Informer) { theTestThatCalled = true }
      }

      val repC = new TestIgnoredTrackingReporter
      c.run(None, repC, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(repC.testIgnoredReceived)
      assert(repC.lastEvent.isDefined)
      assert(repC.lastEvent.get.testName endsWith "testThat(Informer)", repC.lastEvent.get.testName)
      assert(c.theTestThisCalled)
      assert(!c.theTestThatCalled)

      val d = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        @Ignore
        def testThis() { theTestThisCalled = true }
        @Ignore
        def testThat(info: Informer) { theTestThatCalled = true }
      }

      val repD = new TestIgnoredTrackingReporter
      d.run(None, repD, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(repD.testIgnoredReceived)
      assert(repD.lastEvent.isDefined)
      assert(repD.lastEvent.get.testName endsWith "testThis") // last because run alphabetically
      assert(!d.theTestThisCalled)
      assert(!d.theTestThatCalled)
    }
    
    it("should run a test marked as ignored if run is invoked with that testName") {

      val e = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        @Ignore
        def testThis() { theTestThisCalled = true }
        def testThat(info: Informer) { theTestThatCalled = true }
      }

      val repE = new TestIgnoredTrackingReporter
      e.run(Some("testThis"), repE, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repE.testIgnoredReceived)
      assert(e.theTestThisCalled)
      assert(!e.theTestThatCalled)
    }

    it("should throw IllegalArgumentException if run is passed a testName that does not exist") {
      val suite = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        def testThis() { theTestThisCalled = true }
        def testThat(info: Informer) { theTestThatCalled = true }
      }

      intercept[IllegalArgumentException] {
        // Here, they forgot that the name is actually testThis(Fixture)
        suite.run(Some("misspelled"), SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      }
    }

    it("should run only those tests selected by the tags to include and exclude sets") {

      // Nothing is excluded
      val a = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        @SlowAsMolasses
        def testThis() { theTestThisCalled = true }
        def testThat(info: Informer) { theTestThatCalled = true }
      }
      val repA = new TestIgnoredTrackingReporter
      a.run(None, repA, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repA.testIgnoredReceived)
      assert(a.theTestThisCalled)
      assert(a.theTestThatCalled)

      // SlowAsMolasses is included, one test should be excluded
      val b = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        @SlowAsMolasses
        def testThis() { theTestThisCalled = true }
        def testThat(info: Informer) { theTestThatCalled = true }
      }
      val repB = new TestIgnoredTrackingReporter
      b.run(None, repB, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set()), Map(), None, new Tracker)
      assert(!repB.testIgnoredReceived)
      assert(b.theTestThisCalled)
      assert(!b.theTestThatCalled)

      // SlowAsMolasses is included, and both tests should be included
      val c = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        @SlowAsMolasses
        def testThis() { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(info: Informer) { theTestThatCalled = true }
      }
      val repC = new TestIgnoredTrackingReporter
      c.run(None, repB, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set()), Map(), None, new Tracker)
      assert(!repC.testIgnoredReceived)
      assert(c.theTestThisCalled)
      assert(c.theTestThatCalled)

      // SlowAsMolasses is included. both tests should be included but one ignored
      val d = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        @Ignore
        @SlowAsMolasses
        def testThis() { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(info: Informer) { theTestThatCalled = true }
      }
      val repD = new TestIgnoredTrackingReporter
      d.run(None, repD, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.Ignore")), Map(), None, new Tracker)
      assert(repD.testIgnoredReceived)
      assert(!d.theTestThisCalled)
      assert(d.theTestThatCalled)

      // SlowAsMolasses included, FastAsLight excluded
      val e = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @FastAsLight
        @SlowAsMolasses
        def testThis() { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(info: Informer) { theTestThatCalled = true }
        def testTheOther(info: Informer) { theTestTheOtherCalled = true }
      }
      val repE = new TestIgnoredTrackingReporter
      e.run(None, repE, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repE.testIgnoredReceived)
      assert(!e.theTestThisCalled)
      assert(e.theTestThatCalled)
      assert(!e.theTestTheOtherCalled)

      // An Ignored test that was both included and excluded should not generate a TestIgnored event
      val f = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @Ignore
        @FastAsLight
        @SlowAsMolasses
        def testThis() { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(info: Informer) { theTestThatCalled = true }
        def testTheOther(info: Informer) { theTestTheOtherCalled = true }
      }
      val repF = new TestIgnoredTrackingReporter
      f.run(None, repF, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repF.testIgnoredReceived)
      assert(!f.theTestThisCalled)
      assert(f.theTestThatCalled)
      assert(!f.theTestTheOtherCalled)

      // An Ignored test that was not included should not generate a TestIgnored event
      val g = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @FastAsLight
        @SlowAsMolasses
        def testThis() { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(info: Informer) { theTestThatCalled = true }
        @Ignore
        def testTheOther(info: Informer) { theTestTheOtherCalled = true }
      }
      val repG = new TestIgnoredTrackingReporter
      g.run(None, repG, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repG.testIgnoredReceived)
      assert(!g.theTestThisCalled)
      assert(g.theTestThatCalled)
      assert(!g.theTestTheOtherCalled)

      // No tagsToInclude set, FastAsLight excluded
      val h = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @FastAsLight
        @SlowAsMolasses
        def testThis() { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(info: Informer) { theTestThatCalled = true }
        def testTheOther(info: Informer) { theTestTheOtherCalled = true }
      }
      val repH = new TestIgnoredTrackingReporter
      h.run(None, repH, new Stopper {}, Filter(None, Set("org.scalatest.FastAsLight")), Map(), None, new Tracker)
      assert(!repH.testIgnoredReceived)
      assert(!h.theTestThisCalled)
      assert(h.theTestThatCalled)
      assert(h.theTestTheOtherCalled)

      // No tagsToInclude set, SlowAsMolasses excluded
      val i = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @FastAsLight
        @SlowAsMolasses
        def testThis() { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(info: Informer) { theTestThatCalled = true }
        def testTheOther(info: Informer) { theTestTheOtherCalled = true }
      }
      val repI = new TestIgnoredTrackingReporter
      i.run(None, repI, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses")), Map(), None, new Tracker)
      assert(!repI.testIgnoredReceived)
      assert(!i.theTestThisCalled)
      assert(!i.theTestThatCalled)
      assert(i.theTestTheOtherCalled)

      // No tagsToInclude set, SlowAsMolasses excluded, TestIgnored should not be received on excluded ones
      val j = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @Ignore
        @FastAsLight
        @SlowAsMolasses
        def testThis() { theTestThisCalled = true }
        @Ignore
        @SlowAsMolasses
        def testThat(info: Informer) { theTestThatCalled = true }
        def testTheOther(info: Informer) { theTestTheOtherCalled = true }
      }
      val repJ = new TestIgnoredTrackingReporter
      j.run(None, repJ, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses")), Map(), None, new Tracker)
      assert(!repI.testIgnoredReceived)
      assert(!j.theTestThisCalled)
      assert(!j.theTestThatCalled)
      assert(j.theTestTheOtherCalled)

      // Same as previous, except Ignore specifically mentioned in excludes set
      val k = new Suite {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @Ignore
        @FastAsLight
        @SlowAsMolasses
        def testThis() { theTestThisCalled = true }
        @Ignore
        @SlowAsMolasses
        def testThat(info: Informer) { theTestThatCalled = true }
        @Ignore
        def testTheOther(info: Informer) { theTestTheOtherCalled = true }
      }
      val repK = new TestIgnoredTrackingReporter
      k.run(None, repK, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses", "org.scalatest.Ignore")), Map(), None, new Tracker)
      assert(repK.testIgnoredReceived)
      assert(!k.theTestThisCalled)
      assert(!k.theTestThatCalled)
      assert(!k.theTestTheOtherCalled)
    }

    it("should return the correct test count from its expectedTestCount method") {

      val a = new Suite {
        def testThis() = ()
        def testThat(info: Informer) = ()
      }
      assert(a.expectedTestCount(Filter()) === 2)

      val b = new Suite {
        @Ignore
        def testThis() = ()
        def testThat(info: Informer) = ()
      }
      assert(b.expectedTestCount(Filter()) === 1)

      val c = new Suite {
        @FastAsLight
        def testThis() = ()
        def testThat(info: Informer) = ()
      }
      assert(c.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(c.expectedTestCount(Filter(None, Set("org.scalatest.FastAsLight"))) === 1)

      val d = new Suite {
        @FastAsLight
        @SlowAsMolasses
        def testThis() = ()
        @SlowAsMolasses
        def testThat(info: Informer) = ()
        def testTheOtherThing(info: Informer) = ()
      }
      assert(d.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(d.expectedTestCount(Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight"))) === 1)
      assert(d.expectedTestCount(Filter(None, Set("org.scalatest.SlowAsMolasses"))) === 1)
      assert(d.expectedTestCount(Filter()) === 3)

      val e = new Suite {
        @FastAsLight
        @SlowAsMolasses
        def testThis() = ()
        @SlowAsMolasses
        def testThat(info: Informer) = ()
        @Ignore
        def testTheOtherThing(info: Informer) = ()
      }
      assert(e.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(e.expectedTestCount(Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight"))) === 1)
      assert(e.expectedTestCount(Filter(None, Set("org.scalatest.SlowAsMolasses"))) === 0)
      assert(e.expectedTestCount(Filter()) === 2)

      val f = new Suites(a, b, c, d, e)
      assert(f.expectedTestCount(Filter()) === 10)
    }

    it("should generate a TestPending message when the test body is (pending)") {
      val a = new Suite {

        def testDoThis() { pending }

        def testDoThat() {
          assert(2 + 2 === 4)
        }

        def testDoSomethingElse() {
          assert(2 + 2 === 4)
          pending
        }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      val tp = rep.testPendingEventsReceived
      assert(tp.size === 2)
    }
    it("should generate a test failure if a Throwable, or an Error other than direct Error subtypes " +
            "known in JDK 1.5, excluding AssertionError") {
      val a = new Suite {
        def testThrowsAssertionError() { throw new AssertionError }
        def testThrowsPlainOldError() { throw new Error }
        def testThrowsThrowable() { throw new Throwable }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      val tf = rep.testFailedEventsReceived
      assert(tf.size === 3)
    }
    it("should propagate out Errors that are direct subtypes of Error in JDK 1.5, other than " +
            "AssertionError, causing Suites and Runs to abort.") {
      val a = new Suite {
        def testThrowsAssertionError() { throw new OutOfMemoryError }
      }
      intercept[OutOfMemoryError] {
        a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      }
    }
    it("should invoke withFixture from runTest for no-arg test method") {
      val a = new Suite {
        var withFixtureWasInvoked = false
        var testWasInvoked = false
        override def withFixture(test: NoArgTest) {
          withFixtureWasInvoked = true
          super.withFixture(test)
        }
        def testSomething() {
          testWasInvoked = true
        }
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.withFixtureWasInvoked)
      assert(a.testWasInvoked)
    }
    it("should invoke withFixture from runTest for a test method that takes an Informer") {
      val a = new Suite {
        var withFixtureWasInvoked = false
        var testWasInvoked = false
        override def withFixture(test: NoArgTest) {
          withFixtureWasInvoked = true
          super.withFixture(test)
        }
        def testSomething(info: Informer) {
          testWasInvoked = true
        }
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.withFixtureWasInvoked)
      assert(a.testWasInvoked)
    }
    it("should pass the correct test name in the NoArgTest passed to withFixture") {
      val a = new Suite {
        var correctTestNameWasPassed = false
        override def withFixture(test: NoArgTest) {
          correctTestNameWasPassed = test.name == "testSomething(Informer)"
          super.withFixture(test)
        }
        def testSomething(info: Informer) {}
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.correctTestNameWasPassed)
    }
    it("should pass the correct config map in the NoArgTest passed to withFixture") {
      val a = new Suite {
        var correctConfigMapWasPassed = false
        override def withFixture(test: NoArgTest) {
          correctConfigMapWasPassed = (test.configMap == Map("hi" -> 7))
          super.withFixture(test)
        }
        def testSomething(info: Informer) {}
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map("hi" -> 7), None, new Tracker())
      assert(a.correctConfigMapWasPassed)
    }

    describe("(when its pendingUntilFixed method is invoked)") {
      it("should throw TestPendingException if the code block throws an exception") {
        intercept[TestPendingException] {
          pendingUntilFixed {
            assert(1 + 1 === 3)
          }
        }
      }
      it("should throw TestFailedException if the code block doesn't throw an exception") {
        intercept[TestFailedException] {
          pendingUntilFixed {
            assert(1 + 2 === 3)
          }
        }
      }
    }
    it("should, when a test methods takes an Informer and writes to it, report the info after the test completion event") {
      val msg = "hi there dude"
      class MySuite extends Suite {
        def testWithInformer(info: Informer) {
          info(msg)
        }
      }
      val (infoProvidedIndex, testStartingIndex, testSucceededIndex) =
        getIndexesForInformerEventOrderTests(new MySuite, "testWithInformer(Informer)", msg)
      assert(testStartingIndex < testSucceededIndex)
      assert(testSucceededIndex < infoProvidedIndex)
    }
  }
  describe("the stopper") {
    it("should stop nested suites from being executed") {
      class SuiteA extends Suite {
        var executed = false;
        override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
              configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
          executed = true
          super.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
        }
      }
      class SuiteB extends Suite {
        var executed = false;
        override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
              configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
          executed = true
          super.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
        }
      }
      class SuiteC extends Suite {
        var executed = false;
        override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
              configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
          executed = true
          super.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
        }
      }
      class SuiteD extends Suite {
        var executed = false;
        override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
              configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
          executed = true
          super.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
          stopper match {
            case s: MyStopper => s.stop = true
            case _ =>
          }
        }
      }
      class SuiteE extends Suite {
        var executed = false;
        override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
              configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
          executed = true
          super.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
        }
      }
      class SuiteF extends Suite {
        var executed = false;
        override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
              configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
          executed = true
          super.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
        }
      }
      class SuiteG extends Suite {
        var executed = false;
        override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
              configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
          executed = true
          super.run(testName, reporter, stopper, filter, configMap, distributor, tracker)
        }
      }

      val a = new SuiteA
      val b = new SuiteB
      val c = new SuiteC
      val d = new SuiteD
      val e = new SuiteE
      val f = new SuiteF
      val g = new SuiteG

      val x = Suites(a, b, c, d, e, f, g)
      x.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)

      assert(a.executed)
      assert(b.executed)
      assert(c.executed)
      assert(d.executed)
      assert(e.executed)
      assert(f.executed)
      assert(g.executed)

      class MyStopper extends Stopper {
        var stop = false
        override def apply() = stop
      }

      val h = new SuiteA
      val i = new SuiteB
      val j = new SuiteC
      val k = new SuiteD
      val l = new SuiteE
      val m = new SuiteF
      val n = new SuiteG

      val y = Suites(h, i, j, k, l, m, n)
      y.run(None, SilentReporter, new MyStopper, Filter(), Map(), None, new Tracker)

      assert(k.executed)
      assert(i.executed)
      assert(j.executed)
      assert(k.executed)
      assert(!l.executed)
      assert(!m.executed)
      assert(!n.executed)
    }

    it("should stop tests from being executed") {

      class MySuite extends Suite {
        var testsExecutedCount = 0
        def test1() { testsExecutedCount += 1 }
        def test2() { testsExecutedCount += 1 }
        def test3() { testsExecutedCount += 1 }
        def test4() {
          testsExecutedCount += 1
        }
        def test5() { testsExecutedCount += 1 }
        def test6() { testsExecutedCount += 1 }
        def test7() { testsExecutedCount += 1 }
      }

      val x = new MySuite
      x.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(x.testsExecutedCount === 7)

      class MyStopper extends Stopper {
        var stop = false
        override def apply() = stop
      }

      val myStopper = new MyStopper

      class MyStoppingSuite extends Suite {
        var testsExecutedCount = 0
        def test1() { testsExecutedCount += 1 }
        def test2() { testsExecutedCount += 1 }
        def test3() { testsExecutedCount += 1 }
        def test4() {
          testsExecutedCount += 1
          myStopper.stop = true
        }
        def test5() { testsExecutedCount += 1 }
        def test6() { testsExecutedCount += 1 }
        def test7() { testsExecutedCount += 1 }
      }

      val y = new MyStoppingSuite
      y.run(None, SilentReporter, myStopper, Filter(), Map(), None, new Tracker)
      assert(y.testsExecutedCount === 4)
    }
  }
  describe("A Suite's execute method") {
    it("should throw NPE if passed null for configMap") {
      class MySuite extends Suite
      intercept[NullPointerException] {
        (new MySuite).execute(configMap = null)
      }
    }
    it("should throw IAE if a testName is passed that does not exist on the suite") {
      class MySuite extends Suite
      intercept[IllegalArgumentException] {
        (new MySuite).execute(testName = "fred")
      }
    }
  }
}

