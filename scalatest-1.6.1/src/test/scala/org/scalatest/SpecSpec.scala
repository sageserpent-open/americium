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

class SpecSpec extends Spec with SharedHelpers with GivenWhenThen {

  describe("A Spec") {

    it("should return the test names in registration order from testNames") {

      val a = new Spec {
        it("should test this") {}
        it("should test that") {}
      }

      expect(List("should test this", "should test that")) {
        a.testNames.iterator.toList
      }

      val b = new Spec {}

      expect(List[String]()) {
        b.testNames.iterator.toList
      }

      val c = new Spec {
        it("should test that") {}
        it("should test this") {}
      }

      expect(List("should test that", "should test this")) {
        c.testNames.iterator.toList
      }

      val d = new Spec {
        describe("A Tester") {
          it("should test that") {}
          it("should test this") {}
        }
      }

      expect(List("A Tester should test that", "A Tester should test this")) {
        d.testNames.iterator.toList
      }

      val e = new Spec {
        describe("A Tester") {
          it("should test this") {}
          it("should test that") {}
        }
      }

      expect(List("A Tester should test this", "A Tester should test that")) {
        e.testNames.iterator.toList
      }
    }

    it("should throw DuplicateTestNameException if a duplicate test name registration is attempted") {
      
      intercept[DuplicateTestNameException] {
        new Spec {
          it("should test this") {}
          it("should test this") {}
        }
      }
      intercept[DuplicateTestNameException] {
        new Spec {
          it("should test this") {}
          ignore("should test this") {}
        }
      }
      intercept[DuplicateTestNameException] {
        new Spec {
          ignore("should test this") {}
          ignore("should test this") {}
        }
      }
      intercept[DuplicateTestNameException] {
        new Spec {
          ignore("should test this") {}
          it("should test this") {}
        }
      }
    }

    it("should invoke withFixture from runTest") {
      val a = new Spec {
        var withFixtureWasInvoked = false
        var testWasInvoked = false
        override def withFixture(test: NoArgTest) {
          withFixtureWasInvoked = true
          super.withFixture(test)
        }
        it("should do something") {
          testWasInvoked = true
        }
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.withFixtureWasInvoked)
      assert(a.testWasInvoked)
    }
    it("should pass the correct test name in the NoArgTest passed to withFixture") {
      val a = new Spec {
        var correctTestNameWasPassed = false
        override def withFixture(test: NoArgTest) {
          correctTestNameWasPassed = test.name == "should do something"
          super.withFixture(test)
        }
        it("should do something") {}
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      assert(a.correctTestNameWasPassed)
    }
    it("should pass the correct config map in the NoArgTest passed to withFixture") {
      val a = new Spec {
        var correctConfigMapWasPassed = false
        override def withFixture(test: NoArgTest) {
          correctConfigMapWasPassed = (test.configMap == Map("hi" -> 7))
          super.withFixture(test)
        }
        it("should do something") {}
      }
      a.run(None, SilentReporter, new Stopper {}, Filter(), Map("hi" -> 7), None, new Tracker())
      assert(a.correctConfigMapWasPassed)
    }
    describe("(with info calls)") {
      class InfoInsideTestSpec extends Spec {
        val msg = "hi there, dude"
        val testName = "test name"
        it(testName) {
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
      class InfoBeforeTestSpec extends Spec {
        val msg = "hi there, dude"
        val testName = "test name"
        info(msg)
        it(testName) {}
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
        class MySpec extends Spec {
          it(testName) {}
          info(msg)
        }
        val (infoProvidedIndex, testStartingIndex, testSucceededIndex) =
          getIndexesForInformerEventOrderTests(new MySpec, testName, msg)
        assert(testStartingIndex < testSucceededIndex)
        assert(testSucceededIndex < infoProvidedIndex)
      }
      it("should throw an IllegalStateException when info is called by a method invoked after the suite has been executed") {
        class MySpec extends Spec {
          callInfo() // This should work fine
          def callInfo() {
            info("howdy")
          }
          it("howdy also") {
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

      val a = new Spec {
        ignore("should test this") {}
        it("should test that") {}
      }
      expect(Map("should test this" -> Set("org.scalatest.Ignore"))) {
        a.tags
      }

      val b = new Spec {
        it("should test this") {}
        ignore("should test that") {}
      }
      expect(Map("should test that" -> Set("org.scalatest.Ignore"))) {
        b.tags
      }

      val c = new Spec {
        ignore("should test this") {}
        ignore("should test that") {}
      }
      expect(Map("should test this" -> Set("org.scalatest.Ignore"), "should test that" -> Set("org.scalatest.Ignore"))) {
        c.tags
      }

      val d = new Spec {
        it("should test this") {}
        it("should test that") {}
      }
      expect(Map()) {
        d.tags
      }

      val e = new Spec {
        it("should test this", mytags.SlowAsMolasses) {}
        ignore("should test that", mytags.SlowAsMolasses) {}
      }
      expect(Map("should test this" -> Set("org.scalatest.SlowAsMolasses"), "should test that" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses"))) {
        e.tags
      }

      val f = new Spec {}
      expect(Map()) {
        f.tags
      }

      val g = new Spec {
        it("should test this", mytags.SlowAsMolasses, mytags.WeakAsAKitten) {}
        it("should test that", mytags.SlowAsMolasses) {}
      }
      expect(Map("should test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "should test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        g.tags
      }
    }

    describe("(when a nesting rule has been violated)") {

      it("should, if they call a describe from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends Spec {
          it("should blow up") {
            describe("in the wrong place, at the wrong time") {
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a describe with a nested it from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends Spec {
          it("should blow up") {
            describe("in the wrong place, at the wrong time") {
              it("should never run") {
                assert(1 === 1)
              }
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a nested it from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends Spec {
          it("should blow up") {
            it("should never run") {
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a nested it with tags from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends Spec {
          it("should blow up") {
            it("should never run", mytags.SlowAsMolasses) {
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a describe with a nested ignore from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends Spec {
          it("should blow up") {
            describe("in the wrong place, at the wrong time") {
              ignore("should never run") {
                assert(1 === 1)
              }
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a nested ignore from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends Spec {
          it("should blow up") {
            ignore("should never run") {
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
      it("should, if they call a nested ignore with tags from within an it clause, result in a TestFailedException when running the test") {

        class MySpec extends Spec {
          it("should blow up") {
            ignore("should never run", mytags.SlowAsMolasses) {
              assert(1 === 1)
            }
          }
        }

        val spec = new MySpec
        ensureTestFailedEventReceived(spec, "should blow up")
      }
    }
    it("should run tests registered via the 'it should behave like' syntax") {
      trait SharedSpecTests { this: Spec =>
        def nonEmptyStack(s: String)(i: Int) {
          it("should be that I am shared") {}
        }
      }
      class MySpec extends Spec with SharedSpecTests {
        it should behave like nonEmptyStack("hi")(1)
      }
      val suite = new MySpec
      val reporter = new EventRecordingReporter
      suite.run(None, reporter, new Stopper {}, Filter(), Map(), None, new Tracker)

      val indexedList = reporter.eventsReceived

      val testStartingOption = indexedList.find(_.isInstanceOf[TestStarting])
      assert(testStartingOption.isDefined)
      assert(testStartingOption.get.asInstanceOf[TestStarting].testName === "should be that I am shared")
    }

    it("should throw NullPointerException if a null test tag is provided") {
      // it
      intercept[NullPointerException] {
        new Spec {
          it("hi", null) {}
        }
      }
      val caught = intercept[NullPointerException] {
        new Spec {
          it("hi", mytags.SlowAsMolasses, null) {}
        }
      }
      assert(caught.getMessage === "a test tag was null")
      intercept[NullPointerException] {
        new Spec {
          it("hi", mytags.SlowAsMolasses, null, mytags.WeakAsAKitten) {}
        }
      }
      // ignore
      intercept[NullPointerException] {
        new Spec {
          ignore("hi", null) {}
        }
      }
      val caught2 = intercept[NullPointerException] {
        new Spec {
          ignore("hi", mytags.SlowAsMolasses, null) {}
        }
      }
      assert(caught2.getMessage === "a test tag was null")
      intercept[NullPointerException] {
        new Spec {
          ignore("hi", mytags.SlowAsMolasses, null, mytags.WeakAsAKitten) {}
        }
      }
    }
    it("should return a correct tags map from the tags method") {

      val a = new Spec {
        ignore("test this") {}
        it("test that") {}
      }
      expect(Map("test this" -> Set("org.scalatest.Ignore"))) {
        a.tags
      }

      val b = new Spec {
        it("test this") {}
        ignore("test that") {}
      }
      expect(Map("test that" -> Set("org.scalatest.Ignore"))) {
        b.tags
      }

      val c = new Spec {
        ignore("test this") {}
        ignore("test that") {}
      }
      expect(Map("test this" -> Set("org.scalatest.Ignore"), "test that" -> Set("org.scalatest.Ignore"))) {
        c.tags
      }

      val d = new Spec {
        it("test this", mytags.SlowAsMolasses) {}
        ignore("test that", mytags.SlowAsMolasses) {}
      }
      expect(Map("test this" -> Set("org.scalatest.SlowAsMolasses"), "test that" -> Set("org.scalatest.Ignore", "org.scalatest.SlowAsMolasses"))) {
        d.tags
      }

      val e = new Spec {}
      expect(Map()) {
        e.tags
      }

      val f = new Spec {
        it("test this", mytags.SlowAsMolasses, mytags.WeakAsAKitten) {}
        it("test that", mytags.SlowAsMolasses) {}
      }
      expect(Map("test this" -> Set("org.scalatest.SlowAsMolasses", "org.scalatest.WeakAsAKitten"), "test that" -> Set("org.scalatest.SlowAsMolasses"))) {
        f.tags
      }
    }

    class TestWasCalledSuite extends Spec {
      var theTestThisCalled = false
      var theTestThatCalled = false
      it("should run this") { theTestThisCalled = true }
      it("should run that, maybe") { theTestThatCalled = true }
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

      val a = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        it("test this") { theTestThisCalled = true }
        it("test that") { theTestThatCalled = true }
      }

      val repA = new TestIgnoredTrackingReporter
      a.run(None, repA, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repA.testIgnoredReceived)
      assert(a.theTestThisCalled)
      assert(a.theTestThatCalled)

      val b = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        ignore("test this") { theTestThisCalled = true }
        it("test that") { theTestThatCalled = true }
      }

      val repB = new TestIgnoredTrackingReporter
      b.run(None, repB, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(repB.testIgnoredReceived)
      assert(repB.lastEvent.isDefined)
      assert(repB.lastEvent.get.testName endsWith "test this")
      assert(!b.theTestThisCalled)
      assert(b.theTestThatCalled)

      val c = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        it("test this") { theTestThisCalled = true }
        ignore("test that") { theTestThatCalled = true }
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
      val d = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        ignore("test this") { theTestThisCalled = true }
        ignore("test that") { theTestThatCalled = true }
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
      val e = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        ignore("test this") { theTestThisCalled = true }
        it("test that") { theTestThatCalled = true }
      }

      val repE = new TestIgnoredTrackingReporter
      e.run(Some("test this"), repE, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repE.testIgnoredReceived)
      assert(e.theTestThisCalled)
    }

    it("should run only those tests selected by the tags to include and exclude sets") {

      // Nothing is excluded
      val a = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        it("test this", mytags.SlowAsMolasses) { theTestThisCalled = true }
        it("test that") { theTestThatCalled = true }
      }
      val repA = new TestIgnoredTrackingReporter
      a.run(None, repA, new Stopper {}, Filter(), Map(), None, new Tracker)
      assert(!repA.testIgnoredReceived)
      assert(a.theTestThisCalled)
      assert(a.theTestThatCalled)

      // SlowAsMolasses is included, one test should be excluded
      val b = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        it("test this", mytags.SlowAsMolasses) { theTestThisCalled = true }
        it("test that") { theTestThatCalled = true }
      }
      val repB = new TestIgnoredTrackingReporter
      b.run(None, repB, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set()), Map(), None, new Tracker)
      assert(!repB.testIgnoredReceived)
      assert(b.theTestThisCalled)
      assert(!b.theTestThatCalled)

      // SlowAsMolasses is included, and both tests should be included
      val c = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        it("test this", mytags.SlowAsMolasses) { theTestThisCalled = true }
        it("test that", mytags.SlowAsMolasses) { theTestThatCalled = true }
      }
      val repC = new TestIgnoredTrackingReporter
      c.run(None, repB, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set()), Map(), None, new Tracker)
      assert(!repC.testIgnoredReceived)
      assert(c.theTestThisCalled)
      assert(c.theTestThatCalled)

      // SlowAsMolasses is included. both tests should be included but one ignored
      val d = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        ignore("test this", mytags.SlowAsMolasses) { theTestThisCalled = true }
        it("test that", mytags.SlowAsMolasses) { theTestThatCalled = true }
      }
      val repD = new TestIgnoredTrackingReporter
      d.run(None, repD, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.Ignore")), Map(), None, new Tracker)
      assert(repD.testIgnoredReceived)
      assert(!d.theTestThisCalled)
      assert(d.theTestThatCalled)

      // SlowAsMolasses included, FastAsLight excluded
      val e = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        it("test this", mytags.SlowAsMolasses, mytags.FastAsLight) { theTestThisCalled = true }
        it("test that", mytags.SlowAsMolasses) { theTestThatCalled = true }
        it("test the other") { theTestTheOtherCalled = true }
      }
      val repE = new TestIgnoredTrackingReporter
      e.run(None, repE, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repE.testIgnoredReceived)
      assert(!e.theTestThisCalled)
      assert(e.theTestThatCalled)
      assert(!e.theTestTheOtherCalled)

      // An Ignored test that was both included and excluded should not generate a TestIgnored event
      val f = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        ignore("test this", mytags.SlowAsMolasses, mytags.FastAsLight) { theTestThisCalled = true }
        it("test that", mytags.SlowAsMolasses) { theTestThatCalled = true }
        it("test the other") { theTestTheOtherCalled = true }
      }
      val repF = new TestIgnoredTrackingReporter
      f.run(None, repF, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repF.testIgnoredReceived)
      assert(!f.theTestThisCalled)
      assert(f.theTestThatCalled)
      assert(!f.theTestTheOtherCalled)

      // An Ignored test that was not included should not generate a TestIgnored event
      val g = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        it("test this", mytags.SlowAsMolasses, mytags.FastAsLight) { theTestThisCalled = true }
        it("test that", mytags.SlowAsMolasses) { theTestThatCalled = true }
        ignore("test the other") { theTestTheOtherCalled = true }
      }
      val repG = new TestIgnoredTrackingReporter
      g.run(None, repG, new Stopper {}, Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight")),
                Map(), None, new Tracker)
      assert(!repG.testIgnoredReceived)
      assert(!g.theTestThisCalled)
      assert(g.theTestThatCalled)
      assert(!g.theTestTheOtherCalled)

      // No tagsToInclude set, FastAsLight excluded
      val h = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        it("test this", mytags.SlowAsMolasses, mytags.FastAsLight) { theTestThisCalled = true }
        it("test that", mytags.SlowAsMolasses) { theTestThatCalled = true }
        it("test the other") { theTestTheOtherCalled = true }
      }
      val repH = new TestIgnoredTrackingReporter
      h.run(None, repH, new Stopper {}, Filter(None, Set("org.scalatest.FastAsLight")), Map(), None, new Tracker)
      assert(!repH.testIgnoredReceived)
      assert(!h.theTestThisCalled)
      assert(h.theTestThatCalled)
      assert(h.theTestTheOtherCalled)

      // No tagsToInclude set, mytags.SlowAsMolasses excluded
      val i = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        it("test this", mytags.SlowAsMolasses, mytags.FastAsLight) { theTestThisCalled = true }
        it("test that", mytags.SlowAsMolasses) { theTestThatCalled = true }
        it("test the other") { theTestTheOtherCalled = true }
      }
      val repI = new TestIgnoredTrackingReporter
      i.run(None, repI, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses")), Map(), None, new Tracker)
      assert(!repI.testIgnoredReceived)
      assert(!i.theTestThisCalled)
      assert(!i.theTestThatCalled)
      assert(i.theTestTheOtherCalled)

      // No tagsToInclude set, mytags.SlowAsMolasses excluded, TestIgnored should not be received on excluded ones
      val j = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        ignore("test this", mytags.SlowAsMolasses, mytags.FastAsLight) { theTestThisCalled = true }
        ignore("test that", mytags.SlowAsMolasses) { theTestThatCalled = true }
        it("test the other") { theTestTheOtherCalled = true }
      }
      val repJ = new TestIgnoredTrackingReporter
      j.run(None, repJ, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses")), Map(), None, new Tracker)
      assert(!repI.testIgnoredReceived)
      assert(!j.theTestThisCalled)
      assert(!j.theTestThatCalled)
      assert(j.theTestTheOtherCalled)

      // Same as previous, except Ignore specifically mentioned in excludes set
      val k = new Spec {
        var theTestThisCalled = false
        var theTestThatCalled = false
        var theTestTheOtherCalled = false
        ignore("test this", mytags.SlowAsMolasses, mytags.FastAsLight) { theTestThisCalled = true }
        ignore("test that", mytags.SlowAsMolasses) { theTestThatCalled = true }
        ignore("test the other") { theTestTheOtherCalled = true }
      }
      val repK = new TestIgnoredTrackingReporter
      k.run(None, repK, new Stopper {}, Filter(None, Set("org.scalatest.SlowAsMolasses", "org.scalatest.Ignore")), Map(), None, new Tracker)
      assert(repK.testIgnoredReceived)
      assert(!k.theTestThisCalled)
      assert(!k.theTestThatCalled)
      assert(!k.theTestTheOtherCalled)
    }

    it("should return the correct test count from its expectedTestCount method") {

      val a = new Spec {
        it("test this") {}
        it("test that") {}
      }
      assert(a.expectedTestCount(Filter()) === 2)

      val b = new Spec {
        ignore("test this") {}
        it("test that") {}
      }
      assert(b.expectedTestCount(Filter()) === 1)

      val c = new Spec {
        it("test this", mytags.FastAsLight) {}
        it("test that") {}
      }
      assert(c.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(c.expectedTestCount(Filter(None, Set("org.scalatest.FastAsLight"))) === 1)

      val d = new Spec {
        it("test this", mytags.FastAsLight, mytags.SlowAsMolasses) {}
        it("test that", mytags.SlowAsMolasses) {}
        it("test the other thing") {}
      }
      assert(d.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(d.expectedTestCount(Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight"))) === 1)
      assert(d.expectedTestCount(Filter(None, Set("org.scalatest.SlowAsMolasses"))) === 1)
      assert(d.expectedTestCount(Filter()) === 3)

      val e = new Spec {
        it("test this", mytags.FastAsLight, mytags.SlowAsMolasses) {}
        it("test that", mytags.SlowAsMolasses) {}
        ignore("test the other thing") {}
      }
      assert(e.expectedTestCount(Filter(Some(Set("org.scalatest.FastAsLight")), Set())) === 1)
      assert(e.expectedTestCount(Filter(Some(Set("org.scalatest.SlowAsMolasses")), Set("org.scalatest.FastAsLight"))) === 1)
      assert(e.expectedTestCount(Filter(None, Set("org.scalatest.SlowAsMolasses"))) === 0)
      assert(e.expectedTestCount(Filter()) === 2)

      val f = new Suites(a, b, c, d, e)
      assert(f.expectedTestCount(Filter()) === 10)
    }
    
    it("should generate a TestPending message when the test body is (pending)") {

      val a = new Spec {

        it("should do this") (pending)

        it("should do that") {
          assert(2 + 2 === 4)
        }
        
        it("should do something else") {
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
      val a = new Spec {
        it("throws AssertionError") { throw new AssertionError }
        it("throws plain old Error") { throw new Error }
        it("throws Throwable") { throw new Throwable }
      }
      val rep = new EventRecordingReporter
      a.run(None, rep, new Stopper {}, Filter(), Map(), None, new Tracker())
      val tf = rep.testFailedEventsReceived
      assert(tf.size === 3)
    }
    it("should propagate out Errors that are direct subtypes of Error in JDK 1.5, other than " +
            "AssertionError, causing Suites and Runs to abort.") {
      val a = new Spec {
        it("throws AssertionError") { throw new OutOfMemoryError }
      }
      intercept[OutOfMemoryError] {
        a.run(None, SilentReporter, new Stopper {}, Filter(), Map(), None, new Tracker())
      }
    }
    it("should send InfoProvided events with aboutAPendingTest set to true for info " +
            "calls made from a test that is pending") {
      val a = new Spec with GivenWhenThen {
        it("should do something else") {
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
      val a = new Spec with GivenWhenThen {
        it("should do something else") {
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
  }
}

