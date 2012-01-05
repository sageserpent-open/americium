/*
 * Copyright 2001-2011 Artima, Inc.
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

import java.util.concurrent.atomic.AtomicReference
import java.util.ConcurrentModificationException
import org.scalatest.StackDepthExceptionHelper.getStackDepth
import FunSuite.IgnoreTagName 
import org.scalatest.NodeFamily.TestLeaf
import org.scalatest.Suite._
import fixture.NoArgTestWrapper

// T will be () => Unit for FunSuite and FixtureParam => Any for FixtureFunSuite
private[scalatest] sealed abstract class SuperEngine[T](concurrentBundleModResourceName: String, simpleClassName: String)  {

  sealed abstract class Node(val parentOption: Option[Branch]) {
    def indentationLevel: Int = {
      def calcLevel(currentParentOpt: Option[Branch], currentLevel: Int): Int = 
        currentParentOpt match {
          case None => currentLevel
          case Some(parent) => calcLevel(parent.parentOption, currentLevel + 1)
        }
      val level = calcLevel(parentOption, -1)
      if (level < 0) 0 else level
    }
  }

  abstract class Branch(parentOption: Option[Branch]) extends Node(parentOption) {
    var subNodes: List[Node] = Nil
  }

  case object Trunk extends Branch(None)

  case class TestLeaf(
    parent: Branch,
    testName: String, // The full test name
    testText: String, // The last portion of the test name that showed up on an inner most nested level
    testFun: T
  ) extends Node(Some(parent))

  case class InfoLeaf(parent: Branch, message: String) extends Node(Some(parent))

  case class DescriptionBranch(
    parent: Branch,
    descriptionText: String,
    childPrefix: Option[String] // If defined, put it at the beginning of any child descriptionText or testText 
  ) extends Branch(Some(parent))   

  // Access to the testNamesList, testsMap, and tagsMap must be synchronized, because the test methods are invoked by
  // the primary constructor, but testNames, tags, and runTest get invoked directly or indirectly
  // by run. When running tests concurrently with ScalaTest Runner, different threads can
  // instantiate and run the suite. Instead of synchronizing, I put them in an immutable Bundle object (and
  // all three collections--testNamesList, testsMap, and tagsMap--are immuable collections), then I put the Bundle
  // in an AtomicReference. Since the expected use case is the test method will be called
  // from the primary constructor, which will be all done by one thread, I just in effect use optimistic locking on the Bundle.
  // If two threads ever called test at the same time, they could get a ConcurrentModificationException.
  // Test names are in reverse order of test registration method invocations
  class Bundle private(
    val currentBranch: Branch,
    val testNamesList: List[String],
    val testsMap: Map[String, TestLeaf],
    val tagsMap: Map[String, Set[String]],
    val registrationClosed: Boolean
  ) {
    def unpack = (currentBranch, testNamesList, testsMap, tagsMap, registrationClosed)
  }

  object Bundle {
    def apply(
      currentBranch: Branch,
      testNamesList: List[String],
      testsMap: Map[String, TestLeaf],
      tagsMap: Map[String, Set[String]],
      registrationClosed: Boolean
    ): Bundle =
      new Bundle(currentBranch, testNamesList, testsMap, tagsMap, registrationClosed)
  }

  final val atomic = new AtomicReference[Bundle](Bundle(Trunk, List(), Map(), Map(), false))

  def updateAtomic(oldBundle: Bundle, newBundle: Bundle) {
    val shouldBeOldBundle = atomic.getAndSet(newBundle)
    if (!(shouldBeOldBundle eq oldBundle))
      throw new ConcurrentModificationException(Resources(concurrentBundleModResourceName))
  }

  class RegistrationInformer extends Informer {
    def apply(message: String) {
      if (message == null)
        throw new NullPointerException
      val oldBundle = atomic.get
      var (currentBranch, testNamesList, testsMap, tagsMap, registrationClosed) = oldBundle.unpack
      currentBranch.subNodes ::= InfoLeaf(currentBranch, message)
      updateAtomic(oldBundle, Bundle(currentBranch, testNamesList, testsMap, tagsMap, registrationClosed))
    }
  }

  // The informer will be a registration informer until run is called for the first time. (This
  // is the registration phase of a FunSuite's lifecycle.)
  final val atomicInformer = new AtomicReference[Informer](new RegistrationInformer)

  final val zombieInformer =
    new Informer {
      private val complaint = Resources("cantCallInfoNow", simpleClassName)
      def apply(message: String) {
        if (message == null)
          throw new NullPointerException
        throw new IllegalStateException(complaint)
      }
    }

  private def checkTestOrIgnoreParamsForNull(testName: String, testTags: Tag*) {
    if (testName == null)
      throw new NullPointerException("testName was null")
    if (testTags.exists(_ == null))
      throw new NullPointerException("a test tag was null")
  }

  def runTestImpl(
    theSuite: Suite,
    testName: String,
    reporter: Reporter,
    stopper: Stopper,
    configMap: Map[String, Any],
    tracker: Tracker,
    includeIcon: Boolean,
    invokeWithFixture: TestLeaf => Unit
  ) {

    checkRunTestParamsForNull(testName, reporter, stopper, configMap, tracker)

    val (stopRequested, report, hasPublicNoArgConstructor, rerunnable, testStartTime) =
      theSuite.getRunTestGoodies(stopper, reporter, testName)

    reportTestStarting(theSuite, report, tracker, testName, rerunnable)

    if (!atomic.get.testsMap.contains(testName))
      throw new IllegalArgumentException("No test in this suite has name: \"" + testName + "\"")

    val theTest = atomic.get.testsMap(testName)

    val testTextWithOptionalPrefix = prependChildPrefix(theTest.parent, theTest.testText)
    val formatter = getIndentedText(testTextWithOptionalPrefix, theTest.indentationLevel, includeIcon)

    val informerForThisTest =
      MessageRecordingInformer2(
        (message, isConstructingThread, testWasPending) => reportInfoProvided(theSuite, report, tracker, Some(testName), message, theTest.indentationLevel + 1, isConstructingThread, includeIcon, Some(testWasPending))
      )

    val oldInformer = atomicInformer.getAndSet(informerForThisTest)
    var testWasPending = false

    try {

      invokeWithFixture(theTest)

      val duration = System.currentTimeMillis - testStartTime
      reportTestSucceeded(theSuite, report, tracker, testName, duration, formatter, rerunnable)
    }
    catch { 
      case _: TestPendingException =>
        reportTestPending(theSuite, report, tracker, testName, formatter)
        testWasPending = true // Set so info's printed out in the finally clause show up yellow
      case e if !anErrorThatShouldCauseAnAbort(e) =>
        val duration = System.currentTimeMillis - testStartTime
        reportTestFailed(theSuite, report, e, testName, theTest.testText, rerunnable, tracker, duration, theTest.indentationLevel, includeIcon)
      case e => throw e
    }
    finally {
      informerForThisTest.fireRecordedMessages(testWasPending)
      val shouldBeInformerForThisTest = atomicInformer.getAndSet(oldInformer)
      val swapAndCompareSucceeded = shouldBeInformerForThisTest eq informerForThisTest
      if (!swapAndCompareSucceeded)
        throw new ConcurrentModificationException(Resources("concurrentInformerMod", theSuite.getClass.getName))
    }
  }

  private def runTestsInBranch(
    theSuite: Suite,
    branch: Branch,
    report: Reporter,
    stopRequested: Stopper,
    filter: Filter,
    configMap: Map[String, Any],
    tracker: Tracker,
    includeIcon: Boolean,
    runTest: (String, Reporter, Stopper, Map[String, Any], Tracker) => Unit
  ) {

    branch match {

      case desc @ DescriptionBranch(parent, descriptionText, _) =>

        val descriptionTextWithOptionalPrefix = prependChildPrefix(parent, descriptionText)
        val indentationLevel = desc.indentationLevel
        reportInfoProvided(theSuite, report, tracker, None, descriptionTextWithOptionalPrefix, indentationLevel, true, false)

      case Trunk =>
    }


    branch.subNodes.reverse.foreach { node =>
      if (!stopRequested()) {
        node match {
          case testLeaf @ TestLeaf(_, testName, testText, _) =>
            val (filterTest, ignoreTest) = filter(testName, theSuite.tags)
            if (!filterTest)
              if (ignoreTest) {
                val testTextWithOptionalPrefix = prependChildPrefix(branch, testText)
                reportTestIgnored(theSuite, report, tracker, testName, testTextWithOptionalPrefix, testLeaf.indentationLevel)
              }
              else
                runTest(testName, report, stopRequested, configMap, tracker)

          case infoLeaf @ InfoLeaf(_, message) =>
            reportInfoProvided(theSuite, report, tracker, None, message, infoLeaf.indentationLevel, true, includeIcon)

          case branch: Branch => runTestsInBranch(theSuite, branch, report, stopRequested, filter, configMap, tracker, includeIcon, runTest)
        }
      }
    }
  }

  def prependChildPrefix(branch: Branch, testText: String): String =
    branch match {
      case DescriptionBranch(_, _, Some(cp)) => Resources("prefixSuffix", cp, testText)
      case _ => testText
    }

  def runTestsImpl(
    theSuite: Suite,
    testName: Option[String],
    reporter: Reporter,
    stopper: Stopper,
    filter: Filter,
    configMap: Map[String, Any],
    distributor: Option[Distributor],
    tracker: Tracker,
    info: String => Unit,
    includeIcon: Boolean,
    runTest: (String, Reporter, Stopper, Map[String, Any], Tracker) => Unit
  ) {
    if (testName == null)
      throw new NullPointerException("testName was null")
    if (reporter == null)
      throw new NullPointerException("reporter was null")
    if (stopper == null)
      throw new NullPointerException("stopper was null")
    if (filter == null)
      throw new NullPointerException("filter was null")
    if (configMap == null)
      throw new NullPointerException("configMap was null")
    if (distributor == null)
      throw new NullPointerException("distributor was null")
    if (tracker == null)
      throw new NullPointerException("tracker was null")

    val stopRequested = stopper

    // Wrap any non-DispatchReporter, non-CatchReporter in a CatchReporter,
    // so that exceptions are caught and transformed
    // into error messages on the standard error stream.
    val report = theSuite.wrapReporterIfNecessary(reporter)

    // If a testName is passed to run, just run that, else run the tests returned
    // by testNames.
    testName match {
      case Some(tn) => runTest(tn, report, stopRequested, configMap, tracker)
      case None => runTestsInBranch(theSuite, Trunk, report, stopRequested, filter, configMap, tracker, includeIcon, runTest)
    }
  }

  def runImpl(
    theSuite: Suite,
    testName: Option[String],
    reporter: Reporter,
    stopper: Stopper,
    filter: Filter,
    configMap: Map[String, Any],
    distributor: Option[Distributor],
    tracker: Tracker,
    superRun: (Option[String], Reporter, Stopper, Filter, Map[String, Any], Option[Distributor], Tracker) => Unit
  ) {
    val stopRequested = stopper

    // Set the flag that indicates registration is closed (because run has now been invoked),
    // which will disallow any further invocations of "test" or "ignore" with
    // an RegistrationClosedException.    
    val oldBundle = atomic.get
    val (currentBranch, testNamesList, testsMap, tagsMap, registrationClosed) = oldBundle.unpack
    if (!registrationClosed)
      updateAtomic(oldBundle, Bundle(currentBranch, testNamesList, testsMap, tagsMap, true))

    val report = theSuite.wrapReporterIfNecessary(reporter)

    val informerForThisSuite =
      ConcurrentInformer2(
        (message, isConstructingThread) => reportInfoProvided(theSuite, report, tracker, None, message, 1, isConstructingThread)
      )

    atomicInformer.set(informerForThisSuite)

    var swapAndCompareSucceeded = false
    try {
      superRun(testName, report, stopRequested, filter, configMap, distributor, tracker)
    }
    finally {
      val shouldBeInformerForThisSuite = atomicInformer.getAndSet(zombieInformer)
      swapAndCompareSucceeded = shouldBeInformerForThisSuite eq informerForThisSuite
    }
    if (!swapAndCompareSucceeded)  // Do outside finally to workaround Scala compiler bug
      throw new ConcurrentModificationException(Resources("concurrentInformerMod", theSuite.getClass.getName))
  }
  /*
  def describeImpl(description: String, fun: => Unit, registrationClosedResource: String, sourceFile: String, methodName: String) {

    val oldBundle = atomic.get
    val (currentBranch, testNamesList, testsMap, tagsMap, registrationClosed) = oldBundle.unpack

    if (registrationClosed)
      throw new TestRegistrationClosedException(Resources(registrationClosedResource), getStackDepth(sourceFile, methodName))

    val oldBranch = currentBranch
    val newBranch = DescriptionBranch(currentBranch, description, None)
    oldBranch.subNodes ::= newBranch

    // Update atomic, making the current branch to the new branch
    updateAtomic(oldBundle, Bundle(newBranch, testNamesList, testsMap, tagsMap, registrationClosed))

    fun // Execute the function

    { // Put the old branch back as the current branch
      val oldBundle = atomic.get
      val (currentBranch, testNamesList, testsMap, tagsMap, registrationClosed) = oldBundle.unpack
      updateAtomic(oldBundle, Bundle(oldBranch, testNamesList, testsMap, tagsMap, registrationClosed))
    }
  } */

  def registerNestedBranch(description: String, childPrefix: Option[String], fun: => Unit, registrationClosedResource: String, sourceFile: String, methodName: String) {

    val oldBundle = atomic.get
    val (currentBranch, testNamesList, testsMap, tagsMap, registrationClosed) = oldBundle.unpack

    if (registrationClosed)
      throw new TestRegistrationClosedException(Resources(registrationClosedResource), getStackDepth(sourceFile, methodName))

    val oldBranch = currentBranch
    val newBranch = DescriptionBranch(currentBranch, description, childPrefix)
    oldBranch.subNodes ::= newBranch

    // Update atomic, making the current branch to the new branch
    updateAtomic(oldBundle, Bundle(newBranch, testNamesList, testsMap, tagsMap, registrationClosed))

    fun // Execute the function

    { // Put the old branch back as the current branch
      val oldBundle = atomic.get
      val (currentBranch, testNamesList, testsMap, tagsMap, registrationClosed) = oldBundle.unpack
      updateAtomic(oldBundle, Bundle(oldBranch, testNamesList, testsMap, tagsMap, registrationClosed))
    }
  }

  // Used by FlatSpec, which doesn't nest. So this one just makes a new one off of the trunk
  def registerFlatBranch(description: String, registrationClosedResource: String, sourceFile: String, methodName: String) {

    val oldBundle = atomic.get
    val (_, testNamesList, testsMap, tagsMap, registrationClosed) = oldBundle.unpack

    if (registrationClosed)
      throw new TestRegistrationClosedException(Resources(registrationClosedResource), getStackDepth(sourceFile, methodName))

    // Need to use Trunk here. I think it will be visible to all threads because
    // of the atomic, even though it wasn't inside it.
    val newBranch = DescriptionBranch(Trunk, description, None)
    Trunk.subNodes ::= newBranch

    // Update atomic, making the current branch to the new branch
    updateAtomic(oldBundle, Bundle(newBranch, testNamesList, testsMap, tagsMap, registrationClosed))
  }

  def currentBranchIsTrunk: Boolean = {

    val oldBundle = atomic.get
    var (currentBranch, _, _, _, _) = oldBundle.unpack
    currentBranch == Trunk
  }

  def registerTest(testText: String, testFun: T, testRegistrationClosedResourceName: String, sourceFileName: String, methodName: String, testTags: Tag*): String = { // returns testName

    checkRegisterTestParamsForNull(testText, testTags: _*)

    if (atomic.get.registrationClosed)
      throw new TestRegistrationClosedException(Resources(testRegistrationClosedResourceName), getStackDepth(sourceFileName, methodName))
//    throw new TestRegistrationClosedException(Resources("testCannotAppearInsideAnotherTest"), getStackDepth(sourceFileName, "test"))

    val oldBundle = atomic.get
    var (currentBranch, testNamesList, testsMap, tagsMap, registrationClosed) = oldBundle.unpack

    val testName = getTestName(testText, currentBranch)

    if (atomic.get.testsMap.keySet.contains(testName))
      throw new DuplicateTestNameException(testName, getStackDepth(sourceFileName, methodName))

    val testLeaf = TestLeaf(currentBranch, testName, testText, testFun)
    testsMap += (testName -> testLeaf)
    testNamesList ::= testName
    currentBranch.subNodes ::= testLeaf

    val tagNames = Set[String]() ++ testTags.map(_.name)
    if (!tagNames.isEmpty)
      tagsMap += (testName -> tagNames)

    updateAtomic(oldBundle, Bundle(currentBranch, testNamesList, testsMap, tagsMap, registrationClosed))

    testName
  }

  def registerIgnoredTest(testText: String, f: T, testRegistrationClosedResourceName: String, sourceFileName: String, methodName: String, testTags: Tag*) {

    checkRegisterTestParamsForNull(testText, testTags: _*)

// If this works delete this. I think we can rely on registerTest's check
//    if (atomic.get.registrationClosed)
//      throw new TestRegistrationClosedException(Resources("ignoreCannotAppearInsideATest"), getStackDepth(sourceFileName, "ignore"))

    val testName = registerTest(testText, f, testRegistrationClosedResourceName, sourceFileName, methodName) // Call test without passing the tags

    val oldBundle = atomic.get
    var (currentBranch, testNamesList, testsMap, tagsMap, registrationClosed) = oldBundle.unpack

    val tagNames = Set[String]() ++ testTags.map(_.name)
    tagsMap += (testName -> (tagNames + IgnoreTagName))

    updateAtomic(oldBundle, Bundle(currentBranch, testNamesList, testsMap, tagsMap, registrationClosed))
  }

  private[scalatest] def getTestNamePrefix(branch: Branch): String =
    branch match {
      case Trunk => ""
      // Call to getTestNamePrefix is not tail recursive, but I don't expect
      // the describe nesting to be very deep (famous last words).
      case DescriptionBranch(parent, descriptionText, childPrefix) =>
        val optionalChildPrefixAndDescriptionText =
          childPrefix match {
            case Some(cp) => Resources("prefixSuffix", descriptionText, cp)
            case _ => descriptionText
          }
        Resources("prefixSuffix", getTestNamePrefix(parent), optionalChildPrefixAndDescriptionText).trim
    }

  private[scalatest] def getTestName(testText: String, parent: Branch): String =
    Resources("prefixSuffix", getTestNamePrefix(parent), testText).trim

  private def checkRegisterTestParamsForNull(testText: String, testTags: Tag*) {
    if (testText == null)
      throw new NullPointerException("testText was null")
    if (testTags.exists(_ == null))
      throw new NullPointerException("a test tag was null")
  }
}

private[scalatest] class Engine(concurrentBundleModResourceName: String, simpleClassName: String)
    extends SuperEngine[() => Unit](concurrentBundleModResourceName, simpleClassName)

private[scalatest] class FixtureEngine[FixtureParam](concurrentBundleModResourceName: String, simpleClassName: String)
    extends SuperEngine[FixtureParam => Any](concurrentBundleModResourceName, simpleClassName)
