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
package org.scalatest.junit

import org.scalatest._
import _root_.junit.framework.{TestCase, Assert, AssertionFailedError}
import org.junit.runner.RunWith

/**
 * A suite of tests that mimics the syntax of a JUnit 3 <code>TestCase</code>, which can be run with either
 * JUnit 4 or ScalaTest.
 *
 * This class allows you to write tests with ScalaTest's more concise assertion syntax as well as JUnit-style assertions (<code>assertEquals</code>, etc.).
 * You create tests by defining methods that start with <code>test</code>, and can create fixtures with methods
 * named <code>setUp</code> and <code>tearDown</code>. <code>JUnit3ComfortSuite</code> is intended for people who are
 * familiar with JUnit 3 and want to get started quickly writing tests with ScalaTest. Here's an example:
 *
 * <pre class="stHighlight">
 * import org.scalatest.junit.JUnit3ComfortSuite
 * import scala.collection.mutable.ListBuffer
 *
 * class BlastFromThePastSuite extends JUnit3ComfortSuite {
 *
 *   var sb: StringBuilder = _
 *   var lb: ListBuffer[String] = _
 *
 *   override def setUp() {
 *     sb = new StringBuilder("ScalaTest is ")
 *     lb = new ListBuffer[String]
 *   }
 *
 *   def testEasy() { // Uses JUnit-style assertions
 *     sb.append("easy!")
 *     assertEquals("ScalaTest is easy!", sb.toString)
 *     assertTrue(lb.isEmpty)
 *     lb += "sweet"
 *   }
 *
 *   def testFun() { // Uses ScalaTest assertions
 *     sb.append("fun!")
 *     assert(sb.toString === "ScalaTest is fun!")
 *     assert(lb.isEmpty)
 *   }
 * }
 * </pre>
 * 
 * <p>
 * To execute a <code>JUnit3ComfortSuite</code>s with ScalaTest's <code>Runner</code>, you must include a JUnit 4 jar file on the class path or runpath.
 * </p>
 *
 * <p>
 * One difference between a <code>JUnit3ComfortSuite</code> and a JUnit 3 <code>TestCase</code>
 * is that tags are supported in a <code>JUnit3ComfortSuite</code>, but JUnit 3 had no such
 * concept. Tags work in a <code>JUnit3ComfortSuite</code> exactly like they work in <code>Suite</code>:
 * you place annotations on the test methods. This allows you, for example, to ignore a test in
 * a <code>JUnit3ComfortSuite</code> like this:
 * </p>
 *
 * <pre class="stHighlight">
 * @Ignore
 * def testSubtraction() {
 *   val diff = 4 - 1
 *   assert(diff === 3)
 *   assert(diff - 2 === 1)
 * }
 * </pre>
 *
 * <p>
 * Note: the reason <code>JUnit3ComfortSuite</code> is not named <code>JUnit3Suite</code> is because that name might imply
 * that an instance of this class is a JUnit 3 <code>TestCase</code>, just as <code>JUnitSuite</code> is an actual JUnit 4 test
 * class and <code>TestNGSuite</code> is an actual TestNG test class. By contrast, a <code>JUnit3ComfortSuite</code> is not actually
 * a JUnit 3 <code>TestCase</code>, it just looks and behaves like one. Thus it gives you that comfortable familiar feeling of
 * programming with JUnit 3, without actually being JUnit 3.
 * </p>
 *
 * @author Bill Venners
 */
