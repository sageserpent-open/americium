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
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.Iterator
import java.util.Set
import java.io.StringWriter
import org.scalatest.events._
import PrintReporter._

/**
 * A <code>Reporter</code> that prints test status information in HTML format to a file.
 */
private[scalatest] class HtmlReporter(pw: PrintWriter, presentAllDurations: Boolean,
        presentInColor: Boolean, presentStackTraces: Boolean, presentFullStackTraces: Boolean) extends ResourcefulReporter {

  /**
  * Construct a <code>PrintReporter</code> with passed
  * <code>String</code> file name. Information about events reported to instances of this
  * class will be written to the specified file using the
  * default character encoding.
  *
  * @param filename the <code>String</code> name of the file to which to print reported info
  * @throws NullPointerException if passed <code>filename</code> reference is <code>null</code>
  * @throws IOException if unable to open the specified file for writing
  */
  def this(
    filename: String,
    presentAllDurations: Boolean,
    presentInColor: Boolean,
    presentShortStackTraces: Boolean,
    presentFullStackTraces: Boolean
  ) =
    this(
      new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(filename)), BufferSize)),
      presentAllDurations,
      presentInColor,
      presentShortStackTraces,
      presentFullStackTraces
    )

  private def withPossibleLineNumber(stringToPrint: String, throwable: Option[Throwable]): String = {
    throwable match {
      case Some(testFailedException: TestFailedException) =>
        testFailedException.failedCodeFileNameAndLineNumberString match {
          case Some(lineNumberString) =>
            Resources("printedReportPlusLineNumber", stringToPrint, lineNumberString)
          case None => stringToPrint
        }
      case _ => stringToPrint
    }
  }

  private def printPossiblyInColor(text: String, ansiColor: String) {
    pw.println(if (presentInColor) ansiColor + text + ansiReset else text)
  }

  // Called for TestFailed, InfoProvided (because it can have a throwable in it), and SuiteAborted
  private def stringsToPrintOnError(noteResourceName: String, errorResourceName: String, message: String, throwable: Option[Throwable],
    formatter: Option[Formatter], suiteName: Option[String], testName: Option[String], duration: Option[Long]): List[String] = {

    val stringToPrint =
      formatter match {
        case Some(IndentedText(formattedText, _, _)) =>
          Resources("specTextAndNote", formattedText, Resources(noteResourceName))
        case _ =>
          // Deny MotionToSuppress directives in error events, because error info needs to be seen by users
            suiteName match {
              case Some(sn) =>
                testName match {
                  case Some(tn) => Resources(errorResourceName, sn + ": " + tn)
                  case None => Resources(errorResourceName, sn)
                }
              // Should not get here with built-in ScalaTest stuff, but custom stuff could get here.
              case None => Resources(errorResourceName, Resources("noNameSpecified"))
            }
    }

    val stringToPrintWithPossibleLineNumber = withPossibleLineNumber(stringToPrint, throwable)

    val stringToPrintWithPossibleLineNumberAndDuration =
      duration match {
        case Some(milliseconds) =>
          if (presentAllDurations)
            Resources("withDuration", stringToPrintWithPossibleLineNumber, makeDurationString(milliseconds))
          else
            stringToPrintWithPossibleLineNumber
        case None => stringToPrintWithPossibleLineNumber
      }

    // If there's a message, put it on the next line, indented two spaces, unless this is an IndentedText
    val possiblyEmptyMessage =
      formatter match {
        case Some(IndentedText(_, _, _)) => ""
        case _ =>
          Reporter.messageOrThrowablesDetailMessage(message, throwable)
      }

    // I don't want to put a second line out there if the event's message contains the throwable's message,
    // or if niether the event message or throwable message has any message in it.
    val throwableIsATestFailedExceptionWithRedundantMessage =
      throwable match {
        case Some(t) =>
          t.isInstanceOf[TestFailedException] && ((t.getMessage != null &&
          !t.getMessage.trim.isEmpty && possiblyEmptyMessage.indexOf(t.getMessage.trim) != -1) || // This part is where a throwable message exists
          (possiblyEmptyMessage.isEmpty && (t.getMessage == null || t.getMessage.trim.isEmpty))) // This part detects when both have no message
        case None => false
      }

    def getStackTrace(throwable: Option[Throwable]): List[String] =
      throwable match {
        case Some(throwable) =>

          def useConciseTestFailedExceptionForm =
            !presentFullStackTraces && (
              throwable match {
                case tfe: TestFailedException => tfe.cause.isEmpty // If there's a cause inside, show the whole stack trace
                case _ => false
              }
            )

          def stackTrace(throwable: Throwable, isCause: Boolean): List[String] = {
            val className = throwable.getClass.getName 
            val labeledClassName = if (isCause) Resources("DetailsCause") + ": " + className else className
            val labeledClassNameWithMessage =
              if (throwable.getMessage != null && !throwable.getMessage.trim.isEmpty)
                if (!useConciseTestFailedExceptionForm)
                  "  " + labeledClassName + ": " + throwable.getMessage.trim
                else
                  "  " + throwable.getMessage.trim // Don't show "org.scalatest.TestFailedException: " if no stack trace to follow
              else
                "  " + labeledClassName

            if (!useConciseTestFailedExceptionForm) {
              val stackTraceElements = throwable.getStackTrace.toList map { "  " + _.toString } // Indent each stack trace item two spaces
              val cause = throwable.getCause

              val stackTraceThisThrowable = labeledClassNameWithMessage :: stackTraceElements
              if (cause == null)
                stackTraceThisThrowable
              else
                stackTraceThisThrowable ::: stackTrace(cause, true) // Not tail recursive, but shouldn't be too deep
            }
            else List(labeledClassNameWithMessage)
          }
          if (!throwableIsATestFailedExceptionWithRedundantMessage || !useConciseTestFailedExceptionForm)
            stackTrace(throwable, false)
          else List()
        case None => List()
      }

    if (possiblyEmptyMessage.isEmpty)
      stringToPrintWithPossibleLineNumberAndDuration :: getStackTrace(throwable)
    else
      stringToPrintWithPossibleLineNumberAndDuration :: "  " + possiblyEmptyMessage :: getStackTrace(throwable)
  }

  private def stringToPrintWhenNoError(resourceName: String, formatter: Option[Formatter], suiteName: String, testName: Option[String]): Option[String] =
    stringToPrintWhenNoError(resourceName, formatter, suiteName, testName, None)

  private def stringToPrintWhenNoError(resourceName: String, formatter: Option[Formatter], suiteName: String, testName: Option[String], duration: Option[Long]): Option[String] = {

    formatter match {
      case Some(IndentedText(formattedText, _, _)) =>
        duration match {
          case Some(milliseconds) =>
            if (presentAllDurations)
              Some(Resources("withDuration", formattedText, makeDurationString(milliseconds)))
            else
              Some(formattedText)
          case None => Some(formattedText)
        }
      case Some(MotionToSuppress) => None
      case _ =>
        val arg =
          testName match {
            case Some(tn) => suiteName + ": " + tn
            case None => suiteName
          }
        val unformattedText = Resources(resourceName, arg)
        duration match {
          case Some(milliseconds) =>
            if (presentAllDurations)
              Some(Resources("withDuration", unformattedText, makeDurationString(milliseconds)))
            else
              Some(unformattedText)
          case None => Some(unformattedText)
        }

    }
  }

  def apply(event: Event) {

    event match {

      case RunStarting(ordinal, testCount, configMap, formatter, payload, threadName, timeStamp) => 

        if (testCount < 0)
          throw new IllegalArgumentException
  
        val string = Resources("runStarting", testCount.toString)
        printPossiblyInColor(string, ansiCyan)

      case RunCompleted(ordinal, duration, summary, formatter, payload, threadName, timeStamp) => 

        makeFinalReport("runCompleted", duration, summary)

      case RunStopped(ordinal, duration, summary, formatter, payload, threadName, timeStamp) =>

        makeFinalReport("runStopped", duration, summary)

      case RunAborted(ordinal, message, throwable, duration, summary, formatter, payload, threadName, timeStamp) => 

        val lines = stringsToPrintOnError("abortedNote", "runAborted", message, throwable, formatter, None, None, duration)
        for (line <- lines) printPossiblyInColor(line, ansiRed)

      case SuiteStarting(ordinal, suiteName, suiteClassName, formatter, rerunnable, payload, threadName, timeStamp) =>

        val stringToPrint = stringToPrintWhenNoError("suiteStarting", formatter, suiteName, None)

        stringToPrint match {
          case Some(string) => printPossiblyInColor(string, ansiGreen)
          case None =>
        }

      case SuiteCompleted(ordinal, suiteName, suiteClassName, duration, formatter, rerunnable, payload, threadName, timeStamp) => 

        val stringToPrint = stringToPrintWhenNoError("suiteCompleted", formatter, suiteName, None, duration)

        stringToPrint match {
          case Some(string) => printPossiblyInColor(string, ansiGreen)
          case None =>
        }

      case SuiteAborted(ordinal, message, suiteName, suiteClassName, throwable, duration, formatter, rerunnable, payload, threadName, timeStamp) => 

        val lines = stringsToPrintOnError("abortedNote", "suiteAborted", message, throwable, formatter, Some(suiteName), None, duration)
        for (line <- lines) printPossiblyInColor(line, ansiRed)

      case TestStarting(ordinal, suiteName, suiteClassName, testName, formatter, rerunnable, payload, threadName, timeStamp) =>

        val stringToPrint = stringToPrintWhenNoError("testStarting", formatter, suiteName, Some(testName))

        stringToPrint match {
          case Some(string) => printPossiblyInColor(string, ansiGreen)
          case None =>
        }

      case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) => 

        val stringToPrint = stringToPrintWhenNoError("testSucceeded", formatter, suiteName, Some(testName), duration)

        stringToPrint match {
          case Some(string) => printPossiblyInColor(string, ansiGreen)
          case None =>
        }
    
      case TestIgnored(ordinal, suiteName, suiteClassName, testName, formatter, payload, threadName, timeStamp) => 

        val stringToPrint =
          formatter match {
            case Some(IndentedText(formattedText, _, _)) => Some(Resources("specTextAndNote", formattedText, Resources("ignoredNote")))
            case Some(MotionToSuppress) => None
            case _ => Some(Resources("testIgnored", suiteName + ": " + testName))
          }
 
        stringToPrint match {
          case Some(string) => printPossiblyInColor(string, ansiYellow)
          case None =>
        }

      case TestFailed(ordinal, message, suiteName, suiteClassName, testName, throwable, duration, formatter, rerunnable, payload, threadName, timeStamp) => 

        val lines = stringsToPrintOnError("failedNote", "testFailed", message, throwable, formatter, Some(suiteName), Some(testName), duration)
        for (line <- lines) printPossiblyInColor(line, ansiRed)

      case InfoProvided(ordinal, message, nameInfo, aboutAPendingTest, throwable, formatter, payload, threadName, timeStamp) =>

        val (suiteName, testName) =
          nameInfo match {
            case Some(NameInfo(suiteName, _, testName)) => (Some(suiteName), testName)
            case None => (None, None)
          }
        val lines = stringsToPrintOnError("infoProvidedNote", "infoProvided", message, throwable, formatter, suiteName, testName, None)
        val shouldBeYellow =
          aboutAPendingTest match {
            case Some(isPending) => isPending
            case None => false
          }
        for (line <- lines) printPossiblyInColor(line, if (shouldBeYellow) ansiYellow else ansiGreen)

      case TestPending(ordinal, suiteName, suiteClassName, testName, formatter, payload, threadName, timeStamp) =>

        val stringToPrint =
          formatter match {
            case Some(IndentedText(formattedText, _, _)) => Some(Resources("specTextAndNote", formattedText, Resources("pendingNote")))
            case Some(MotionToSuppress) => None
            case _ => Some(Resources("testPending", suiteName + ": " + testName))
          }

        stringToPrint match {
          case Some(string) => printPossiblyInColor(string, ansiYellow)
          case None =>
        }

     // case _ => throw new RuntimeException("Unhandled event")
    }

    pw.flush()
  }

  // Closes the print writer. Subclasses StandardOutReporter and StandardErrReporter override dispose to do nothing
  // so that those aren't closed.
  override def dispose() {
    pw.close()
  }

  private def printResourceString(resourceName: String) {

    pw.println(Resources(resourceName))
    pw.flush()
  }

  private def makeFinalReport(resourceName: String, duration: Option[Long], summaryOption: Option[Summary]) {

    summaryOption match {
      case Some(summary) =>

        import summary._

        duration match {
          case Some(msSinceEpoch) =>
            printPossiblyInColor(Resources(resourceName + "In", makeDurationString(msSinceEpoch)), ansiCyan)
          case None =>
            printPossiblyInColor(Resources(resourceName), ansiCyan)
        }     

        // totalNumberOfTestsRun=Total number of tests run was: {0}
        printPossiblyInColor(Resources("totalNumberOfTestsRun", testsCompletedCount.toString), ansiCyan)

        // Suite Summary: completed {0}, aborted {1}
        printPossiblyInColor(Resources("suiteSummary", suitesCompletedCount.toString, suitesAbortedCount.toString), ansiCyan)

        // Test Summary: succeeded {0}, failed {1}, ignored, {2}, pending {3}
        printPossiblyInColor(Resources("testSummary", testsSucceededCount.toString, testsFailedCount.toString, testsIgnoredCount.toString, testsPendingCount.toString), ansiCyan)

        // *** 1 SUITE ABORTED ***
        if (suitesAbortedCount == 1)
          printPossiblyInColor(Resources("oneSuiteAborted"), ansiRed)

        // *** {0} SUITES ABORTED ***
        else if (suitesAbortedCount > 1)
          printPossiblyInColor(Resources("multipleSuitesAborted", suitesAbortedCount.toString), ansiRed)

        // *** 1 TEST FAILED ***
        if (testsFailedCount == 1)
          printPossiblyInColor(Resources("oneTestFailed"), ansiRed)

        // *** {0} TESTS FAILED ***
        else if (testsFailedCount > 1)
          printPossiblyInColor(Resources("multipleTestsFailed", testsFailedCount.toString), ansiRed)

        else if (suitesAbortedCount == 0) // Maybe don't want to say this if the run aborted or stopped because "all"
          printPossiblyInColor(Resources("allTestsPassed"), ansiGreen)

      case None =>
    }

    pw.flush()

    println("THAT'S ALL FOLKS!")
  }

  // We subtract one from test reports because we add "- " in front, so if one is actually zero, it will come here as -1
  // private def indent(s: String, times: Int) = if (times <= 0) s else ("  " * times) + s

  // Stupid properties file won't let me put spaces at the beginning of a property
  // "  {0}" comes out as "{0}", so I can't do indenting in a localizable way. For now
  // just indent two space to the left.  //  if (times <= 0) s 
  //  else Resources("indentOnce", indent(s, times - 1))
}
