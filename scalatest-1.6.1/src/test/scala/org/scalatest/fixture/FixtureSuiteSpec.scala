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
package fixture

import collection.immutable.TreeSet
import events.TestFailed
import events.TestSucceeded
import mock.MockitoSugar

class FixtureSuiteSpec extends org.scalatest.Spec with PrivateMethodTester with SharedHelpers {

  describe("The private testMethodTakesInformer method") {
    val testMethodTakesAFixtureAndInformer = PrivateMethod[Boolean]('testMethodTakesAFixtureAndInformer)
    val suiteObject = FixtureSuite
    it("should return true if passed a string that ends in (FixtureParam, Informer)") {
      assert(suiteObject invokePrivate testMethodTakesAFixtureAndInformer("thisDoes(FixtureParam, Informer)"))
      assert(suiteObject invokePrivate testMethodTakesAFixtureAndInformer("(FixtureParam, Informer)"))
      assert(suiteObject invokePrivate testMethodTakesAFixtureAndInformer("test(FixtureParam, Informer)"))
    }
    it("should return false if passed a string that doesn't end in (FixtureParam, Informer)") {
      assert(!(suiteObject invokePrivate testMethodTakesAFixtureAndInformer("thisDoesNot(FixtureParam)")))
      assert(!(suiteObject invokePrivate testMethodTakesAFixtureAndInformer("test(FixtureParam)")))
    }
  }


/*
  describe("A fixture.Suite without SimpleWithFixture") {

    it("should return the test names in alphabetical order from testNames") {
      val a = new FixtureSuite {
        type FixtureParam = String
        def withFixture(fun: String => Unit, config: Map[String, Any]) {}
        def testThis(fixture: String) {}
        def testThat(fixture: String) {}
      }

      expect(List("testThat(FixtureParam)", "testThis(FixtureParam)")) {
        a.testNames.iterator.toList
      }

      val b = new FixtureSuite {
        type FixtureParam = String
        def withFixture(fun: String => Unit, config: Map[String, Any]) {}
      }

      expect(List[String]()) {
        b.testNames.iterator.toList
      }

      val c = new FixtureSuite {
        type FixtureParam = String
        def withFixture(fun: String => Unit, config: Map[String, Any]) {}
        def testThat(fixture: String) {}
        def testThis(fixture: String) {}
      }

      expect(List("testThat(FixtureParam)", "testThis(FixtureParam)")) {
        c.testNames.iterator.toList
      }
    }

    it("should discover tests with and without Informer parameters") {
      val a = new FixtureSuite {
        type FixtureParam = String
        def withFixture(fun: String => Unit, config: Map[String, Any]) {}
        def testThis(fixture: String) = ()
        def testThat(fixture: String, info: Informer) = ()
      }
      assert(a.testNames === TreeSet("testThat(FixtureParam, Informer)", "testThis(FixtureParam)"))
    }

    it("should pass in the fixture to every test method") {
      val a = new FixtureSuite {
        type FixtureParam = String
        val hello = "Hello, world!"
        def withFixture(fun: String => Unit, config: Map[String, Any]) {
          test(hello)
        }
        def testThis(fixture: String) {
          assert(fixture === hello)
        }
        def testThat(fixture: String, info: Informer) {
          assert(fixture === hello)
        }
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
    }

    it("can pass in the config map to every test method via the fixture") {
      val key = "greeting"
      val hello = "Hello, world!"
      val a = new FixtureSuite {
        type FixtureParam = Map[String, Any]
        def withFixture(fun: FixtureParam => Unit, config: Map[String, Any]) {
          test(config)
        }
        def testThis(fixture: FixtureParam) {
          assert(fixture(key) === hello)
        }
        def testThat(fixture: FixtureParam, info: Informer) {
          assert(fixture(key) === hello)
        }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(key -> hello), None, new Tracker())
      assert(!rep.eventsReceived.exists(_.isInstanceOf[TestFailed]))
    }
  }
*/

  describe("A FixtureSuite") {
    it("should return the test names in alphabetical order from testNames") {
      val a = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        def testThis(fixture: String) {}
        def testThat(fixture: String) {}
      }

      expect(List("testThat(FixtureParam)", "testThis(FixtureParam)")) {
        a.testNames.iterator.toList
      }

      val b = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
      }

      expect(List[String]()) {
        b.testNames.iterator.toList
      }

      val c = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        def testThat(fixture: String) {}
        def testThis(fixture: String) {}
      }

