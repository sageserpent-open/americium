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
package org.scalatest.fixture

import org.scalatest._
import org.scalatest.events.{TestStarting, TestFailed}

object SlowTest extends Tag("SlowTest")

class FixtureFlatSpecSpec extends org.scalatest.Spec with PrivateMethodTester with SharedHelpers {

  describe("A FixtureFlatSpec ") {
    it("A fixture.Spec should return the test names in order of registration from testNames") {
      val a = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "Something" should "do that" in { fixture =>
        }
        it should "do this" in { fixture =>
        }
      }

      expect(List("Something should do that", "Something should do this")) {
        a.testNames.iterator.toList
      }

      val b = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
      }

      expect(List[String]()) {
        b.testNames.iterator.toList
      }

      val c = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "Something" should "do this" in { fixture =>
        }
        it should "do that" in { fixture =>
        }
      }

      expect(List("Something should do this", "Something should do that")) {
        c.testNames.iterator.toList
      }
    }

    it("should throw DuplicateTestNameException if a duplicate test name registration is attempted") {

      intercept[DuplicateTestNameException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          it should "test this" in { fixture => }
          it should "test this" in { fixture => }
        }
      }
      intercept[DuplicateTestNameException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          it should "test this" in { fixture => }
          it should "test this" taggedAs(SlowTest) ignore { fixture => }
        }
      }
      intercept[DuplicateTestNameException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          it should "test this" in { fixture => }
          it should "test this" taggedAs(Tag("SlowTest")) ignore { fixture => }
        }
      }
      intercept[DuplicateTestNameException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          ignore should "test this" in { fixture => }
          it should "test this" ignore { fixture => }
        }
      }
      intercept[DuplicateTestNameException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          ignore should "test this" in { fixture => }
          it should "test this" in { fixture => }
        }
      }
    }

    it("should pass in the fixture to every test method") {
      val a = new FixtureFlatSpec {
        type FixtureParam = String
        val hello = "Hello, world!"
        def withFixture(test: OneArgTest) {
          test(hello)
        }
        "Something" should "do this" in { fixture =>
          assert(fixture === hello)
        }
        it should "do that" in { fixture =>
          assert(fixture === hello)
        }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(!rep.eventsReceived.exists(_.isInstanceOf[TestFailed]))
    }

    it("should run tests registered via the 'it can behave like' syntax") {
      trait SharedFlatSpecTests { this: FixtureFlatSpec =>
        def nonEmptyStack(s: String)(i: Int) {
          it can "I am shared" in { fixture => }
        }
      }
      class MyConfigFlatSpec extends FixtureFlatSpec with SharedFlatSpecTests {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {
          test("hi")
        }
        it can behave like nonEmptyStack("hi")(1)
      }
      val suite = new MyConfigFlatSpec
      val reporter = new EventRecordingReporter
      suite.run(None, reporter, new Stopper {}, Filter(), Map(), None, new Tracker)

      val indexedList = reporter.eventsReceived

      val testStartingOption = indexedList.find(_.isInstanceOf[TestStarting])
      assert(testStartingOption.isDefined)
      assert(testStartingOption.get.asInstanceOf[TestStarting].testName === "can I am shared")
    }
    it("should throw NullPointerException if a null test tag is provided") {
      // it
      intercept[NullPointerException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          it should "hi" taggedAs(null) in { fixture => }
        }
      }
      val caught = intercept[NullPointerException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          it should "hi" taggedAs(mytags.SlowAsMolasses, null) in { fixture => }
        }
      }
      assert(caught.getMessage === "a test tag was null")
      intercept[NullPointerException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          it should "hi" taggedAs(mytags.SlowAsMolasses, null, mytags.WeakAsAKitten) in { fixture => }
        }
      }
      // ignore
      intercept[NullPointerException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          ignore should "hi" taggedAs(null) in { fixture => }
        }
      }
      val caught2 = intercept[NullPointerException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          ignore should "hi" taggedAs(mytags.SlowAsMolasses, null) in { fixture => }
        }
      }
      assert(caught2.getMessage === "a test tag was null")
      intercept[NullPointerException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          ignore should "hi" taggedAs(mytags.SlowAsMolasses, null, mytags.WeakAsAKitten) in { fixture => }
        }
      }
      intercept[NullPointerException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          it should "hi" taggedAs(null) ignore { fixture => }
        }
      }
      val caught3 = intercept[NullPointerException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          it should "hi" taggedAs(mytags.SlowAsMolasses, null) ignore { fixture => }
        }
      }
      assert(caught3.getMessage === "a test tag was null")
      intercept[NullPointerException] {
        new FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) {}
          it should "hi" taggedAs(mytags.SlowAsMolasses, null, mytags.WeakAsAKitten) ignore { fixture => }
        }
      }
    }
    it("should return a correct tags map from the tags method, when using regular (not shorthand)" +
            " notation and ignore replacing it") {

      val a = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        ignore should "test this" in { fixture => }
        it should "test that" in { fixture => }
      }
      expect(Map("should test this" -> Set("org.scalatest.Ignore"))) {
        a.tags
      }

      val b = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it can "test this" in { fixture => }
        ignore can "test that" in { fixture => }
      }
      expect(Map("can test that" -> Set("org.scalatest.Ignore"))) {
        b.tags
      }

      val c = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        ignore must "test this" in { fixture => }
        ignore must "test that" in { fixture => }
      }
      expect(Map("must test this" -> Set("org.scalatest.Ignore"), "must test that" -> Set("org.scalatest.Ignore"))) {
        c.tags
      }

      val d = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it must "test this" taggedAs(mytags.SlowAsMolasses) in { fixture => }
        ignore must "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => }
      }
      expect(Map("must test this" -> Set("org.scalatest.SlowAsMolasses"), "must test that" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses"))) {
        d.tags
      }

      val e = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it must "test this" in { fixture => }
        it must "test that" in { fixture => }
      }
      expect(Map()) {
        e.tags
      }

      val f = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it can "test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) in { fixture => }
        it can "test that" taggedAs(mytags.SlowAsMolasses) in  { fixture => }
      }
      expect(Map("can test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "can test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        f.tags
      }

      val g = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it should "test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) in { fixture => }
        it should "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => }
      }
      expect(Map("should test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "should test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        g.tags
      }
    }
    it("should return a correct tags map from the tags method, when using regular (not shorthand)" +
            " notation and ignore replacing in") {

      val a = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it should "test this" ignore { fixture => }
        it should "test that" in { fixture => }
      }
      expect(Map("should test this" -> Set("org.scalatest.Ignore"))) {
        a.tags
      }

      val b = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it can "test this" in { fixture => }
        it can "test that" ignore { fixture => }
      }
      expect(Map("can test that" -> Set("org.scalatest.Ignore"))) {
        b.tags
      }

      val c = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it must "test this" ignore { fixture => }
        it must "test that" ignore { fixture => }
      }
      expect(Map("must test this" -> Set("org.scalatest.Ignore"), "must test that" -> Set("org.scalatest.Ignore"))) {
        c.tags
      }

      val d = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it must "test this" taggedAs(mytags.SlowAsMolasses) in { fixture => }
        it must "test that" taggedAs(mytags.SlowAsMolasses) ignore { fixture => }
      }
      expect(Map("must test this" -> Set("org.scalatest.SlowAsMolasses"), "must test that" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses"))) {
        d.tags
      }
    }

    it("should return a correct tags map from the tags method, when using shorthand notation") {

      val a = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" should "test this" ignore { fixture => }
        "A Stack" should "test that" in { fixture => }
      }
      expect(Map("A Stack should test this" -> Set("org.scalatest.Ignore"))) {
        a.tags
      }

      val b = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" can "test this" in { fixture => }
        "A Stack" can "test that" ignore { fixture => }
      }
      expect(Map("A Stack can test that" -> Set("org.scalatest.Ignore"))) {
        b.tags
      }

      val c = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" must "test this" ignore { fixture => }
        "A Stack" must "test that" ignore { fixture => }
      }
      expect(Map("A Stack must test this" -> Set("org.scalatest.Ignore"), "A Stack must test that" -> Set("org.scalatest.Ignore"))) {
        c.tags
      }

      val d = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" must "test this" taggedAs(mytags.SlowAsMolasses) in { fixture => }
        "A Stack" must "test that" taggedAs(mytags.SlowAsMolasses) ignore { fixture => }
      }
      expect(Map("A Stack must test this" -> Set("org.scalatest.SlowAsMolasses"), "A Stack must test that" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses"))) {
        d.tags
      }

      val e = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" must "test this" in { fixture => }
        "A Stack" must "test that" in { fixture => }
      }
      expect(Map()) {
        e.tags
      }

      val f = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" can "test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) in { fixture => }
        "A Stack" can "test that" taggedAs(mytags.SlowAsMolasses) in  { fixture => }
      }
      expect(Map("A Stack can test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "A Stack can test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        f.tags
      }

      val g = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" should "test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) in { fixture => }
        "A Stack" should "test that" taggedAs(mytags.SlowAsMolasses) in  { fixture => }
      }
      expect(Map("A Stack should test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "A Stack should test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        g.tags
      }
    }

    it("should return a correct tags map from the tags method using is (pending), when using regular (not shorthand)" +
            " notation and ignore replacing it") {

      val a = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        ignore should "test this" is (pending)
        it should "test that" is (pending)
      }
      expect(Map("should test this" -> Set("org.scalatest.Ignore"))) {
        a.tags
      }

      val b = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it can "test this" is (pending)
        ignore can "test that" is (pending)
      }
      expect(Map("can test that" -> Set("org.scalatest.Ignore"))) {
        b.tags
      }

      val c = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        ignore must "test this" is (pending)
        ignore must "test that" is (pending)
      }
      expect(Map("must test this" -> Set("org.scalatest.Ignore"), "must test that" -> Set("org.scalatest.Ignore"))) {
        c.tags
      }

      val d = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it must "test this" taggedAs(mytags.SlowAsMolasses) is (pending)
        ignore must "test that" taggedAs(mytags.SlowAsMolasses) is (pending)
      }
      expect(Map("must test this" -> Set("org.scalatest.SlowAsMolasses"), "must test that" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses"))) {
        d.tags
      }

      val e = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it must "test this" is (pending)
        it must "test that" is (pending)
      }
      expect(Map()) {
        e.tags
      }

      val f = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it can "test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) is (pending)
        it can "test that" taggedAs(mytags.SlowAsMolasses) in  { fixture => }
      }
      expect(Map("can test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "can test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        f.tags
      }

      val g = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it should "test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) is (pending)
        it should "test that" taggedAs(mytags.SlowAsMolasses) is (pending)
      }
      expect(Map("should test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "should test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        g.tags
      }
    }
    it("should return a correct tags map from the tags method using is (pending), when using regular (not shorthand)" +
            " notation and ignore replacing in") {

      val a = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it should "test this" ignore { fixture => }
        it should "test that" is (pending)
      }
      expect(Map("should test this" -> Set("org.scalatest.Ignore"))) {
        a.tags
      }

      val b = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it can "test this" is (pending)
        it can "test that" ignore { fixture => }
      }
      expect(Map("can test that" -> Set("org.scalatest.Ignore"))) {
        b.tags
      }

      val c = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it must "test this" ignore { fixture => }
        it must "test that" ignore { fixture => }
      }
      expect(Map("must test this" -> Set("org.scalatest.Ignore"), "must test that" -> Set("org.scalatest.Ignore"))) {
        c.tags
      }

      val d = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        it must "test this" taggedAs(mytags.SlowAsMolasses) is (pending)
        it must "test that" taggedAs(mytags.SlowAsMolasses) ignore { fixture => }
      }
      expect(Map("must test this" -> Set("org.scalatest.SlowAsMolasses"), "must test that" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses"))) {
        d.tags
      }
    }

    it("should return a correct tags map from the tags method using is (pending), when using shorthand notation") {

      val a = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" should "test this" ignore { fixture => }
        "A Stack" should "test that" is (pending)
      }
      expect(Map("A Stack should test this" -> Set("org.scalatest.Ignore"))) {
        a.tags
      }

      val b = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" can "test this" is (pending)
        "A Stack" can "test that" ignore { fixture => }
      }
      expect(Map("A Stack can test that" -> Set("org.scalatest.Ignore"))) {
        b.tags
      }

      val c = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" must "test this" ignore { fixture => }
        "A Stack" must "test that" ignore { fixture => }
      }
      expect(Map("A Stack must test this" -> Set("org.scalatest.Ignore"), "A Stack must test that" -> Set("org.scalatest.Ignore"))) {
        c.tags
      }

      val d = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" must "test this" taggedAs(mytags.SlowAsMolasses) is (pending)
        "A Stack" must "test that" taggedAs(mytags.SlowAsMolasses) ignore { fixture => }
      }
      expect(Map("A Stack must test this" -> Set("org.scalatest.SlowAsMolasses"), "A Stack must test that" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses"))) {
        d.tags
      }

      val e = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" must "test this" is (pending)
        "A Stack" must "test that" is (pending)
      }
      expect(Map()) {
        e.tags
      }

      val f = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" can "test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) is (pending)
        "A Stack" can "test that" taggedAs(mytags.SlowAsMolasses) in  { fixture => }
      }
      expect(Map("A Stack can test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "A Stack can test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        f.tags
      }

      val g = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) {}
        "A Stack" should "test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) is (pending)
        "A Stack" should "test that" taggedAs(mytags.SlowAsMolasses) in  { fixture => }
      }
      expect(Map("A Stack should test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "A Stack should test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        g.tags
      }
    }

    class TestWasCalledSuite extends FixtureFlatSpec {
      type FixtureParam = String
      def withFixture(test: OneArgTest) { test("hi") }
      var theTestThisCalled = false
      var theTestThatCalled = false
      it should "run this" in { fixture => theTestThisCalled = true }
      it should "run that, maybe" in { fixture => theTestThatCalled = true }
    }

    it("should execute all tests when run is called with testName None") {

      val b = new TestWasCalledSuite
      b.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(b.theTestThisCalled)
      assert(b.theTestThatCalled)
    }

    it("should execute one test when run is called with a defined testName") {

      val a = new TestWasCalledSuite
      a.run(Some("should run this"), SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(a.theTestThisCalled)
      assert(!a.theTestThatCalled)
    }

    it("should report as ignored, and not run, tests marked ignored") {

      val a = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        it can "test this" in { fixture => theTestThisCalled = true }
        it can "test that" in { fixture => theTestThatCalled = true }
      }

      val repA = new TestIgnoredTrackingReporter
      a.run(None, repA, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repA.testIgnoredReceived)
      assert(a.theTestThisCalled)
      assert(a.theTestThatCalled)

      val b = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        ignore must "test this" in { fixture => theTestThisCalled = true }
        it must "test that" in { fixture => theTestThatCalled = true }
      }

      val repB = new TestIgnoredTrackingReporter
      b.run(None, repB, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(repB.testIgnoredReceived)
      assert(repB.lastEvent.isDefined)
      assert(repB.lastEvent.get.testName endsWith "test this")
      assert(!b.theTestThisCalled)
      assert(b.theTestThatCalled)

      val c = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        it can "test this" in { fixture => theTestThisCalled = true }
        ignore can "test that" in { fixture => theTestThatCalled = true }
      }

      val repC = new TestIgnoredTrackingReporter
      c.run(None, repC, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(repC.testIgnoredReceived)
      assert(repC.lastEvent.isDefined)
      assert(repC.lastEvent.get.testName endsWith "test that", repC.lastEvent.get.testName)
      assert(c.theTestThisCalled)
      assert(!c.theTestThatCalled)

      // The order I want is order of appearance in the file.
      // Will try and implement that tomorrow. Subtypes will be able to change the order.
      val d = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        ignore should "test this" in { fixture => theTestThisCalled = true }
        ignore should "test that" in { fixture => theTestThatCalled = true }
      }

      val repD = new TestIgnoredTrackingReporter
      d.run(None, repD, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(repD.testIgnoredReceived)
      assert(repD.lastEvent.isDefined)
      assert(repD.lastEvent.get.testName endsWith "test that") // last because should be in order of appearance
      assert(!d.theTestThisCalled)
      assert(!d.theTestThatCalled)
    }

    it("should run a test marked as ignored if run is invoked with that testName") {
      // If I provide a specific testName to run, then it should ignore an Ignore on that test
      // method and actually invoke it.
      val e = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        ignore must "test this" in { fixture => theTestThisCalled = true }
        it must "test that" in { fixture => theTestThatCalled = true }
      }

      val repE = new TestIgnoredTrackingReporter
      e.run(Some("must test this"), repE, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repE.testIgnoredReceived)
      assert(e.theTestThisCalled)
      assert(!e.theTestThatCalled)
    }

    it("should run only those tests selected by the tags to include and exclude sets") {

      // Nothing is excluded
      val a = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        it should "test this" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThisCalled = true }
        it should "test that" in { fixture => theTestThatCalled = true }
      }
      val repA = new TestIgnoredTrackingReporter
      a.run(None, repA, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repA.testIgnoredReceived)
      assert(a.theTestThisCalled)
      assert(a.theTestThatCalled)

      // SlowAsMolasses is included, one test should be excluded
      val b = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        it should "test this" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThisCalled = true }
        it should "test that" in { fixture => theTestThatCalled = true }
      }
      val repB = new TestIgnoredTrackingReporter
      b.run(None, repB, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set()), Map(), None, new Tracker)
      assert(!repB.testIgnoredReceived)
      assert(b.theTestThisCalled)
      assert(!b.theTestThatCalled)

      // SlowAsMolasses is included, and both tests should be included
      val c = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        it should "test this" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThisCalled = true }
        it should "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThatCalled = true }
      }
      val repC = new TestIgnoredTrackingReporter
      c.run(None, repB, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set()), Map(), None, new Tracker)
      assert(!repC.testIgnoredReceived)
      assert(c.theTestThisCalled)
      assert(c.theTestThatCalled)

      // SlowAsMolasses is included. both tests should be included but one ignored
      val d = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        ignore should "test this" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThisCalled = true }
        it should "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThatCalled = true }
      }
      val repD = new TestIgnoredTrackingReporter
      d.run(None, repD, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.Ignore")), Map(), None, new Tracker)
      assert(repD.testIgnoredReceived)
      assert(!d.theTestThisCalled)
      assert(d.theTestThatCalled)

      // SlowAsMolasses included, FastAsLight excluded
      val e = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        it should "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) in { fixture => theTestThisCalled = true }
        it should "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThatCalled = true }
        it should "test the other" in { fixture => theTestTheOtherCalled = true }
      }
      val repE = new TestIgnoredTrackingReporter
      e.run(None, repE, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repE.testIgnoredReceived)
      assert(!e.theTestThisCalled)
      assert(e.theTestThatCalled)
      assert(!e.theTestTheOtherCalled)

      // An Ignored test that was both included and excluded should not generate a TestIgnored event
      val f = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        ignore should "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) in { fixture => theTestThisCalled = true }
        it should "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThatCalled = true }
        it should "test the other" in { fixture => theTestTheOtherCalled = true }
      }
      val repF = new TestIgnoredTrackingReporter
      f.run(None, repF, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repF.testIgnoredReceived)
      assert(!f.theTestThisCalled)
      assert(f.theTestThatCalled)
      assert(!f.theTestTheOtherCalled)

      // An Ignored test that was not included should not generate a TestIgnored event
      val g = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        it should "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) in { fixture => theTestThisCalled = true }
        it should "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThatCalled = true }
        ignore should "test the other" in { fixture => theTestTheOtherCalled = true }
      }
      val repG = new TestIgnoredTrackingReporter
      g.run(None, repG, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repG.testIgnoredReceived)
      assert(!g.theTestThisCalled)
      assert(g.theTestThatCalled)
      assert(!g.theTestTheOtherCalled)

      // No tagsToInclude set, FastAsLight excluded
      val h = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        it should "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) in { fixture => theTestThisCalled = true }
        it should "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThatCalled = true }
        it should "test the other" in { fixture => theTestTheOtherCalled = true }
      }
      val repH = new TestIgnoredTrackingReporter
      h.run(None, repH, new Stopper {}, Filter(None, Set("org.scalatest.FastAsLight")), Map(), None, new Tracker)
      assert(!repH.testIgnoredReceived)
      assert(!h.theTestThisCalled)
      assert(h.theTestThatCalled)
      assert(h.theTestTheOtherCalled)

      // No tagsToInclude set, SlowAsMolasses excluded
      val i = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        it should "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) in { fixture => theTestThisCalled = true }
        it should "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThatCalled = true }
        it should "test the other" in { fixture => theTestTheOtherCalled = true }
      }
      val repI = new TestIgnoredTrackingReporter
      i.run(None, repI, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses")), Map(), None, new Tracker)
      assert(!repI.testIgnoredReceived)
      assert(!i.theTestThisCalled)
      assert(!i.theTestThatCalled)
      assert(i.theTestTheOtherCalled)

      // No tagsToInclude set, SlowAsMolasses excluded, TestIgnored should not be received on excluded ones
      val j = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        ignore should "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) in { fixture => theTestThisCalled = true }
        ignore should "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThatCalled = true }
        it should "test the other" in { fixture => theTestTheOtherCalled = true }
      }
      val repJ = new TestIgnoredTrackingReporter
      j.run(None, repJ, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses")), Map(), None, new Tracker)
      assert(!repI.testIgnoredReceived)
      assert(!j.theTestThisCalled)
      assert(!j.theTestThatCalled)
      assert(j.theTestTheOtherCalled)

      // Same as previous, except Ignore specifically mentioned in excludes set
      val k = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        ignore should "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) in { fixture => theTestThisCalled = true }
        ignore should "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => theTestThatCalled = true }
        ignore should "test the other" in { fixture => theTestTheOtherCalled = true }
      }
      val repK = new TestIgnoredTrackingReporter
      k.run(None, repK, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses", "org.scalatest.Ignore")), Map(), None, new Tracker)
      assert(repK.testIgnoredReceived)
      assert(!k.theTestThisCalled)
      assert(!k.theTestThatCalled)
      assert(!k.theTestTheOtherCalled)
    }

    it("should return the correct test count from its expectedTestCount method") {

      val a = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        it should "test this" in { fixture => }
        it should "test that" in { fixture => }
      }
      assert(a.expectedTestCount(Filter()) === 2)

      val b = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        ignore should "test this" in { fixture => }
        it should "test that" in { fixture => }
      }
      assert(b.expectedTestCount(Filter()) === 1)

      val c = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        it should "test this" taggedAs(mytags.FastAsLight) in { fixture => }
        it should "test that" in { fixture => }
      }
      assert(c.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(c.expectedTestCount(Filter(None, Set("org.scalatest.FastAsLight"))) === 1)

      val d = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        it should "test this" taggedAs(mytags.FastAsLight, mytags.SlowAsMolasses) in { fixture => }
        it should "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => }
        it should "test the other thing" in { fixture => }
      }
      assert(d.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(d.expectedTestCount(Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight"))) === 1)
      assert(d.expectedTestCount(Filter(None, Set("org.scalatest.SlowAsMolasses"))) === 1)
      assert(d.expectedTestCount(Filter()) === 3)

      val e = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        it should "test this" taggedAs(mytags.FastAsLight, mytags.SlowAsMolasses) in { fixture => }
        it should "test that" taggedAs(mytags.SlowAsMolasses) in { fixture => }
        ignore should "test the other thing" in { fixture => }
      }
      assert(e.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(e.expectedTestCount(Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight"))) === 1)
      assert(e.expectedTestCount(Filter(None, Set("org.scalatest.SlowAsMolasses"))) === 0)
      assert(e.expectedTestCount(Filter()) === 2)

      val f = new Suites(a, b, c, d, e)
      assert(f.expectedTestCount(Filter()) === 10)
    }

    it("should generate a TestPending message when the test body is (pending)") {
      val a = new FixtureFlatSpec {
        type FixtureParam = String
        val hello = "Hello, world!"
        def withFixture(test: OneArgTest) {
          test(hello)
        }

        it should "do this" is (pending)

        it should "do that" in { fixture =>
          assert(fixture === hello)
        }
        
        it should "do something else" in { fixture =>
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
      val a = new FixtureFlatSpec {
        type FixtureParam = String
        val hello = "Hello, world!"
        def withFixture(test: OneArgTest) {
          test(hello)
        }
        it should "throw AssertionError" in { s => throw new AssertionError }
        it should "throw plain old Error" in { s => throw new Error }
        it should "throw Throwable" in { s =>  throw new Throwable }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      val tf = rep.testFailedEventsReceived
      assert(tf.size === 3)
    }
    it("should propagate out Errors that are direct subtypes of Error in JDK 1.5, other than " +
            "AssertionError, causing Suites and Runs to abort.") {
      val a = new FixtureFlatSpec {
        type FixtureParam = String
        val hello = "Hello, world!"
        def withFixture(test: OneArgTest) {
          test(hello)
        }
        it should "throws AssertionError" in { s => throw new OutOfMemoryError }
      }
      intercept[OutOfMemoryError] {
        a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      }
    }
    it("should send InfoProvided events with aboutAPendingTest set to true for info " +
            "calls made from a test that is pending") {
      val a = new FixtureFlatSpec with GivenWhenThen {
        type FixtureParam = String
        val hello = "Hello, world!"
        def withFixture(test: OneArgTest) {
          test(hello)
        }
        it should "do something else" in { s =>
          given("two integers")
          when("one is subracted from the other")
          then("the result is the difference between the two numbers")
          pending
        }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      val ip = rep.infoProvidedEventsReceived
      assert(ip.size === 3)
      for (event <- ip) {
        assert(event.aboutAPendingTest.isDefined && event.aboutAPendingTest.get)
      }
    }
    it("should send InfoProvided events with aboutAPendingTest set to false for info " +
            "calls made from a test that is not pending") {
      val a = new FixtureFlatSpec with GivenWhenThen {
        type FixtureParam = String
        val hello = "Hello, world!"
        def withFixture(test: OneArgTest) {
          test(hello)
        }
        it should "do something else" in { s =>
          given("two integers")
          when("one is subracted from the other")
          then("the result is the difference between the two numbers")
          assert(1 + 1 === 2)
        }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      val ip = rep.infoProvidedEventsReceived
      assert(ip.size === 3)
      for (event <- ip) {
        assert(event.aboutAPendingTest.isDefined && !event.aboutAPendingTest.get)
      }
    }
    it("should allow both tests that take fixtures and tests that don't") {
      val a = new FixtureFlatSpec {

        type FixtureParam = String
        def withFixture(test: OneArgTest) {
          test("Hello, world!")
        }

        var takesNoArgsInvoked = false
        it should "take no args" in { () => takesNoArgsInvoked = true }

        var takesAFixtureInvoked = false
        it should "take a fixture" in { s => takesAFixtureInvoked = true }
      }

      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.testNames.size === 2, a.testNames)
      assert(a.takesNoArgsInvoked)
      assert(a.takesAFixtureInvoked)
    }
    it("should work with test functions whose inferred result type is not Unit") {
      val a = new FixtureFlatSpec {

        type FixtureParam = String
        def withFixture(test: OneArgTest) {
          test("Hello, world!")
        }

        var takesNoArgsInvoked = false
        it should "take no args" in { () => takesNoArgsInvoked = true; true }

        var takesAFixtureInvoked = false
        it should "take a fixture" in { s => takesAFixtureInvoked = true; true }
      }

      assert(!a.takesNoArgsInvoked)
      assert(!a.takesAFixtureInvoked)
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.testNames.size === 2, a.testNames)
      assert(a.takesNoArgsInvoked)
      assert(a.takesAFixtureInvoked)
    }
    it("should work with ignored tests whose inferred result type is not Unit") {
      val a = new FixtureFlatSpec {
        type FixtureParam = String
        def withFixture(test: OneArgTest) { test("hi") }
        var theTestThisCalled = false
        var theTestThatCalled = false
        ignore should "test this" in { () => theTestThisCalled = true; "hi" }
        ignore should "test that" in { fixture => theTestThatCalled = true; 42 }
      }

      assert(!a.theTestThisCalled)
      assert(!a.theTestThatCalled)
      val reporter = new EventRecordingReporter
      a.run(None, reporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(reporter.testIgnoredEventsReceived.size === 2)
      assert(!a.theTestThisCalled)
      assert(!a.theTestThatCalled)
    }
    it("should pass a NoArgTest to withFixture for tests that take no fixture") {
      class MySpec extends FixtureFlatSpec {
        type FixtureParam = String
        var aNoArgTestWasPassed = false
        var aOneArgTestWasPassed = false
        override def withFixture(test: NoArgTest) {
          aNoArgTestWasPassed = true
        }
        def withFixture(test: OneArgTest) {
          aOneArgTestWasPassed = true
        }
        it should "do something" in { () =>
          assert(1 + 1 === 2)
        }
      }

      val s = new MySpec
      s.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(s.aNoArgTestWasPassed)
      assert(!s.aOneArgTestWasPassed)
    }
    it("should not pass a NoArgTest to withFixture for tests that take a Fixture") {
      class MySpec extends FixtureFlatSpec {
        type FixtureParam = String
        var aNoArgTestWasPassed = false
        var aOneArgTestWasPassed = false
        override def withFixture(test: NoArgTest) {
          aNoArgTestWasPassed = true
        }
        def withFixture(test: OneArgTest) {
          aOneArgTestWasPassed = true
        }
        it should "do something" in { fixture =>
          assert(1 + 1 === 2)
        }
      }

      val s = new MySpec
      s.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(!s.aNoArgTestWasPassed)
      assert(s.aOneArgTestWasPassed)
    }
    it("should pass a NoArgTest that invokes the no-arg test when the " +
            "NoArgTest's no-arg apply method is invoked") {

      class MySpec extends FixtureFlatSpec {
        type FixtureParam = String
        var theNoArgTestWasInvoked = false
        def withFixture(test: OneArgTest) {
          // Shouldn't be called, but just in case don't invoke a OneArgTest
        }
        it should "do something" in { () =>
          theNoArgTestWasInvoked = true
        }
      }

      val s = new MySpec
      s.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(s.theNoArgTestWasInvoked)
    }
    it("should pass the correct test name in the OneArgTest passed to withFixture") {
      val a = new FixtureFlatSpec {
        type FixtureParam = String
        var correctTestNameWasPassed = false
        def withFixture(test: OneArgTest) {
          correctTestNameWasPassed = test.name == "should do something"
          test("hi")
        }
        it should "do something" in { fixture => }
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.correctTestNameWasPassed)
    }
    it("should pass the correct config map in the OneArgTest passed to withFixture") {
      val a = new FixtureFlatSpec {
        type FixtureParam = String
        var correctConfigMapWasPassed = false
        def withFixture(test: OneArgTest) {
          correctConfigMapWasPassed = (test.configMap == Map("hi" -> 7))
          test("hi")
        }
        it should "do something" in { fixture => }
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map("hi" -> 7), None, new Tracker())
      assert(a.correctConfigMapWasPassed)
    }
    describe("(when a nesting rule has been violated)") {

      it("should, if they call a behavior-of from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) { test("hi") }
          it should "blow up" in { fixture =>
            behavior of "in the wrong place, at the wrong time"
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a behavior-of with a nested it from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) { test("hi") }
          it should "blow up" in { fixture =>
            behavior of "in the wrong place, at the wrong time"
            it should "never run" in { fixture =>
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a nested it from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) { test("hi") }
          it should "blow up" in { fixture =>
            it should "never run" in { fixture =>
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a nested it with tags from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) { test("hi") }
          it should "blow up" in { fixture =>
            it should "never run" taggedAs(mytags.SlowAsMolasses) in { fixture =>
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a behavior-of with a nested ignore from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) { test("hi") }
          it should "blow up" in { fixture =>
            behavior of "in the wrong place, at the wrong time"
            ignore should "never run" in { fixture =>
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a nested ignore from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) { test("hi") }
          it should "blow up" in { fixture =>
            ignore should "never run" in { fixture =>
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a nested ignore with tags from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends FixtureFlatSpec {
          type FixtureParam = String
          def withFixture(test: OneArgTest) { test("hi") }
          it should "blow up" in { fixture =>
            ignore should "never run" taggedAs(mytags.SlowAsMolasses) in { fixture =>
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
    }
  }
}
