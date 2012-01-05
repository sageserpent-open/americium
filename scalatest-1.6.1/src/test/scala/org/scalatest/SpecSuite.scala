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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.events._

class SpecSuite extends FunSuite with SharedHelpers {

  test("three plain-old specifiers should be invoked in order") {
    class MySpec extends Spec with ShouldMatchers {
      var example1WasInvoked = false
      var example2WasInvokedAfterExample1 = false
      var example3WasInvokedAfterExample2 = false
      it("should get invoked") {
        example1WasInvoked = true
      }
      it("should also get invoked") {
        if (example1WasInvoked)
          example2WasInvokedAfterExample1 = true
      }
      it("should also also get invoked") {
        if (example2WasInvokedAfterExample1)
          example3WasInvokedAfterExample2 = true
      }
    }
    val a = new MySpec
    a.execute()
    assert(a.example1WasInvoked)
    assert(a.example2WasInvokedAfterExample1)
    assert(a.example3WasInvokedAfterExample2)
  }

  test("three plain-old specifiers should be invoked in order when two are surrounded by a plain-old describe") {
    class MySpec extends Spec with ShouldMatchers {
      var example1WasInvoked = false
      var example2WasInvokedAfterExample1 = false
      var example3WasInvokedAfterExample2 = false
      it("should get invoked") {
        example1WasInvoked = true
      }
      describe("Stack") {
        it("should also get invoked") {
          if (example1WasInvoked)
            example2WasInvokedAfterExample1 = true
        }
        it("should also also get invoked") {
          if (example2WasInvokedAfterExample1)
            example3WasInvokedAfterExample2 = true
        }
      }
    }
    val a = new MySpec
    a.execute()
    assert(a.example1WasInvoked)
    assert(a.example2WasInvokedAfterExample1)
    assert(a.example3WasInvokedAfterExample2)
  }
   
  test("two plain-old specifiers should show up in order of appearance in testNames") {
    class MySpec extends Spec with ShouldMatchers {
      var example1WasInvoked = false
      var example2WasInvokedAfterExample1 = false
      it("should get invoked") {
        example1WasInvoked = true
      }
      it("should also get invoked") {
        if (example1WasInvoked)
          example2WasInvokedAfterExample1 = true
      }
    }
    val a = new MySpec
    a.execute()
    assert(a.testNames.size === 2)
    assert(a.testNames.iterator.toList(0) === "should get invoked")
    assert(a.testNames.iterator.toList(1) === "should also get invoked")
  }
 
  test("plain-old specifier test names should include an enclosing describe string, separated by a space") {
    class MySpec extends Spec with ShouldMatchers {
      describe("A Stack") {
        it("must allow me to pop") {}
        it("must allow me to push") {}
      }
    }
    val a = new MySpec
    assert(a.testNames.size === 2)
    assert(a.testNames.iterator.toList(0) === "A Stack must allow me to pop")
    assert(a.testNames.iterator.toList(1) === "A Stack must allow me to push")
  }

  test("plain-old test names should properly nest plain-old descriptions in test names") {
    class MySpec extends Spec with ShouldMatchers {
      describe("A Stack") {
        describe("(when not empty)") {
          it("must allow me to pop") {}
        }
        describe("(when not full)") {
          it("must allow me to push") {}
        }
      }
    }
    val a = new MySpec
    assert(a.testNames.size === 2)
    assert(a.testNames.iterator.toList(0) === "A Stack (when not empty) must allow me to pop")
    assert(a.testNames.iterator.toList(1) === "A Stack (when not full) must allow me to push")
  }
  
  test("should be able to mix in BeforeAndAfterEach with BeforeAndAfterAll without any problems") {
    class MySpec extends Spec with ShouldMatchers with BeforeAndAfterEach with BeforeAndAfterAll {
      describe("A Stack") {
        describe("(when not empty)") {
          it("should allow me to pop") {}
        }
        describe("(when not full)") {
          it("should allow me to push") {}
        }
      }
    }
    val a = new MySpec
    a.execute()
  }
  
  // Test for good strings in report for top-level examples  
  test("Top-level plain-old specifiers should yield good strings in a TestSucceeded report") {
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            if (testName.indexOf("must start with proper words") != -1)
              reportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "must start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "- must start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      it("must start with proper words") {}
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
  }
    