@RunWith(classOf[JUnitRunner])
private [junit] class JUnit3ComfortSuite extends Suite with OneInstancePerTest {

  /**
   * Defines a method to be run before each of this suite's tests. This trait's implementation
   * of <code>runTest</code> invokes this method before running
   * each test, thus this method can be used to set up a test fixture
   * needed by each test. This trait's implementation of this method does nothing.
   */
  protected def setUp() = ()
  
  /**
   * Defines a method to be run after each of this suite's tests. This trait's implementation
   * of <code>runTest</code> invokes this method after running
   * each test, thus this method can be used to tear down a test fixture
   * needed by each test. This trait's implementation of this method does nothing.
   */
  protected def tearDown() = ()
  
  /**
   * Run a test surrounded by calls to <code>setUp</code> and <code>tearDown</code>.
   * This trait's implementation of this method ("this method") invokes <code>setUp</code>
   * before running each test and <code>tearDown</code>
   * after running each test. It runs each test by invoking <code>super.runTest</code>, passing along
   * the four parameters passed to it.
   * 
   * <p>
   * If any invocation of <code>setUp</code> completes abruptly with an exception, this
   * method will complete abruptly with the same exception. If any call to
   * <code>super.runTest</code> completes abruptly with an exception, this method
   * will complete abruptly with the same exception, however, before doing so, it will
   * invoke <code>tearDown</code>. If <cod>tearDown</code> <em>also</em> completes abruptly with an exception, this 
   * method will nevertheless complete abruptly with the exception previously thrown by <code>super.runTest</code>.
   * If <code>super.runTest</code> returns normally, but <code>tearDown</code> completes abruptly with an
   * exception, this method will complete abruptly with the same exception.
   * </p>
  */
  override def runTest(testName: String, reporter: Reporter, stopper: Stopper, configMap: Map[String, Any], tracker: Tracker) {

    var thrownException: Option[Throwable] = None

    setUp()
    try {
      super.runTest(testName, reporter, stopper, configMap, tracker)
    }
    catch {
      case e: Exception => thrownException = Some(e)
    }
    finally {
      try {
        tearDown() // Make sure that afterEach is called even if runTest completes abruptly.
        thrownException match {
          case Some(e) => throw e
          case None => 
        }
      }
      catch {
        case laterException: Exception =>
          thrownException match { // If both run and afterAll throw an exception, report the test exception
            case Some(earlierException) => throw earlierException
            case None => throw laterException
          }
      }
    }
  }

  /**
   * Assert that a boolean condition, described in <code>String</code>
   * <code>message</code>, is true.
   * If the condition is <code>true</code>, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param condition the boolean condition to assert
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the condition is <code>false</code>.
   */
  def assertTrue(message: String, condition: Boolean) {
    Assert.assertTrue(message, condition)
  }

  /**
   * Assert that a boolean condition is true.
   * If the condition is <code>true</code>, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   *
   * @param condition the boolean condition to assert
   * @throws AssertionFailedError if the condition is <code>false</code>.
   */
  def assertTrue(condition: Boolean) {
    Assert.assertTrue(condition)
  }

  /**
   * Assert that a boolean condition, described in <code>String</code>
   * <code>message</code>, is false.
   * If the condition is <code>false</code>, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param condition the boolean condition to ensure is false
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the condition is <code>true</code>.
   */
  def assertFalse(message: String, condition: Boolean) {
    Assert.assertFalse(message, condition)
  }

  /**
   * Assert that a boolean condition is false.
   * If the condition is <code>true</code>, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   *
   * @param condition the boolean condition to assert
   * @throws AssertionFailedError if the condition is <code>false</code>.
   */
  def assertFalse(condition: Boolean) {
    Assert.assertFalse(condition)
  }

  /**
   * Throws <code>AssertionFailedError</code>, with the passed
   * <code>String</code> <code>message</code> as the exception's detail
   * message, to indicate a test failed.
   *
   * @param message A message describing the failure.
   */
  override def fail(message: String) = throw new AssertionFailedError(message)

  /**
   * Throws <code>AssertionFailedError</code> to indicate a test failed.
   */
  override def fail(): Nothing = throw new AssertionFailedError

  /**
   * Assert that two objects are equal, with additional information described in a <code>String</code>
   * <code>message</code>.
   * If the two objects are equal, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the two objects are not equal.
   */
  def assertEquals(message: String, expected: AnyRef, actual: AnyRef) {
    Assert.assertEquals(message, expected, actual)
  }

  /**
   * Assert that a two objects are equal.
   * If the two objects are equal, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @throws AssertionFailedError if the two objects are equal.
   */
  def assertEquals(expected: AnyRef, actual: AnyRef) {
    Assert.assertEquals(expected, actual)
  }

  /**
   * Assert that two <code>String</code>s are equal, with additional information described in a <code>String</code>
   * <code>message</code>.
   * If the two <code>String</code>s are equal, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the two <code>String</code>s are not equal.
   */
  def assertEquals(message: String, expected: String, actual: String) {
    Assert.assertEquals(message, expected, actual)
  }

  /**
   * Assert that a two <code>String</code>s are equal.
   * If the two <code>String</code>s are equal, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @throws AssertionFailedError if the two <code>String</code>s are not equal.
   */
  def assertEquals(expected: String, actual: String) {
    Assert.assertEquals(expected, actual)
  }

  /**
   * Assert that two <code>Double</code>s are equal within a tolerance specified by <code>delta</code>,
   * with additional information described in a <code>String</code> <code>message</code>.
   * If the two <code>Double</code>s are equal within the tolerance, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   * If the expected value is infinity then the delta value is ignored.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the two <code>Double</code>s are not equal within the tolerance specified by <code>delta</code>.
   */
  def assertEquals(message: String, expected: Double, actual: Double, delta: Double) {
    Assert.assertEquals(message, expected, actual, delta)
  }

  /**
   * Assert that a two <code>Double</code>s are equal within a tolerance specified by <code>delta</code>.
   * If the two <code>Double</code>s are equal within the tolerance, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   * If the expected value is infinity then the delta value is ignored.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @throws AssertionFailedError if the two <code>Double</code>s are not equal within the tolerance specified by <code>delta</code>.
   */
  def assertEquals(expected: Double, actual: Double, delta: Double) {
    Assert.assertEquals(expected, actual, delta)
  }

  /**
   * Assert that two <code>Float</code>s are equal within a tolerance specified by <code>delta</code>,
   * with additional information described in a <code>String</code> <code>message</code>.
   * If the two <code>Float</code>s are equal within the tolerance, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   * If the expected value is infinity then the delta value is ignored.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the two <code>Float</code>s are not equal within the tolerance specified by <code>delta</code>.
   */
  def assertEquals(message: String, expected: Float, actual: Float, delta: Float) {
    Assert.assertEquals(message, expected, actual, delta)
  }

  /**
   * Assert that a two <code>Float</code>s are equal within a tolerance specified by <code>delta</code>.
   * If the two <code>Float</code>s are equal within the tolerance, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   * If the expected value is infinity then the delta value is ignored.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @throws AssertionFailedError if the two <code>Float</code>s are not equal within the tolerance specified by <code>delta</code>.
   */
  def assertEquals(expected: Float, actual: Float, delta: Float) {
    Assert.assertEquals(expected, actual, delta)
  }

  /**
   * Assert that two <code>Long</code>s are equal within a tolerance specified by <code>delta</code>,
   * with additional information described in a <code>String</code> <code>message</code>.
   * If the two <code>Long</code>s are equal, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the two <code>Long</code>s are not equal.
   */
  def assertEquals(message: String, expected: Long, actual: Long) {
    Assert.assertEquals(message, expected, actual)
  }

  /**
   * Assert that a two <code>Long</code>s are equal.
   * If the two <code>Long</code>s are equal, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @throws AssertionFailedError if the two <code>Long</code>s are equal.
   */
  def assertEquals(expected: Long, actual: Long) {
    Assert.assertEquals(expected, actual)
  }

  /**
   * Assert that two <code>Boolean</code>s are equal, with additional information described in a <code>String</code>
   * <code>message</code>.
   * If the two <code>Boolean</code>s are equal, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the two <code>Boolean</code>s are not equal.
   */
  def assertEquals(message: String, expected: Boolean, actual: Boolean) {
    Assert.assertEquals(message, expected, actual)
  }

  /**
   * Assert that a two <code>Boolean</code>s are equal.
   * If the two <code>Boolean</code>s are equal, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @throws AssertionFailedError if the two <code>Boolean</code>s are equal.
   */
  def assertEquals(expected: Boolean, actual: Boolean) {
    Assert.assertEquals(expected, actual)
  }

  /**
   * Assert that two <code>Byte</code>s are equal, with additional information described in a <code>String</code>
   * <code>message</code>.
   * If the two <code>Byte</code>s are equal, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the two <code>Byte</code>s are not equal.
   */
  def assertEquals(message: String, expected: Byte, actual: Byte) {
    Assert.assertEquals(message, expected, actual)
  }

  /**
   * Assert that a two <code>Byte</code>s are equal.
   * If the two <code>Byte</code>s are equal, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @throws AssertionFailedError if the two <code>Byte</code>s are equal.
   */
  def assertEquals(expected: Byte, actual: Byte) {
    Assert.assertEquals(expected, actual)
  }

  /**
   * Assert that two <code>Char</code>s are equal, with additional information described in a <code>String</code>
   * <code>message</code>.
   * If the two <code>Char</code>s are equal, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the two <code>Char</code>s are not equal.
   */
  def assertEquals(message: String, expected: Char, actual: Char) {
    Assert.assertEquals(message, expected, actual)
  }

  /**
   * Assert that a two <code>Char</code>s are equal.
   * If the two <code>Char</code>s are equal, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @throws AssertionFailedError if the two <code>Char</code>s are equal.
   */
  def assertEquals(expected: Char, actual: Char) {
    Assert.assertEquals(expected, actual)
  }

  /**
   * Assert that two <code>Short</code>s are equal, with additional information described in a <code>String</code>
   * <code>message</code>.
   * If the two <code>Short</code>s are equal, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the two <code>Short</code>s are not equal.
   */
  def assertEquals(message: String, expected: Short, actual: Short) {
    Assert.assertEquals(message, expected, actual)
  }

  /**
   * Assert that a two <code>Short</code>s are equal.
   * If the two <code>Short</code>s are equal, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @throws AssertionFailedError if the two <code>Short</code>s are equal.
   */
  def assertEquals(expected: Short, actual: Short) {
    Assert.assertEquals(expected, actual)
  }

  /**
   * Assert that two <code>Int</code>s are equal, with additional information described in a <code>String</code>
   * <code>message</code>.
   * If the two <code>Int</code>s are equal, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the two <code>Int</code>s are not equal.
   */
  def assertEquals(message: String, expected: Int, actual: Int) {
    Assert.assertEquals(message, expected, actual)
  }

  /**
   * Assert that a two <code>Int</code>s are equal.
   * If the two <code>Int</code>s are equal, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @throws AssertionFailedError if the two <code>Int</code>s are equal.
   */
  def assertEquals(expected: Int, actual: Int) {
    Assert.assertEquals(expected, actual)
  }

  /**
   * Assert that an object reference is not <code>null</code>.
   * If the object is non-<code>null</code>, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   *
   * @param anyRef the object reference to check for <code>null</code>
   * @throws AssertionFailedError if the object reference is <code>null</code>
   */
  def assertNotNull(anyRef: AnyRef) {
    Assert.assertNotNull(anyRef)
  }

  /**
   * Assert that an object reference is not <code>null</code>, with
   * additional information described in a <code>String</code> <code>message</code>.
   * If the object is non-<code>null</code>, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param anyRef the object reference to check for <code>null</code>
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the object reference is <code>null</code>
   */
  def assertNotNull(message: String, anyRef: AnyRef) {
    Assert.assertNotNull(message, anyRef)
  }

  /**
   * Assert that an object reference is <code>null</code>.
   * If the object is <code>null</code>, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code>.
   *
   * @param anyRef the object reference to ensure is <code>null</code>
   * @throws AssertionFailedError if the object reference is non-<code>null</code>
   */
  def assertNull(anyRef: AnyRef) {
    Assert.assertNull(anyRef)
  }

  /**
   * Assert that an object reference is <code>null</code>, with
   * additional information described in a <code>String</code> <code>message</code>.
   * If the object is <code>null</code>, this method returns normally.
   * Else, it throws <code>AssertionFailedError</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param anyRef the object reference to ensure is <code>null</code>
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the object reference is non-<code>null</code>
   */
  def assertNull(message: String, anyRef: AnyRef) {
    Assert.assertNull(message, anyRef)
  }

  /**
   * Assert that two object references refer to the same object,
   * with additional information described in a <code>String</code> <code>message</code>.
   * If the two object references refer to the same object, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the two object references refer to different objects.
   */
  def assertSame(message: String, expected: AnyRef, actual: AnyRef) {
    Assert.assertSame(message, expected, actual)
  }

  /**
   * Assert that two object references refer to the same object.
   * If the two object references refer to the same object, this method returns normally.
   * Else, it throws <code>TestFailedException</code>.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @throws AssertionFailedError if the two object references refer to different objects.
   */
  def assertSame(expected: AnyRef, actual: AnyRef) {
    Assert.assertSame(expected, actual)
  }

  /**
   * Assert that two object references do not refer to the same object,
   * with additional information described in a <code>String</code> <code>message</code>.
   * If the two object references refer to different objects, this method returns normally.
   * Else, it throws <code>TestFailedException</code> with the
   * specified <code>message</code> as the exception's detail message.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @param message a <code>String</code> message to include in a failure report.
   * @throws AssertionFailedError if the two object references refer to the same object.
   */
  def assertNotSame(message: String, expected: AnyRef, actual: AnyRef) {
    Assert.assertNotSame(message, expected, actual)
  }

  /**
   * Assert that two object references do not refer to the same object.
   * If the two object references refer to different objects, this method returns normally.
   * Else, it throws <code>TestFailedException</code>.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @throws AssertionFailedError if the two object references refer to the same object.
   */
  def assertNotSame(expected: AnyRef, actual: AnyRef) {
    Assert.assertNotSame(expected, actual)
  }
}

/*
 * <strong>Note: <code>JUnit3Suite</code> has been deprecated, and will be removed in a future version of ScalaTest. Please
 * change to using <code>JUnit3ComforSuite</code> instead. <code>JUnit3ComfortSuite</code> does not extend <code>junit.framework.TestCase</code>.
 * In versions of ScalaTest prior to 0.9.5, <code>JUnit3Suite</code> extended <code>TestCase</code> so that it could be run by a JUnit 3 runner. In
 * 0.9.6, ScalaTest's <code>execute</code> methods were renamed to <code>run</code>, which is not compatible with <code>TestCase</code>.
 * As a result the goal of providing a trait in ScalaTest that can either run with ScalaTest and JUnit 3 was dropped. Instead, the
 * <code>JUnit3ComfortSuite</code> trait can give you that comfortable feeling of using JUnit 3-like syntax, and it can be run with
 * either ScalaTest or JUnit 4.</strong>
 */
//@deprecated
//class JUnit3Suite extends JUnit3ComfortSuite