      expect(List("testThat(FixtureParam)", "testThis(FixtureParam)")) {
        c.testNames.iterator.toList
      }
    }

    it("should discover tests with and without Informer parameters") {
      val a = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        def testThis(fixture: String) = ()
        def testThat(fixture: String, info: Informer) = ()
      }
      assert(a.testNames === TreeSet("testThat(FixtureParam, Informer)", "testThis(FixtureParam)"))
    }

    it("should pass in the fixture to every test method") {
      val a = new FixtureSuite {
        type FixtureParam = String
        val hello = "Hello, world!"
        def withFixture(test: OneArgTest) {
          test(hello)
        }
        def testThis(fixture: String) {
          assert(fixture === hello)
        }
        def testThat(fixture: String, info: Informer) {
          assert(fixture === hello)
        }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(!rep.eventsReceived.exists(_.isInstanceOf[TestFailed]))
    }
    
    it("should return a correct tags map from the tags method") {

      val a = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        @Ignore
        def testThis(fixture: FixtureParam) = ()
        def testThat(fixture: FixtureParam, info: Informer) = ()
      }

      assert(a.tags === Map("testThis(FixtureParam)" -> Set("org.scalatest.Ignore")))

      val b = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        def testThis(fixture: FixtureParam) = ()
        @Ignore
        def testThat(fixture: FixtureParam, info: Informer) = ()
      }

      assert(b.tags === Map("testThat(FixtureParam, Informer)" -> Set("org.scalatest.Ignore")))

      val c = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        @Ignore
        def testThis(fixture: FixtureParam) = ()
        @Ignore
        def testThat(fixture: FixtureParam, info: Informer) = ()
      }

      assert(c.tags === Map("testThis(FixtureParam)" -> Set("org.scalatest.Ignore"), "testThat(FixtureParam, Informer)" -> Set("org.scalatest.Ignore")))

      val d = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) = ()
        @SlowAsMolasses
        @Ignore
        def testThat(fixture: FixtureParam, info: Informer) = ()
      }

      assert(d.tags === Map("testThis(FixtureParam)" -> Set("org.scalatest.SlowAsMolasses"), "testThat(FixtureParam, Informer)" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses")))

      val e = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
      }
      assert(e.tags === Map())
    }

    class TestWasCalledSuite extends FixtureSuite {
      type FixtureParam = String
      def withFixture(test: OneArgTest) { test("hi") }
      var theTestThisCalled = false
      var theTestThatCalled = false
      def testThis(s: String) { theTestThisCalled = true }
      def testThat(s: String) { theTestThatCalled = true }
    }

    it("should execute all tests when run is called with testName None") {

      val b = new TestWasCalledSuite
      b.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(b.theTestThisCalled)
      assert(b.theTestThatCalled)
    }

    it("should execute one test when run is called with a defined testName") {

      val a = new TestWasCalledSuite
      a.run(Some("testThis(FixtureParam)"), SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(a.theTestThisCalled)
      assert(!a.theTestThatCalled)
    }

    it("should report as ignored, ant not run, tests marked ignored") {

      val a = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
      }

      val repA = new TestIgnoredTrackingReporter
      a.run(None, repA, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repA.testIgnoredReceived)
      assert(a.theTestThisCalled)
      assert(a.theTestThatCalled)

      val b = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        @Ignore
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
      }

      val repB = new TestIgnoredTrackingReporter
      b.run(None, repB, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(repB.testIgnoredReceived)
      assert(repB.lastEvent.isDefined)
      assert(repB.lastEvent.get.testName endsWith "testThis(FixtureParam)")
      assert(!b.theTestThisCalled)
      assert(b.theTestThatCalled)

      val c = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        @Ignore
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
      }

      val repC = new TestIgnoredTrackingReporter
      c.run(None, repC, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(repC.testIgnoredReceived)
      assert(repC.lastEvent.isDefined)
      assert(repC.lastEvent.get.testName endsWith "testThat(FixtureParam, Informer)", repC.lastEvent.get.testName)
      assert(c.theTestThisCalled)
      assert(!c.theTestThatCalled)

      val d = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        @Ignore
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        @Ignore
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
      }

      val repD = new TestIgnoredTrackingReporter
      d.run(None, repD, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(repD.testIgnoredReceived)
      assert(repD.lastEvent.isDefined)
      assert(repD.lastEvent.get.testName endsWith "testThis(FixtureParam)") // last because run alphabetically
      assert(!d.theTestThisCalled)
      assert(!d.theTestThatCalled)
    }

    it("should run a test marked as ignored if run is invoked with that testName") {

      val e = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        @Ignore
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
      }

      val repE = new TestIgnoredTrackingReporter
      e.run(Some("testThis(FixtureParam)"), repE, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repE.testIgnoredReceived)
      assert(e.theTestThisCalled)
      assert(!e.theTestThatCalled)
    }

    it("should throw IllegalArgumentException if run is passed a testName that does not exist") {

      val suite = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
      }

      intercept[IllegalArgumentException] {
        // Here, they forgot that the name is actually testThis(FixtureParam)
        suite.run(Some("testThis"), SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      }

      intercept[IllegalArgumentException] {
        // Here, they gave a non-existent test name
        suite.run(Some("doesNotExist(FixtureParam)"), SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      }
    }

    it("should run only those tests selected by the tags to include and exclude sets") {

      // Nothing is excluded
      val a = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
      }
      val repA = new TestIgnoredTrackingReporter
      a.run(None, repA, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repA.testIgnoredReceived)
      assert(a.theTestThisCalled)
      assert(a.theTestThatCalled)

      // SlowAsMolasses is included, one test should be excluded
      val b = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
      }
      val repB = new TestIgnoredTrackingReporter
      b.run(None, repB, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set()), Map(), None, new Tracker)
      assert(!repB.testIgnoredReceived)
      assert(b.theTestThisCalled)
      assert(!b.theTestThatCalled)

      // SlowAsMolasses is included, and both tests should be included
      val c = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
      }
      val repC = new TestIgnoredTrackingReporter
      c.run(None, repB, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set()), Map(), None, new Tracker)
      assert(!repC.testIgnoredReceived)
      assert(c.theTestThisCalled)
      assert(c.theTestThatCalled)

      // SlowAsMolasses is included. both tests should be included but one ignored
      val d = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        @Ignore
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
      }
      val repD = new TestIgnoredTrackingReporter
      d.run(None, repD, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.Ignore")), Map(), None, new Tracker)
      assert(repD.testIgnoredReceived)
      assert(!d.theTestThisCalled)
      assert(d.theTestThatCalled)

      // SlowAsMolasses included, FastAsLight excluded
      val e = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @FastAsLight
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
        def testTheOther(fixture: FixtureParam, info: Informer) { theTestTheOtherCalled = true }
      }
      val repE = new TestIgnoredTrackingReporter
      e.run(None, repE, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repE.testIgnoredReceived)
      assert(!e.theTestThisCalled)
      assert(e.theTestThatCalled)
      assert(!e.theTestTheOtherCalled)

      // An Ignored test that was both included and excluded should not generate a TestIgnored event
      val f = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @Ignore
        @FastAsLight
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
        def testTheOther(fixture: FixtureParam, info: Informer) { theTestTheOtherCalled = true }
      }
      val repF = new TestIgnoredTrackingReporter
      f.run(None, repF, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repF.testIgnoredReceived)
      assert(!f.theTestThisCalled)
      assert(f.theTestThatCalled)
      assert(!f.theTestTheOtherCalled)

      // An Ignored test that was not included should not generate a TestIgnored event
      val g = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @FastAsLight
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
        @Ignore
        def testTheOther(fixture: FixtureParam, info: Informer) { theTestTheOtherCalled = true }
      }
      val repG = new TestIgnoredTrackingReporter
      g.run(None, repG, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repG.testIgnoredReceived)
      assert(!g.theTestThisCalled)
      assert(g.theTestThatCalled)
      assert(!g.theTestTheOtherCalled)

      // No tagsToInclude set, FastAsLight excluded
      val h = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @FastAsLight
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
        def testTheOther(fixture: FixtureParam, info: Informer) { theTestTheOtherCalled = true }
      }
      val repH = new TestIgnoredTrackingReporter
      h.run(None, repH, new Stopper {}, Filter(None, Set("org.scalatest.FastAsLight")), Map(), None, new Tracker)
      assert(!repH.testIgnoredReceived)
      assert(!h.theTestThisCalled)
      assert(h.theTestThatCalled)
      assert(h.theTestTheOtherCalled)

      // No tagsToInclude set, SlowAsMolasses excluded
      val i = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @FastAsLight
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        @SlowAsMolasses
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
        def testTheOther(fixture: FixtureParam, info: Informer) { theTestTheOtherCalled = true }
      }
      val repI = new TestIgnoredTrackingReporter
      i.run(None, repI, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses")), Map(), None, new Tracker)
      assert(!repI.testIgnoredReceived)
      assert(!i.theTestThisCalled)
      assert(!i.theTestThatCalled)
      assert(i.theTestTheOtherCalled)

      // No tagsToInclude set, SlowAsMolasses excluded, TestIgnored should not be received on excluded ones
      val j = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @Ignore
        @FastAsLight
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        @Ignore
        @SlowAsMolasses
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
        def testTheOther(fixture: FixtureParam, info: Informer) { theTestTheOtherCalled = true }
      }
      val repJ = new TestIgnoredTrackingReporter
      j.run(None, repJ, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses")), Map(), None, new Tracker)
      assert(!repI.testIgnoredReceived)
      assert(!j.theTestThisCalled)
      assert(!j.theTestThatCalled)
      assert(j.theTestTheOtherCalled)

      // Same as previous, except Ignore specifically mentioned in excludes set
      val k = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        @Ignore
        @FastAsLight
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) { theTestThisCalled = true }
        @Ignore
        @SlowAsMolasses
        def testThat(fixture: FixtureParam, info: Informer) { theTestThatCalled = true }
        @Ignore
        def testTheOther(fixture: FixtureParam, info: Informer) { theTestTheOtherCalled = true }
      }
      val repK = new TestIgnoredTrackingReporter
      k.run(None, repK, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses", "org.scalatest.Ignore")), Map(), None, new Tracker)
      assert(repK.testIgnoredReceived)
      assert(!k.theTestThisCalled)
      assert(!k.theTestThatCalled)
      assert(!k.theTestTheOtherCalled)
    }

    it("should return the correct test count from its expectedTestCount method") {

      val a = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        def testThis(fixture: FixtureParam) = ()
        def testThat(fixture: FixtureParam, info: Informer) = ()
      }
      assert(a.expectedTestCount(Filter()) === 2)

      val b = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        @Ignore
        def testThis(fixture: FixtureParam) = ()
        def testThat(fixture: FixtureParam, info: Informer) = ()
      }
      assert(b.expectedTestCount(Filter()) === 1)

      val c = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        @FastAsLight
        def testThis(fixture: FixtureParam) = ()
        def testThat(fixture: FixtureParam, info: Informer) = ()
      }
      assert(c.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(c.expectedTestCount(Filter(None, Set("org.scalatest.FastAsLight"))) === 1)

      val d = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        @FastAsLight
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) = ()
        @SlowAsMolasses
        def testThat(fixture: FixtureParam, info: Informer) = ()
        def testTheOtherThing(info: Informer) = ()
      }
      assert(d.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(d.expectedTestCount(Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight"))) === 1)
      assert(d.expectedTestCount(Filter(None, Set("org.scalatest.SlowAsMolasses"))) === 1)
      assert(d.expectedTestCount(Filter()) === 3)

      val e = new FixtureSuite {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        @FastAsLight
        @SlowAsMolasses
        def testThis(fixture: FixtureParam) = ()
        @SlowAsMolasses
        def testThat(fixture: FixtureParam, info: Informer) = ()
        @Ignore
        def testTheOtherThing(info: Informer) = ()
      }
      assert(e.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(e.expectedTestCount(Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight"))) === 1)
      assert(e.expectedTestCount(Filter(None, Set("org.scalatest.SlowAsMolasses"))) === 0)
      assert(e.expectedTestCount(Filter()) === 2)

      val f = Suites(a, b, c, d, e)
      assert(f.expectedTestCount(Filter()) === 10)
    }

    it("should generate a TestPending message when the test body is (pending)") {
      val a = new FixtureSuite {
        type FixtureParam = String
        val hello = "Hello, world!"
        def withFixture(test: OneArgTest) {
          test(hello)
        }

        def testDoThis(fixture: FixtureParam) { pending }

        def testDoThat(fixture: FixtureParam) {
          assert(fixture === hello)
        }

        def testDoSomethingElse(fixture: FixtureParam) {
          assert(fixture === hello)
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
      val a = new FixtureSuite {
        type FixtureParam = String
        val hello = "Hello, world!"
        def withFixture(test: OneArgTest) {
          test(hello)
        }
        def testThrowsAssertionError(s: String) { throw new AssertionError }
        def testThrowsPlainOldError(s: String) { throw new Error }
        def testThrowsThrowable(s: String) { throw new Throwable }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      val tf = rep.testFailedEventsReceived
      assert(tf.size === 3)
    }
    it("should propagate out Errors that are direct subtypes of Error in JDK 1.5, other than " +
            "AssertionError, causing Suites and Runs to abort.") {
      val a = new FixtureSuite {
        type FixtureParam = String
        val hello = "Hello, world!"
        def withFixture(test: OneArgTest) {
          test(hello)
        }
        def testThrowsAssertionError(s: String) { throw new OutOfMemoryError }
      }
      intercept[OutOfMemoryError] {
        a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      }
    }
    it("should allow both tests that take fixtures and tests that don't") {
      val a = new FixtureSuite {

        type FixtureParam = String
        def withFixture(test: OneArgTest) {
          test("Hello, world!")
        }

        var takesNoArgsInvoked = false
        def testTakesNoArgs() { takesNoArgsInvoked = true }

        var takesAnInformerInvoked = false
        def testTakesAnInformer(info: Informer) { takesAnInformerInvoked = true }

        var takesAFixtureInvoked = false
        def testTakesAFixture(s: String) { takesAFixtureInvoked = true }

        var takesAFixtureAndInformerInvoked = false
        def testTakesAFixtureAndInformer(s: String, info: Informer) { takesAFixtureAndInformerInvoked = true }
      }

      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.testNames.size === 4, a.testNames)
      assert(a.takesNoArgsInvoked)
      assert(a.takesAnInformerInvoked)
      assert(a.takesAFixtureInvoked)
      assert(a.takesAFixtureAndInformerInvoked)
    }
    it("should allow primitive type fixtures") {
      val a = new FixtureSuite {

        type FixtureParam = Int
        def withFixture(test: OneArgTest) {
          test(99)
        }

        var takesNoArgsInvoked = false
        def testTakesNoArgs() { takesNoArgsInvoked = true }

        var takesAnInformerInvoked = false
        def testTakesAnInformer(info: Informer) { takesAnInformerInvoked = true }

        var takesAFixtureInvoked = false
        def testTakesAFixture(i: Int) { takesAFixtureInvoked = true }

        var takesAFixtureAndInformerInvoked = false
        def testTakesAFixtureAndInformer(i: Int, info: Informer) { takesAFixtureAndInformerInvoked = true }
      }

      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.testNames.size === 4, a.testNames)
      assert(a.takesNoArgsInvoked)
      assert(a.takesAnInformerInvoked)
      assert(a.takesAFixtureInvoked)
      assert(a.takesAFixtureAndInformerInvoked)
    }
    it("should pass a NoArgTest to withFixture for test methods that take no arguments") {
      class MySuite extends FixtureSuite {
        type FixtureParam = String
        var aNoArgTestWasPassed = false
        var aOneArgTestWasPassed = false
        override def withFixture(test: NoArgTest) {
          aNoArgTestWasPassed = true
        }
        def withFixture(test: OneArgTest) {
          aOneArgTestWasPassed = true
        }
        def testSomething() {
          assert(1 + 1 === 2)
        }
      }

      val s = new MySuite
      s.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(s.aNoArgTestWasPassed)
      assert(!s.aOneArgTestWasPassed)
    }
    it("should pass a NoArgTest to withFixture for test methods that take only an Informer") {
      class MySuite extends FixtureSuite {
        type FixtureParam = String
        var aNoArgTestWasPassed = false
        var aOneArgTestWasPassed = false
        override def withFixture(test: NoArgTest) {
          aNoArgTestWasPassed = true
        }
        def withFixture(test: OneArgTest) {
          aOneArgTestWasPassed = true
        }
        def testSomething(info: Informer) {
          assert(1 + 1 === 2)
        }
      }

      val s = new MySuite
      s.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(s.aNoArgTestWasPassed)
      assert(!s.aOneArgTestWasPassed)
    }
    it("should not pass a NoArgTest to withFixture for test methods that take a Fixture and an Informer") {
      class MySuite extends FixtureSuite {
        type FixtureParam = String
        var aNoArgTestWasPassed = false
        var aOneArgTestWasPassed = false
        override def withFixture(test: NoArgTest) {
          aNoArgTestWasPassed = true
        }
        def withFixture(test: OneArgTest) {
          aOneArgTestWasPassed = true
        }
        def testSomething(fixture: FixtureParam, info: Informer) {
          assert(1 + 1 === 2)
        }
      }

      val s = new MySuite
      s.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(!s.aNoArgTestWasPassed)
      assert(s.aOneArgTestWasPassed)
    }
    it("should not pass a NoArgTest to withFixture for test methods that take a Fixture") {
      class MySuite extends FixtureSuite {
        type FixtureParam = String
        var aNoArgTestWasPassed = false
        def withFixture(test: OneArgTest) {
          // Shouldn't be called
        }
        override def withFixture(test: NoArgTest) {
          aNoArgTestWasPassed = true
        }
        def testSomething(fixture: FixtureParam) {
          assert(1 + 1 === 2)
        }
      }

      val s = new MySuite
      s.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(!s.aNoArgTestWasPassed)
    }
    it("should pass a NoArgTest that invokes the no-arg test when the " +
            "NoArgTest's no-arg apply method is invoked") {

      class MySuite extends FixtureSuite {
        type FixtureParam = String
        var theNoArgTestWasInvoked = false
        def withFixture(test: OneArgTest) {
          // Shouldn't be called, but just in case don't invoke a OneArgTest
        }
        def testSomething() {
          theNoArgTestWasInvoked = true
        }
      }

      val s = new MySuite
      s.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(s.theNoArgTestWasInvoked)
    }
    it("should pass a NoArgTest that invokes a test that takse an Informer when the " +
            "NoArgTest's no-arg apply method is invoked") {

      class MySuite extends FixtureSuite {
        type FixtureParam = String
        var theNoArgTestWasInvoked = false
        def withFixture(test: OneArgTest) {
          // Shouldn't be called, but just in case don't invoke a OneArgTest
        }
        def testSomething(info: Informer) {
          theNoArgTestWasInvoked = true
        }
      }

      val s = new MySuite
      s.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(s.theNoArgTestWasInvoked)
    }
    it("should pass the correct test name in the OneArgTest passed to withFixture") {
      val a = new FixtureSuite {
        type FixtureParam = String
        var correctTestNameWasPassed = false
        def withFixture(test: OneArgTest) {
          correctTestNameWasPassed = test.name == "testSomething(FixtureParam, Informer)"
          test("hi")
        }
        def testSomething(fixture: FixtureParam, info: Informer) {}
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.correctTestNameWasPassed)
    }
    it("should pass the correct config map in the OneArgTest passed to withFixture") {
      val a = new FixtureSuite {
        type FixtureParam = String
        var correctConfigMapWasPassed = false
        def withFixture(test: OneArgTest) {
          correctConfigMapWasPassed = (test.configMap == Map("hi" -> 7))
          test("hi")
        }
        def testSomething(fixture: FixtureParam, info: Informer) {}
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map("hi" -> 7), None, new Tracker())
      assert(a.correctConfigMapWasPassed)
    }
  }
  describe("A OneArgTest") {
    it("should provide an easy way to invoke a NoArgTest") {

      var noArgWithFixtureWasCalled = false

      val a = new FixtureSuite {

        type FixtureParam = String

        override def withFixture(test: NoArgTest) {
          noArgWithFixtureWasCalled = true
          test()
        }

        def withFixture(test: OneArgTest) {
          withFixture(test.toNoArgTest("hi"))
        }

        def testSomething(fixture: String) { assert(fixture === "hi") }
      }

      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(noArgWithFixtureWasCalled)
      assert(rep.eventsReceived.exists(_.isInstanceOf[TestSucceeded]))
    }
  }
}