  test("Top-level plain-old specifiers should yield good strings in a testSucceeded report") {
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            if (testName.indexOf("must start with proper words") != -1)
              reportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "must start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "- must start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      it("must start with proper words") {}
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
  }

  test("Top-level plain-old specifiers should yield good strings in a testFailed report") {
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case event: TestFailed =>
            if (event.testName.indexOf("must start with proper words") != -1)
              reportHadCorrectTestName = true
            event.formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "must start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "- must start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      it("must start with proper words") { fail() }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
  }

  // Tests for good strings in report for nested-one-level examples
  test("Nested-one-level plain-old specifiers should yield good strings in a TestSucceeded report") {
    var infoReportHadCorrectTestName = false
    var infoReportHadCorrectSpecText = false
    var infoReportHadCorrectFormattedSpecText = false
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    var infoProvidedHasBeenInvoked = false
    var theOtherMethodHasBeenInvoked = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case InfoProvided(ordinal, message, nameInfo, aboutAPendingTest, throwable, formatter, payload, threadName, timeStamp) =>
            // infoProvided should be invoked before the other method
            assert(!theOtherMethodHasBeenInvoked)
            infoProvidedHasBeenInvoked = true
            if (message.indexOf("My Spec") != -1)
              infoReportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "My Spec")
                  infoReportHadCorrectSpecText = true
                if (formattedText == "My Spec")
                  infoReportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            // infoProvided should be invoked before the this method
            assert(infoProvidedHasBeenInvoked)
            theOtherMethodHasBeenInvoked = true
            if (testName.indexOf("My Spec must start with proper words") != -1)
              reportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "must start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "- must start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      describe("My Spec") {
        it("must start with proper words") {}
      }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
    assert(infoReportHadCorrectTestName)
    assert(infoReportHadCorrectSpecText)
    assert(infoReportHadCorrectFormattedSpecText)
  }

  test("Nested-one-level plain-old specifiers should yield good strings in a testSucceeded report") {
    var infoReportHadCorrectTestName = false
    var infoReportHadCorrectSpecText = false
    var infoReportHadCorrectFormattedSpecText = false
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    var infoProvidedHasBeenInvoked = false
    var theOtherMethodHasBeenInvoked = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case InfoProvided(ordinal, message, nameInfo, aboutAPendingTest, throwable, formatter, payload, threadName, timeStamp) =>
            // infoProvided should be invoked before the other method
            assert(!theOtherMethodHasBeenInvoked)
            infoProvidedHasBeenInvoked = true
            if (message.indexOf("My Spec") != -1)
              infoReportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "My Spec")
                  infoReportHadCorrectSpecText = true
                if (formattedText == "My Spec")
                  infoReportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            // infoProvided should be invoked before the this method
            assert(infoProvidedHasBeenInvoked)
            theOtherMethodHasBeenInvoked = true
            if (testName.indexOf("My Spec must start with proper words") != -1)
              reportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "must start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "- must start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      describe("My Spec") {
        it("must start with proper words") {}
      }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
    assert(infoReportHadCorrectTestName)
    assert(infoReportHadCorrectSpecText)
    assert(infoReportHadCorrectFormattedSpecText)
  }

  test("Nested-one-level plain-old specifiers should yield good strings in a TestFailed report") {
    var infoReportHadCorrectTestName = false
    var infoReportHadCorrectSpecText = false
    var infoReportHadCorrectFormattedSpecText = false
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    var infoProvidedHasBeenInvoked = false
    var theOtherMethodHasBeenInvoked = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case InfoProvided(ordinal, message, nameInfo, aboutAPendingTest, throwable, formatter, payload, threadName, timeStamp) =>
            // infoProvided should be invoked before the other method
            assert(!theOtherMethodHasBeenInvoked)
            infoProvidedHasBeenInvoked = true
            if (message.indexOf("My Spec") != -1)
              infoReportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "My Spec")
                  infoReportHadCorrectSpecText = true
                if (formattedText == "My Spec")
                  infoReportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case event: TestFailed =>
            // infoProvided should be invoked before the this method
            assert(infoProvidedHasBeenInvoked)
            theOtherMethodHasBeenInvoked = true
            if (event.testName.indexOf("My Spec must start with proper words") != -1)
              reportHadCorrectTestName = true
            event.formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "must start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "- must start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      describe("My Spec") {
        it("must start with proper words") { fail() }
      }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
    assert(infoReportHadCorrectTestName)
    assert(infoReportHadCorrectSpecText)
    assert(infoReportHadCorrectFormattedSpecText)
  }

  
  // Tests for good strings in report for nested-two-levels examples
  test("Nested-two-levels plain-old specifiers should yield good strings in a TestSucceeded report") { //ZZZ
    var infoReportHadCorrectTestName = false
    var infoReportHadCorrectSpecText = false
    var infoReportHadCorrectFormattedSpecText = false
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    var infoProvidedHasBeenInvokedOnce = false
    var infoProvidedHasBeenInvokedTwice = false
    var theOtherMethodHasBeenInvoked = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case InfoProvided(ordinal, message, nameInfo, aboutAPendingTest, throwable, formatter, payload, threadName, timeStamp) =>
            // infoProvided should be invoked before the other method
            assert(!theOtherMethodHasBeenInvoked)
            if (!infoProvidedHasBeenInvokedOnce) { 
              infoProvidedHasBeenInvokedOnce = true
              if (message.indexOf("My") >= 0)
                infoReportHadCorrectTestName = true
              formatter match {
                case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                  if (rawText == "My")
                    infoReportHadCorrectSpecText = true
                  if (formattedText == "My")
                    infoReportHadCorrectFormattedSpecText = true
                case _ =>
              }
            }
            else {
              infoProvidedHasBeenInvokedTwice = true
              if (message.indexOf("Spec") < 0)
                infoReportHadCorrectTestName = false
              formatter match {
                case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                  if (rawText != "Spec")
                    infoReportHadCorrectSpecText = false
                  if (formattedText != "  Spec")
                    infoReportHadCorrectFormattedSpecText = false
                case _ =>
              }
            }
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            // infoProvided should be invoked before the this method
            assert(infoProvidedHasBeenInvokedTwice)
            theOtherMethodHasBeenInvoked = true
            if (testName.indexOf("My Spec must start with proper words") != -1)
              reportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "must start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "  - must start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      describe("My") {
        describe("Spec") {
          it("must start with proper words") {}
        }
      }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
    assert(infoReportHadCorrectTestName)
    assert(infoReportHadCorrectSpecText)
    assert(infoReportHadCorrectFormattedSpecText)
  }

  test("Nested-two-levels plain-old specifiers should yield good strings in a TestFailed report") { //YYY
    var infoReportHadCorrectTestName = false
    var infoReportHadCorrectSpecText = false
    var infoReportHadCorrectFormattedSpecText = false
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    var infoProvidedHasBeenInvokedOnce = false
    var infoProvidedHasBeenInvokedTwice = false
    var theOtherMethodHasBeenInvoked = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case InfoProvided(ordinal, message, nameInfo, aboutAPendingTest, throwable, formatter, payload, threadName, timeStamp) =>
            // infoProvided should be invoked before the other method
            assert(!theOtherMethodHasBeenInvoked)
            if (!infoProvidedHasBeenInvokedOnce) { 
              infoProvidedHasBeenInvokedOnce = true
              if (message.indexOf("My") >= 0)
                infoReportHadCorrectTestName = true
              formatter match {
                case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                  if (rawText == "My")
                    infoReportHadCorrectSpecText = true
                  if (formattedText == "My")
                    infoReportHadCorrectFormattedSpecText = true
                case _ =>
              }
            }
            else {
              infoProvidedHasBeenInvokedTwice = true
              if (message.indexOf("Spec") < 0)
                infoReportHadCorrectTestName = false
              formatter match {
                case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                  if (rawText != "Spec")
                    infoReportHadCorrectSpecText = false
                  if (formattedText != "  Spec")
                    infoReportHadCorrectFormattedSpecText = false
                case _ =>
              }
            }
          case event: TestFailed =>
            // infoProvided should be invoked before the this method
            assert(infoProvidedHasBeenInvokedTwice)
            theOtherMethodHasBeenInvoked = true
            if (event.testName.indexOf("My Spec must start with proper words") != -1)
              reportHadCorrectTestName = true
            event.formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "must start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "  - must start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      describe("My") {
        describe("Spec") {
          it("must start with proper words") { fail() }
        }
      }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
    assert(infoReportHadCorrectTestName)
    assert(infoReportHadCorrectSpecText)
    assert(infoReportHadCorrectFormattedSpecText)
  }

  // Test for good strings in report for top-level shared behavior examples
  test("Top-level 'shared behavior - fancy specifiers' should yield good strings in a TestSucceeded report") {
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            if (testName.indexOf("it should start with proper words") != -1)
              reportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "it should start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "- it should start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec {
      def myBehavior(i: Int) {
        it("it should start with proper words") {}
      }
      it should behave like myBehavior(1)
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
  }
  
  test("Top-level 'shared behavior - plain-old specifiers' should yield good strings in a TestSucceeded report") {
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            if (testName.indexOf("must start with proper words") != -1)
              reportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "must start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "- must start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec {
      def myBehavior(i: Int) {
        it("must start with proper words") {}
      }
      it should behave like myBehavior(1)
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
  }

  test("Top-level 'shared behavior - plain-old specifiers' should yield good strings in a testSucceeded report") {
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            if (testName.indexOf("must start with proper words") != -1)
              reportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "must start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "- must start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec {
      def myBehavior(i: Int) {
        it("must start with proper words") {}
      }
      it should behave like myBehavior(1)
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
  }

  test("Top-level 'shared behavior - plain-old specifiers' should yield good strings in a TestFailed report") {
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case event: TestFailed =>
            if (event.testName.indexOf("must start with proper words") != -1)
              reportHadCorrectTestName = true
            event.formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "must start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "- must start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec {
      def myBehavior(i: Int) {
        it("must start with proper words") { fail() }
      }
      it should behave like myBehavior(1)
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
  }

  // Tests for good strings in report for shared-behavior, nested-one-level specifiers
  test("Nested-one-level 'shared behavior' should yield good strings in a TestSucceeded report") {
    var infoReportHadCorrectTestName = false
    var infoReportHadCorrectSpecText = false
    var infoReportHadCorrectFormattedSpecText = false
    var reportHadCorrectTestName = false
    var reportHadCorrectSpecText = false
    var reportHadCorrectFormattedSpecText = false
    var infoProvidedHasBeenInvoked = false
    var theOtherMethodHasBeenInvoked = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case InfoProvided(ordinal, message, nameInfo, aboutAPendingTest, throwable, formatter, payload, threadName, timeStamp) =>
            // infoProvided should be invoked before the other method
            assert(!theOtherMethodHasBeenInvoked)
            infoProvidedHasBeenInvoked = true
            if (message.indexOf("My Spec") != -1)
              infoReportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "My Spec")
                  infoReportHadCorrectSpecText = true
                if (formattedText == "My Spec")
                  infoReportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            // infoProvided should be invoked before the this method
            assert(infoProvidedHasBeenInvoked)
            theOtherMethodHasBeenInvoked = true
            if (testName.indexOf("My Spec should start with proper words") != -1)
              reportHadCorrectTestName = true
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "should start with proper words")
                  reportHadCorrectSpecText = true
                if (formattedText == "- should start with proper words")
                  reportHadCorrectFormattedSpecText = true
              case _ =>
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec {
      def myBehavior(i: Int) {
        it("should start with proper words") {}
      }
      describe("My Spec") {
        it should behave like myBehavior(1)
      }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(reportHadCorrectTestName)
    assert(reportHadCorrectSpecText)
    assert(reportHadCorrectFormattedSpecText)
    assert(infoReportHadCorrectTestName)
    assert(infoReportHadCorrectSpecText)
    assert(infoReportHadCorrectFormattedSpecText)
  }

  // Huh? what was I testing here?
  test("An empty describe shouldn't throw an exception") {
    class MySpec extends Spec with ShouldMatchers {
      describe("this will be empty") {}
    }
    val a = new MySpec
    a.execute()
  }  
  
  test("Only a passed test name should be invoked.") {
    var correctTestWasInvoked = false
    var wrongTestWasInvoked = false
    class MySpec extends Spec with ShouldMatchers {
      it("it should be invoked") {
        correctTestWasInvoked = true
      }
      it("it should not be invoked") {
        wrongTestWasInvoked = true
      }
    }
    val a = new MySpec
    a.run(Some("it should be invoked"), StubReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(correctTestWasInvoked)
    assert(!wrongTestWasInvoked)
  }
  
  test("Config map should make it through to runTest") {
    var foundMyGoodie = false
    class MySpec extends Spec with ShouldMatchers {
      override def runTest(testName: String, reporter: Reporter, stopper: Stopper, config: Map[String, Any], tracker: Tracker) {
        foundMyGoodie = config.contains("my goodie")
        super.runTest(testName, reporter, stopper, config, tracker)
      }
      it("it should find my goodie") {}
    }
    val a = new MySpec
    a.run(None, StubReporter, new Stopper {}, Filter(), Map("my goodie" -> "hi"), None, new Tracker)
    assert(foundMyGoodie)  
  }
  
  // I think delete this one. Repeat.
  test("In a TestSucceeded report, the example name should start with '<description> should' if nested two levels inside describe clauses") {
    var testSucceededReportHadCorrectTestName = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            if (testName.indexOf("A Stack (when working right) should push and pop properly") != -1) {
              testSucceededReportHadCorrectTestName = true
            }  
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      describe("A Stack") {
        describe("(when working right)") {
          it("should push and pop properly") {}
        }
      }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testSucceededReportHadCorrectTestName)
  }
  
  test("expectedTestCount is the number of plain-old specifiers if no shares") {
    class MySpec extends Spec with ShouldMatchers {
      it("must one") {}
      it("must two") {}
      describe("behavior") {
        it("must three") {}  
        it("must four") {}
      }
      it("must five") {}
    }
    val a = new MySpec
    assert(a.expectedTestCount(Filter()) === 5)
  }

  // Testing strings sent in reports
  test("In a TestSucceeded report, the example name should be verbatim if top level if example registered with it") {
    var testSucceededReportHadCorrectTestName = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            if (testName.indexOf("this thing must start with proper words") != -1) {
              testSucceededReportHadCorrectTestName = true
            }  
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      it("this thing must start with proper words") {}
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testSucceededReportHadCorrectTestName)
  }

  test("In a testSucceeded report, the example name should be verbatim if top level if example registered with it") {
    var testSucceededReportHadCorrectTestName = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            if (testName.indexOf("this thing must start with proper words") != -1) {
              testSucceededReportHadCorrectTestName = true
            }  
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      it("this thing must start with proper words") {}
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testSucceededReportHadCorrectTestName)
  }

  test("In a TestFailed report, the example name should be verbatim if top level if example registered with it") {
    var testFailedReportHadCorrectTestName = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case event: TestFailed =>
            if (event.testName.indexOf("this thing must start with proper words") != -1)
              testFailedReportHadCorrectTestName = true
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      it("this thing must start with proper words") { fail() }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testFailedReportHadCorrectTestName)
  }
  
  test("In a TestStarting report, the example name should start with '<description> ' if nested one level " +
        "inside a describe clause and registered with it") {
    var testSucceededReportHadCorrectTestName = false
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case TestStarting(_, _, _, testName, _, _, _, _, _) =>
            if (testName == "A Stack needs to push and pop properly") {
              testSucceededReportHadCorrectTestName = true
            }
          case _ => 
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      describe("A Stack") {
        it("needs to push and pop properly") {}
      }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testSucceededReportHadCorrectTestName)
  }
    
  test("Specs should send defined formatters") {
    class MyReporter extends Reporter {

      var gotAnUndefinedFormatter = false
      var lastEventWithUndefinedFormatter: Option[Event] = None

      private def ensureFormatterIsDefined(event: Event) {
        if (!event.formatter.isDefined) {
          gotAnUndefinedFormatter = true
          lastEventWithUndefinedFormatter = Some(event)
        }
      }

      def apply(event: Event) {
        event match {
          case event: RunAborted => ensureFormatterIsDefined(event)
          case event: SuiteAborted => ensureFormatterIsDefined(event)
          case event: SuiteStarting => ensureFormatterIsDefined(event)
          case event: SuiteCompleted => ensureFormatterIsDefined(event)
          case event: TestStarting => ensureFormatterIsDefined(event)
          case event: TestSucceeded => ensureFormatterIsDefined(event)
          case event: TestIgnored => ensureFormatterIsDefined(event)
          case event: TestFailed => ensureFormatterIsDefined(event)
          case event: InfoProvided => ensureFormatterIsDefined(event)
          case _ =>
        }
      }
    }

    class MySpec extends Spec with ShouldMatchers {
      it("it should send defined formatters") {
        assert(true)
      }
      it("it should also send defined formatters") {
        assert(false)
      }
    }
    val a = new MySpec
    val myRep = new MyReporter
    a.run(None, myRep, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(!myRep.gotAnUndefinedFormatter, myRep.lastEventWithUndefinedFormatter.toString)
  }

  test("SpecText should come through correctly in a SpecReport when registering with it") {
    var testSucceededReportHadCorrectSpecText = false
    var lastSpecText: Option[String] = None
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "My spec text must have the proper words")
                  testSucceededReportHadCorrectSpecText = true
                else
                  lastSpecText = Some(rawText)
              case _ => throw new RuntimeException("Got a non-SpecReport")
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      it("My spec text must have the proper words") {}
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testSucceededReportHadCorrectSpecText, lastSpecText match { case Some(s) => s; case None => "No report"})
  }

  test("Spec text should come through correctly in a SpecReport when registering with it when nested in one describe") {
    var testSucceededReportHadCorrectSpecText = false
    var lastSpecText: Option[String] = None
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "My short name must have the proper words")
                  testSucceededReportHadCorrectSpecText = true
                else
                  lastSpecText = Some(rawText)
              case _ => throw new RuntimeException("Got a non-SpecReport")
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      describe("A Stack") {
        it("My short name must have the proper words") {}
      }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testSucceededReportHadCorrectSpecText, lastSpecText match { case Some(s) => s; case None => "No report"})
  }

  test("Spec text should come through correctly in a SpecReport when registering with it when nested in two describes") {
    var testSucceededReportHadCorrectSpecText = false
    var lastSpecText: Option[String] = None
    class MyReporter extends Reporter {
      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                if (rawText == "My short name must have the proper words")
                  testSucceededReportHadCorrectSpecText = true
                else
                  lastSpecText = Some(rawText)
              case _ => throw new RuntimeException("Got a non-SpecReport")
            }
          case _ =>
        }
      }
    }
    class MySpec extends Spec with ShouldMatchers {
      describe("A Stack") {
        describe("(when empty)") {
          it("My short name must have the proper words") {}
        }
      }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testSucceededReportHadCorrectSpecText, lastSpecText match { case Some(s) => s; case None => "No report"})
  }

  test("Should get infoProvided with description if one and only one describe clause") {

    val expectedSpecText = "A Stack"

    class MyReporter extends Reporter {
      var infoProvidedCalled = false
      var expectedMessageReceived = false
      def apply(event: Event) {
        event match {
          case event: InfoProvided =>
            event.formatter match {
              case Some(IndentedText(formattedText, rawText, indentationLevel)) =>
                infoProvidedCalled = true
                if (!expectedMessageReceived) {
                  expectedMessageReceived = (rawText == expectedSpecText)
                }
              case _ =>
            }
          case _ =>
        }
      }
    }

    class MySpec extends Spec with ShouldMatchers {
      describe("A Stack") {
        it("should allow me to push") {}
      }
    }
    
    val a = new MySpec
    val myRep = new MyReporter
    a.run(None, myRep, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(myRep.infoProvidedCalled)
    assert(myRep.expectedMessageReceived)
  }
 
  // Testing Shared behaviors
  test("a shared specifier invoked with 'should behave like a' should get invoked") {
    class MySpec extends Spec with BeforeAndAfterEach with BeforeAndAfterAll {
      var sharedExampleInvoked = false
      def invocationVerifier(i: Int) {
        it("should be invoked") {
          sharedExampleInvoked = true
        }
      }
      describe("A Stack") {
        describe("(when not empty)") {
          it("should allow me to pop") {}
          it should behave like invocationVerifier(1) 
        }
        describe("(when not full)") {
          it("should allow me to push") {}
        }
      }
    }
    val a = new MySpec
    a.execute()
    assert(a.sharedExampleInvoked)
  }
  
  test("two examples in a shared behavior should get invoked") {
    class MySpec extends Spec with BeforeAndAfterEach with BeforeAndAfterAll {
      var sharedExampleInvoked = false
      var sharedExampleAlsoInvoked = false
      def invocationVerifier(i: Int) {
        it("should be invoked") {
          sharedExampleInvoked = true
        }
        it("should also be invoked") {
          sharedExampleAlsoInvoked = true
        }
      }
      describe("A Stack") {
        describe("(when not empty)") {
          it("should allow me to pop") {}
          it should behave like invocationVerifier(1)
        }
        describe("(when not full)") {
          it("should allow me to push") {}
        }
      }
    }
    val a = new MySpec
    a.execute()
    assert(a.sharedExampleInvoked)
    assert(a.sharedExampleAlsoInvoked)
  }

  test("three examples in a shared behavior should be invoked in order") {
    class MySpec extends Spec {
      var example1WasInvoked = false
      var example2WasInvokedAfterExample1 = false
      var example3WasInvokedAfterExample2 = false
      def invocationVerifier(i: Int) {
        it("should get invoked") {
          example1WasInvoked = true
        }
        it("should also get invoked") {
          if (example1WasInvoked)
            example2WasInvokedAfterExample1 = true
        }
        it("should also also get invoked") {
          if (example2WasInvokedAfterExample1)
            example3WasInvokedAfterExample2 = true
        }
      }
      it should behave like invocationVerifier(1)
    }
    val a = new MySpec
    a.execute()
    assert(a.example1WasInvoked)
    assert(a.example2WasInvokedAfterExample1)
    assert(a.example3WasInvokedAfterExample2)
  }
  
  test("three examples in a shared behavior should not get invoked at all if the behavior isn't used in a like clause") {
    class MySpec extends Spec with ShouldMatchers {
      var example1WasInvoked = false
      var example2WasInvokedAfterExample1 = false
      var example3WasInvokedAfterExample2 = false
      def invocationVerifier(i: Int) {
        it("should get invoked") {
          example1WasInvoked = true
        }
        it("should also get invoked") {
          if (example1WasInvoked)
            example2WasInvokedAfterExample1 = true
        }
        it("should also also get invoked") {
          if (example2WasInvokedAfterExample1)
            example3WasInvokedAfterExample2 = true
        }
      }
      // don't use it: behaves like (an InvocationVerifier())
    }
    val a = new MySpec
    a.execute()
    assert(!a.example1WasInvoked)
    assert(!a.example2WasInvokedAfterExample1)
    assert(!a.example3WasInvokedAfterExample2)
  }
  
  // Probably delete
  test("The test name for a shared specifier invoked with 'should behave like a' should be verbatim if top level") {
    var testSucceededReportHadCorrectTestName = false
    class MyReporter extends Reporter {

      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            if (testName.indexOf("it should be invoked") != -1) {
              testSucceededReportHadCorrectTestName = true
            }  
          case _ =>
        }
      }
    }
    class MySpec extends Spec with BeforeAndAfterEach with BeforeAndAfterAll {
      var sharedExampleInvoked = false
      def invocationVerifier(i: Int) {
        it("it should be invoked") {
          sharedExampleInvoked = true
        }
      }
      it should behave like invocationVerifier(1) 
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testSucceededReportHadCorrectTestName)
  }
  
  ignore("The example name for a shared example invoked with 'it should behave like' should start with '<description> should' if nested one level in a describe clause") {
    var testSucceededReportHadCorrectTestName = false
    class MyReporter extends Reporter {

      def apply(event: Event) {
        event match {
          case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
            if (testName.indexOf("A Stack should pop properly") != -1) {
              testSucceededReportHadCorrectTestName = true
            }  
          case _ =>
        }
      }
    }
    class MySpec extends Spec {
      var sharedExampleInvoked = false
      def invocationVerifier(i: Int) {
        it("should pop properly") {
          sharedExampleInvoked = true
        }
      }
      describe("A Stack") {
        it should behave like invocationVerifier(1) 
      }
    }
    val a = new MySpec
    a.run(None, new MyReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(testSucceededReportHadCorrectTestName)
  }
 
  test("expectedTestCount should not include tests in shares if never called") {
    class MySpec extends Spec with ShouldMatchers {
      class Misbehavior extends Spec with ShouldMatchers {
        it("should six") {}
        it("should seven") {}
      }
      it("should one") {}
      it("should two") {}
      describe("behavior") {
        it("should three") {}
        it("should four") {}
      }
      it("should five") {}
    }
    val a = new MySpec
    assert(a.expectedTestCount(Filter()) === 5)
  }

  test("expectedTestCount should include tests in a share that is called") {
    class MySpec extends Spec {
      def misbehavior(i: Int) {
        it("should six") {}
        it("should seven") {}
      }
      it("should one") {}
      it("should two") {}
      describe("behavior") {
        it("should three") {}
        it should behave like misbehavior(1) 
        it("should four") {}
      }
      it("should five") {}
    }
    val a = new MySpec
    assert(a.expectedTestCount(Filter()) === 7)
  }

  test("expectedTestCount should include tests in a share that is called twice") {
    class MySpec extends Spec {
      def misbehavior(i: Int) {
        it("should six") {}
        it("should seven") {}
      }
      it("should one") {}
      it("should two") {}
      describe("behavior") {
        it("should three") {}
        it should behave like misbehavior(1) 
        it("should four") {}
      }
      it("should five") {}
      it should behave like misbehavior(1) 
    }
    val a = new MySpec
    assert(a.expectedTestCount(Filter()) === 9)
  }

  test("Spec's expectedTestCount includes tests in nested suites") {
    class TwoTestSpec extends Spec {
      it("should count this test") {}
      it("should count this test also") {}
    }
    class MySpec extends Spec {

      override def nestedSuites = List(new TwoTestSpec, new TwoTestSpec, new TwoTestSpec)

      it("should count this here test") {}
    }
    val mySpec = new MySpec
    assert(mySpec.expectedTestCount(Filter()) === 7)
  }

  // End of Share stuff
  ignore("should be able to send info to the reporter") { // Can't do this yet, no info in Spec yet

    val expectedMessage = "this is the expected message"

    class MyReporter extends Reporter {
      var infoProvidedCalled = false
      var expectedMessageReceived = false

      def apply(event: Event) {
        event match {
          case event: InfoProvided =>
            infoProvidedCalled = true
            if (!expectedMessageReceived) {
              expectedMessageReceived = event.message.indexOf(expectedMessage) != -1
            }
          case _ =>
        }
      }
    }

    class MySpec extends Spec with ShouldMatchers {
      describe("A Stack") {
        describe("(when not empty)") {
          it("should allow me to pop") {
            info(expectedMessage)
            ()
          }
        }
        describe("(when not full)") {
          it("should allow me to push") {}
        }
      }
    }
    val a = new MySpec
    val myRep = new MyReporter
    a.run(None, myRep, new Stopper {}, Filter(), Map(), None, new Tracker)
    assert(myRep.infoProvidedCalled)
    assert(myRep.expectedMessageReceived)
  }

  test("that a null specText results in a thrown NPE at construction time") {
    intercept[NullPointerException] {
      new Spec {
        it(null) {}
      }
    }
    intercept[NullPointerException] {
      new Spec {
        ignore(null) {}
      }
    }
  }

  test("test durations are included in TestFailed and TestSucceeded events fired from Spec") {

    class MySpec extends Spec {
      it("should succeed") {}
      it("should fail") { fail() }
    }

    val mySpec = new MySpec
    val myReporter = new TestDurationReporter
    mySpec.run(None, myReporter, new Stopper {}, Filter(), Map(), None, new Tracker(new Ordinal(99)))
    assert(myReporter.testSucceededWasFiredAndHadADuration)
    assert(myReporter.testFailedWasFiredAndHadADuration)
  }

  test("suite durations are included in SuiteCompleted events fired from Spec") {

    class MySpec extends Spec {
      override def nestedSuites = List(new Suite {})
    }

    val mySuite = new MySpec
    val myReporter = new SuiteDurationReporter
    mySuite.run(None, myReporter, new Stopper {}, Filter(), Map(), None, new Tracker(new Ordinal(99)))
    assert(myReporter.suiteCompletedWasFiredAndHadADuration)
  }

  test("suite durations are included in SuiteAborted events fired from Spec") {

    class SuiteThatAborts extends Suite {
      override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
              config: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
        throw new RuntimeException("Aborting for testing purposes")
      }
    }

    class MySpec extends Spec {
      override def nestedSuites = List(new SuiteThatAborts {})
    }

    val mySuite = new MySpec
    val myReporter = new SuiteDurationReporter
    mySuite.run(None, myReporter, new Stopper {}, Filter(), Map(), None, new Tracker(new Ordinal(99)))
    assert(myReporter.suiteAbortedWasFiredAndHadADuration)
  }

  test("pending in a Spec should cause TestPending to be fired") {

    class MySpec extends Spec {
      it("should be pending") (pending)
    }

    val mySuite = new MySpec
    val myReporter = new PendingReporter
    mySuite.run(None, myReporter, new Stopper {}, Filter(), Map(), None, new Tracker(new Ordinal(99)))
    assert(myReporter.testPendingWasFired)
  }
}

