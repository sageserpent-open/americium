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

// elements
import org.scalatest.events._

class WordSpecSpec extends Spec with SharedHelpers with GivenWhenThen {

  describe("A WordSpec") {

    it("should invoke withFixture from runTest") {
      val a = new WordSpec {
        var withFixtureWasInvoked = false
        var testWasInvoked = false
        override def withFixture(test: NoArgTest) {
          withFixtureWasInvoked = true
          super.withFixture(test)
        }
        "do something" in {
          testWasInvoked = true
        }
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.withFixtureWasInvoked)
      assert(a.testWasInvoked)
    }
    it("should pass the correct test name in the NoArgTest passed to withFixture") {
      val a = new WordSpec {
        var correctTestNameWasPassed = false
        override def withFixture(test: NoArgTest) {
          correctTestNameWasPassed = test.name == "do something"
          super.withFixture(test)
        }
        "do something" in {}
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.correctTestNameWasPassed)
    }
    it("should pass the correct config map in the NoArgTest passed to withFixture") {
      val a = new WordSpec {
        var correctConfigMapWasPassed = false
        override def withFixture(test: NoArgTest) {
          correctConfigMapWasPassed = (test.configMap == Map("hi" -> 7))
          super.withFixture(test)
        }
        "do something" in {}
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map("hi" -> 7), None, new Tracker())
      assert(a.correctConfigMapWasPassed)
    }

    describe("(when a nesting rule has been violated)") {

      it("should, if they call a describe from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends WordSpec {
          "should blow up" in {
            "in the wrong place, at the wrong time" should {
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a describe with a nested it from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends WordSpec {
          "should blow up" in {
            "in the wrong place, at the wrong time" should {
              "should never run" in {
                assert(1 === 1)
              }
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a nested it from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends WordSpec {
          "should blow up" in {
            "should never run" in {
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a nested it with tags from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends WordSpec {
          "should blow up" in {
            "should never run" taggedAs(mytags.SlowAsMolasses) in {
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a describe with a nested ignore from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends WordSpec {
          "should blow up" in {
            "in the wrong place, at the wrong time" should {
              "should never run" ignore {
                assert(1 === 1)
              }
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a nested ignore from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends WordSpec {
          "should blow up" in {
            "should never run" ignore {
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a nested ignore with tags from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends WordSpec {
          "should blow up" in {
            "should never run" taggedAs(mytags.SlowAsMolasses) ignore {
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
    }

    it("should return the test names in registration order from testNames") {

      val a = new WordSpec {
        "it should test this" in {}
        "it should test that" in {}
      }

      expect(List("it should test this", "it should test that")) {
        a.testNames.iterator.toList
      }

      val b = new WordSpec {}

      expect(List[String]()) {
        b.testNames.iterator.toList
      }

      val c = new WordSpec {
        "it should test that" in {}
        "it should test this" in {}
      }

      expect(List("it should test that", "it should test this")) {
        c.testNames.iterator.toList
      }

      val d = new WordSpec {
        "A Tester" should {
          "test that" in {}
          "test this" in {}
        }
      }

      expect(List("A Tester should test that", "A Tester should test this")) {
        d.testNames.iterator.toList
      }

      val e = new WordSpec {
        "A Tester" should {
          "test this" in {}
          "test that" in {}
        }
      }

      expect(List("A Tester should test this", "A Tester should test that")) {
        e.testNames.iterator.toList
      }
    }

    it("should throw DuplicateTestNameException if a duplicate test name registration is attempted") {
      
      intercept[DuplicateTestNameException] {
        new WordSpec {
          "should test this" in {}
          "should test this" in {}
        }
      }
      intercept[DuplicateTestNameException] {
        new WordSpec {
          "should test this" in {}
          "should test this" ignore {}
        }
      }
      intercept[DuplicateTestNameException] {
        new WordSpec {
          "should test this" ignore {}
          "should test this" ignore {}
        }
      }
      intercept[DuplicateTestNameException] {
        new WordSpec {
          "should test this" ignore {}
          "should test this" in {}
        }
      }
    }

    describe("(with info calls)") {
      class InfoInsideTestSpec extends WordSpec {
        val msg = "hi there, dude"
        val testName = "test name"
        testName in {
          info(msg)
        }
      }
      // In a Spec, any InfoProvided's fired during the test should be cached and sent out after the test has
      // suceeded or failed. This makes the report look nicer, because the info is tucked under the "specifier'
      // text for that test.
      it("should, when the info appears in the code of a successful test, report the info after the TestSucceeded") {
        val spec = new InfoInsideTestSpec
        val (infoProvidedIndex, testStartingIndex, testSucceededIndex) =
          getIndexesForInformerEventOrderTests(spec, spec.testName, spec.msg)
        assert(testSucceededIndex < infoProvidedIndex)
      }
      class InfoBeforeTestSpec extends WordSpec {
        val msg = "hi there, dude"
        val testName = "test name"
        info(msg)
        testName in {}
      }
      it("should, when the info appears in the body before a test, report the info before the test") {
        val spec = new InfoBeforeTestSpec
        val (infoProvidedIndex, testStartingIndex, testSucceededIndex) =
          getIndexesForInformerEventOrderTests(spec, spec.testName, spec.msg)
        assert(infoProvidedIndex < testStartingIndex)
        assert(testStartingIndex < testSucceededIndex)
      }
      it("should, when the info appears in the body after a test, report the info after the test runs") {
        val msg = "hi there, dude"
        val testName = "test name"
        class MySpec extends WordSpec {
          testName in {}
          info(msg)
        }
        val (infoProvidedIndex, testStartingIndex, testSucceededIndex) =
          getIndexesForInformerEventOrderTests(new MySpec, testName, msg)
        assert(testStartingIndex < testSucceededIndex)
        assert(testSucceededIndex < infoProvidedIndex)
      }
      it("should throw an IllegalStateException when info is called by a method invoked after the suite has been executed") {
        class MySpec extends WordSpec {
          callInfo() // This should work fine
          def callInfo() {
            info("howdy")
          }
          "howdy also" in {
            callInfo() // This should work fine
          }
        }
        val spec = new MySpec
        val myRep = new EventRecordingReporter
        spec.run(None, myRep, new Stopper {}, Filter(), Map(), None, new Tracker)
        intercept[IllegalStateException] {
          spec.callInfo()
        }
      }
      it("should send an InfoProvided with an IndentedText formatter with level 1 when called outside a test") {
        val spec = new InfoBeforeTestSpec
        val indentedText = getIndentedTextFromInfoProvided(spec)
        assert(indentedText === IndentedText("+ " + spec.msg, spec.msg, 0))
      }
      it("should send an InfoProvided with an IndentedText formatter with level 2 when called within a test") {
        val spec = new InfoInsideTestSpec
        val indentedText = getIndentedTextFromInfoProvided(spec)
        assert(indentedText === IndentedText("  + " + spec.msg, spec.msg, 1))
      }
    }
    it("should return registered tags, including ignore tags, from the tags method") {

      val a = new WordSpec {
        "should test this" ignore {}
        "should test that" in {}
      }
      expect(Map("should test this" -> Set("org.scalatest.Ignore"))) {
        a.tags
      }

      val b = new WordSpec {
        "should test this" in {}
        "should test that" ignore {}
      }
      expect(Map("should test that" -> Set("org.scalatest.Ignore"))) {
        b.tags
      }

      val c = new WordSpec {
        "should test this" ignore {}
        "should test that" ignore {}
      }
      expect(Map("should test this" -> Set("org.scalatest.Ignore"), "should test that" -> Set("org.scalatest.Ignore"))) {
        c.tags
      }

      val d = new WordSpec {
        "should test this" in {}
        "should test that" in {} // was an in
      }
      expect(Map()) {
        d.tags
      }

      val e = new WordSpec {
        "should test this" taggedAs(mytags.SlowAsMolasses) in {}
        "should test that" taggedAs(mytags.SlowAsMolasses) ignore {}
      }
      expect(Map("should test this" -> Set("org.scalatest.SlowAsMolasses"), "should test that" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses"))) {
        e.tags
      }

      val f = new WordSpec {}
      expect(Map()) {
        f.tags
      }

      val g = new WordSpec {
        "should test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) in {}
        "should test that" taggedAs(mytags.SlowAsMolasses) in {}
      }
      expect(Map("should test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "should test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        g.tags
      }
    }
    it("should throw NullPointerException if a null test tag is provided") {
      // it
      intercept[NullPointerException] {
        new WordSpec {
          "hi" taggedAs(null) in {}
        }
      }
      val caught = intercept[NullPointerException] {
        new WordSpec {
          "hi" taggedAs(mytags.SlowAsMolasses, null) in {}
        }
      }
      assert(caught.getMessage === "a test tag was null")
      intercept[NullPointerException] {
        new WordSpec {
          "hi" taggedAs(mytags.SlowAsMolasses, null, mytags.WeakAsAKitten) in {}
        }
      }
      // ignore
      intercept[NullPointerException] {
        new WordSpec {
          "hi" taggedAs(null) ignore {}
        }
      }
      val caught2 = intercept[NullPointerException] {
        new WordSpec {
          "hi" taggedAs(mytags.SlowAsMolasses, null) ignore {}
        }
      }
      assert(caught2.getMessage === "a test tag was null")
      intercept[NullPointerException] {
        new WordSpec {
          "hi" taggedAs(mytags.SlowAsMolasses, null, mytags.WeakAsAKitten) ignore {}
        }
      }
    }
    it("should return a correct tags map from the tags method") {

      val a = new WordSpec {
        "test this" ignore {}
        "test that" in {}
      }
      expect(Map("test this" -> Set("org.scalatest.Ignore"))) {
        a.tags
      }

      val b = new WordSpec {
        "test this" in {}
        "test that" ignore {}
      }
      expect(Map("test that" -> Set("org.scalatest.Ignore"))) {
        b.tags
      }

      val c = new WordSpec {
        "test this" ignore {}
        "test that" ignore {}
      }
      expect(Map("test this" -> Set("org.scalatest.Ignore"), "test that" -> Set("org.scalatest.Ignore"))) {
        c.tags
      }

      val d = new WordSpec {
        "test this" taggedAs(mytags.SlowAsMolasses) in {}
        "test that" taggedAs(mytags.SlowAsMolasses) ignore {}
      }
      expect(Map("test this" -> Set("org.scalatest.SlowAsMolasses"), "test that" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses"))) {
        d.tags
      }

      val e = new WordSpec {
        "test this" in {}
        "test that" in {}
      }
      expect(Map()) {
        e.tags
      }

      val f = new WordSpec {
        "test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) in {}
        "test that" taggedAs(mytags.SlowAsMolasses) in  {}
      }
      expect(Map("test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        f.tags
      }

      val g = new WordSpec {
        "test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) in {}
        "test that" taggedAs(mytags.SlowAsMolasses) in  {}
      }
      expect(Map("test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        g.tags
      }
    }
    it("should return a correct tags map from the tags method using is (pending)") {

      val a = new WordSpec {
        "test this" ignore {}
        "test that" is (pending)
      }
      expect(Map("test this" -> Set("org.scalatest.Ignore"))) {
        a.tags
      }

      val b = new WordSpec {
        "test this" is (pending)
        "test that" ignore {}
      }
      expect(Map("test that" -> Set("org.scalatest.Ignore"))) {
        b.tags
      }

      val c = new WordSpec {
        "test this" ignore {}
        "test that" ignore {}
      }
      expect(Map("test this" -> Set("org.scalatest.Ignore"), "test that" -> Set("org.scalatest.Ignore"))) {
        c.tags
      }

      val d = new WordSpec {
        "test this" taggedAs(mytags.SlowAsMolasses) is (pending)
        "test that" taggedAs(mytags.SlowAsMolasses) ignore {}
      }
      expect(Map("test this" -> Set("org.scalatest.SlowAsMolasses"), "test that" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses"))) {
        d.tags
      }

      val e = new WordSpec {
        "test this" is (pending)
        "test that" is (pending)
      }
      expect(Map()) {
        e.tags
      }

      val f = new WordSpec {
        "test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) is (pending)
        "test that" taggedAs(mytags.SlowAsMolasses) in  {}
      }
      expect(Map("test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        f.tags
      }

      val g = new WordSpec {
        "test this" taggedAs(mytags.SlowAsMolasses, mytags.WeakAsAKitten) is (pending)
        "test that" taggedAs(mytags.SlowAsMolasses) in  {}
      }
      expect(Map("test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        g.tags
      }
    }

    class TestWasCalledSuite extends WordSpec {
      var theTestThisCalled = false
      var theTestThatCalled = false
      "run this" in { theTestThisCalled = true }
      "run that, maybe" in { theTestThatCalled = true }
    }

    it("should execute all tests when run is called with testName None") {

      val b = new TestWasCalledSuite
      b.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(b.theTestThisCalled)
      assert(b.theTestThatCalled)
    }

    it("should execute one test when run is called with a defined testName") {

      val a = new TestWasCalledSuite
      a.run(Some("run this"), SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(a.theTestThisCalled)
      assert(!a.theTestThatCalled)
    }

    it("should report as ignored, and not run, tests marked ignored") {

      val a = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        "test this" in { theTestThisCalled = true }
        "test that" in { theTestThatCalled = true }
      }

      val repA = new TestIgnoredTrackingReporter
      a.run(None, repA, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repA.testIgnoredReceived)
      assert(a.theTestThisCalled)
      assert(a.theTestThatCalled)

      val b = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        "test this" ignore { theTestThisCalled = true }
        "test that" in { theTestThatCalled = true }
      }

      val repB = new TestIgnoredTrackingReporter
      b.run(None, repB, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(repB.testIgnoredReceived)
      assert(repB.lastEvent.isDefined)
      assert(repB.lastEvent.get.testName endsWith "test this")
      assert(!b.theTestThisCalled)
      assert(b.theTestThatCalled)

      val c = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        "test this" in { theTestThisCalled = true }
        "test that" ignore { theTestThatCalled = true }
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
      val d = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        "test this" ignore { theTestThisCalled = true }
        "test that" ignore { theTestThatCalled = true }
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
      val e = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        "test this" ignore { theTestThisCalled = true }
        "test that" in { theTestThatCalled = true }
      }

      val repE = new TestIgnoredTrackingReporter
      e.run(Some("test this"), repE, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repE.testIgnoredReceived)
      assert(e.theTestThisCalled)
      assert(!e.theTestThatCalled)
    }

    it("should run only those tests selected by the tags to include and exclude sets") {

      // Nothing is excluded
      val a = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        "test this" taggedAs(mytags.SlowAsMolasses) in { theTestThisCalled = true }
        "test that" in { theTestThatCalled = true }
      }
      val repA = new TestIgnoredTrackingReporter
      a.run(None, repA, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repA.testIgnoredReceived)
      assert(a.theTestThisCalled)
      assert(a.theTestThatCalled)

      // SlowAsMolasses is included, one test should be excluded
      val b = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        "test this" taggedAs(mytags.SlowAsMolasses) in { theTestThisCalled = true }
        "test that" in { theTestThatCalled = true }
      }
      val repB = new TestIgnoredTrackingReporter
      b.run(None, repB, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set()), Map(), None, new Tracker)
      assert(!repB.testIgnoredReceived)
      assert(b.theTestThisCalled)
      assert(!b.theTestThatCalled)

      // SlowAsMolasses is included, and both tests should be included
      val c = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        "test this" taggedAs(mytags.SlowAsMolasses) in { theTestThisCalled = true }
        "test that" taggedAs(mytags.SlowAsMolasses) in { theTestThatCalled = true }
      }
      val repC = new TestIgnoredTrackingReporter
      c.run(None, repB, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set()), Map(), None, new Tracker)
      assert(!repC.testIgnoredReceived)
      assert(c.theTestThisCalled)
      assert(c.theTestThatCalled)

      // SlowAsMolasses is included. both tests should be included but one ignored
      val d = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        "test this" taggedAs(mytags.SlowAsMolasses) ignore { theTestThisCalled = true }
        "test that" taggedAs(mytags.SlowAsMolasses) in { theTestThatCalled = true }
      }
      val repD = new TestIgnoredTrackingReporter
      d.run(None, repD, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.Ignore")), Map(), None, new Tracker)
      assert(repD.testIgnoredReceived)
      assert(!d.theTestThisCalled)
      assert(d.theTestThatCalled)

      // SlowAsMolasses included, FastAsLight excluded
      val e = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) in { theTestThisCalled = true }
        "test that" taggedAs(mytags.SlowAsMolasses) in { theTestThatCalled = true }
        "test the other" in { theTestTheOtherCalled = true }
      }
      val repE = new TestIgnoredTrackingReporter
      e.run(None, repE, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repE.testIgnoredReceived)
      assert(!e.theTestThisCalled)
      assert(e.theTestThatCalled)
      assert(!e.theTestTheOtherCalled)

      // An Ignored test that was both included and excluded should not generate a TestIgnored event
      val f = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) ignore { theTestThisCalled = true }
        "test that" taggedAs(mytags.SlowAsMolasses) in { theTestThatCalled = true }
        "test the other" in { theTestTheOtherCalled = true }
      }
      val repF = new TestIgnoredTrackingReporter
      f.run(None, repF, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repF.testIgnoredReceived)
      assert(!f.theTestThisCalled)
      assert(f.theTestThatCalled)
      assert(!f.theTestTheOtherCalled)

      // An Ignored test that was not included should not generate a TestIgnored event
      val g = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) in { theTestThisCalled = true }
        "test that" taggedAs(mytags.SlowAsMolasses) in { theTestThatCalled = true }
        "test the other" ignore { theTestTheOtherCalled = true }
      }
      val repG = new TestIgnoredTrackingReporter
      g.run(None, repG, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repG.testIgnoredReceived)
      assert(!g.theTestThisCalled)
      assert(g.theTestThatCalled)
      assert(!g.theTestTheOtherCalled)

      // No tagsToInclude set, FastAsLight excluded
      val h = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) in { theTestThisCalled = true }
        "test that" taggedAs(mytags.SlowAsMolasses) in { theTestThatCalled = true }
        "test the other" in { theTestTheOtherCalled = true }
      }
      val repH = new TestIgnoredTrackingReporter
      h.run(None, repH, new Stopper {}, Filter(None, Set("org.scalatest.FastAsLight")), Map(), None, new Tracker)
      assert(!repH.testIgnoredReceived)
      assert(!h.theTestThisCalled)
      assert(h.theTestThatCalled)
      assert(h.theTestTheOtherCalled)

      // No tagsToInclude set, mytags.SlowAsMolasses excluded
      val i = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) in { theTestThisCalled = true }
        "test that" taggedAs(mytags.SlowAsMolasses) in { theTestThatCalled = true }
        "test the other" in { theTestTheOtherCalled = true }
      }
      val repI = new TestIgnoredTrackingReporter
      i.run(None, repI, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses")), Map(), None, new Tracker)
      assert(!repI.testIgnoredReceived)
      assert(!i.theTestThisCalled)
      assert(!i.theTestThatCalled)
      assert(i.theTestTheOtherCalled)

      // No tagsToInclude set, mytags.SlowAsMolasses excluded, TestIgnored should not be received on excluded ones
      val j = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) ignore { theTestThisCalled = true }
        "test that" taggedAs(mytags.SlowAsMolasses) ignore { theTestThatCalled = true }
        "test the other" in { theTestTheOtherCalled = true }
      }
      val repJ = new TestIgnoredTrackingReporter
      j.run(None, repJ, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses")), Map(), None, new Tracker)
      assert(!repI.testIgnoredReceived)
      assert(!j.theTestThisCalled)
      assert(!j.theTestThatCalled)
      assert(j.theTestTheOtherCalled)

      // Same as previous, except Ignore specifically mentioned in excludes set
      val k = new WordSpec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        "test this" taggedAs(mytags.SlowAsMolasses, mytags.FastAsLight) ignore { theTestThisCalled = true }
        "test that" taggedAs(mytags.SlowAsMolasses) ignore { theTestThatCalled = true }
        "test the other" ignore { theTestTheOtherCalled = true }
      }
      val repK = new TestIgnoredTrackingReporter
      k.run(None, repK, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses", "org.scalatest.Ignore")), Map(), None, new Tracker)
      assert(repK.testIgnoredReceived)
      assert(!k.theTestThisCalled)
      assert(!k.theTestThatCalled)
      assert(!k.theTestTheOtherCalled)
    }

    it("should return the correct test count from its expectedTestCount method") {

      val a = new WordSpec {
        "test this" in {}
        "test that" in {}
      }
      assert(a.expectedTestCount(Filter()) === 2)

      val b = new WordSpec {
        "test this" ignore {}
        "test that" in {}
      }
      assert(b.expectedTestCount(Filter()) === 1)

      val c = new WordSpec {
        "test this" taggedAs(mytags.FastAsLight) in {}
        "test that" in {}
      }
      assert(c.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(c.expectedTestCount(Filter(None, Set("org.scalatest.FastAsLight"))) === 1)

      val d = new WordSpec {
        "test this" taggedAs(mytags.FastAsLight, mytags.SlowAsMolasses) in {}
        "test that" taggedAs(mytags.SlowAsMolasses) in {}
        "test the other thing" in {}
      }
      assert(d.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(d.expectedTestCount(Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight"))) === 1)
      assert(d.expectedTestCount(Filter(None, Set("org.scalatest.SlowAsMolasses"))) === 1)
      assert(d.expectedTestCount(Filter()) === 3)

      val e = new WordSpec {
        "test this" taggedAs(mytags.FastAsLight, mytags.SlowAsMolasses) in {}
        "test that" taggedAs(mytags.SlowAsMolasses) in {}
        "test the other thing" ignore {}
      }
      assert(e.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(e.expectedTestCount(Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight"))) === 1)
      assert(e.expectedTestCount(Filter(None, Set("org.scalatest.SlowAsMolasses"))) === 0)
      assert(e.expectedTestCount(Filter()) === 2)

      val f = new Suites(a, b, c, d, e)
      assert(f.expectedTestCount(Filter()) === 10)
    }
    it("should generate a TestPending message when the test body is (pending)") {
      val a = new WordSpec {

        "should do this" is (pending)

        "should do that" in {
          assert(2 + 2 === 4)
        }
        "should do something else" in {
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
      val a = new WordSpec {
        "This WordSpec" should {
          "throw AssertionError" in { throw new AssertionError }
          "throw plain old Error" in { throw new Error }
          "throw Throwable" in { throw new Throwable }
        }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      val tf = rep.testFailedEventsReceived
      assert(tf.size === 3)
    }
    it("should propagate out Errors that are direct subtypes of Error in JDK 1.5, other than " +
            "AssertionError, causing Suites and Runs to abort.") {
      val a = new WordSpec {
        "This WordSpec" should {
          "throw AssertionError" in { throw new OutOfMemoryError }
        }
      }
      intercept[OutOfMemoryError] {
        a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      }
    }
    it("should send InfoProvided events with aboutAPendingTest set to true for info " +
            "calls made from a test that is pending") {
      val a = new WordSpec with GivenWhenThen {
        "A WordSpec" should {
          "do something" in {
            given("two integers")
            when("one is subracted from the other")
            then("the result is the difference between the two numbers")
            pending
          }
        }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      val ip = rep.infoProvidedEventsReceived
      assert(ip.size === 4)
      for (event <- ip) {
        assert(event.message == "A WordSpec" || event.aboutAPendingTest.isDefined && event.aboutAPendingTest.get)
      }
    }
    it("should send InfoProvided events with aboutAPendingTest set to false for info " +
            "calls made from a test that is not pending") {
      val a = new WordSpec with GivenWhenThen {
        "A WordSpec" should {
          "do something" in {
            given("two integers")
            when("one is subracted from the other")
            then("the result is the difference between the two numbers")
            assert(1 + 1 === 2)
          }
        }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      val ip = rep.infoProvidedEventsReceived
      assert(ip.size === 4)
      for (event <- ip) {
        assert(event.message == "A WordSpec" || event.aboutAPendingTest.isDefined && !event.aboutAPendingTest.get)
      }
    }
    it("should not put parentheses around should clauses that follow when") {
      val a = new WordSpec {
        "A Stack" when {
          "empty" should {
            "chill out" in {
              assert(1 + 1 === 2)
            }
          }
        }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      val ts = rep.testSucceededEventsReceived
      assert(ts.size === 1)
      assert(ts.head.testName === "A Stack when empty should chill out")
    }
    it("should not put parentheses around should clauses that don't follow when") {
      val a = new WordSpec {
        "A Stack" should {
          "chill out" in {
            assert(1 + 1 === 2)
          }
        }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      val ts = rep.testSucceededEventsReceived
      assert(ts.size === 1)
      assert(ts.head.testName === "A Stack should chill out")
    }
  }
}
