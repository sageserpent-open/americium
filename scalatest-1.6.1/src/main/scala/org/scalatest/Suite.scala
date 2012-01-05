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

import java.awt.AWTError
import java.lang.annotation._
import java.io.Serializable
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.charset.CoderMalfunctionError
import javax.xml.parsers.FactoryConfigurationError
import javax.xml.transform.TransformerFactoryConfigurationError
import Suite.simpleNameForTest
import Suite.parseSimpleName
import Suite.stripDollars
import Suite.formatterForSuiteStarting
import Suite.formatterForSuiteCompleted
import Suite.checkForPublicNoArgConstructor
import Suite.formatterForSuiteAborted
import Suite.anErrorThatShouldCauseAnAbort
import Suite.getSimpleNameOfAnObjectsClass
import Suite.takesInformer
import Suite.isTestMethodGoodies
import Suite.testMethodTakesAnInformer
import scala.collection.immutable.TreeSet
import Suite.getIndentedText
import org.scalatest.events._
import org.scalatest.tools.StandardOutReporter
import Suite.checkRunTestParamsForNull
import Suite.getIndentedTextForInfo
import Suite.getMessageForException
import Suite.reportTestStarting
import Suite.reportTestIgnored
import Suite.reportTestSucceeded
import Suite.reportTestPending
import Suite.reportInfoProvided

/**
 * A suite of tests. A <code>Suite</code> instance encapsulates a conceptual
 * suite (<em>i.e.</em>, a collection) of tests.
 *
 * <p>
 * This trait provides an interface that allows suites of tests to be run.
 * Its implementation enables a default way of writing and executing tests.  Subtraits and subclasses can
 * override <code>Suite</code>'s methods to enable other ways of writing and executing tests.
 * This trait's default approach allows tests to be defined as methods whose name starts with "<code>test</code>."
 * This approach is easy to understand, and a good way for Scala beginners to start writing tests.
 * More advanced Scala programmers may prefer to mix together other <code>Suite</code> subtraits defined in ScalaTest, 
 * or create their own, to write tests in the way they feel makes them most productive. Here's a quick overview
 * of some of the options to help you get started:
 * </p>
 *
 * <p>
 * <em>For JUnit 3 users</em>
 * </p>
 *
 * <p>
 * If you are using JUnit 3 (version 3.8 or earlier releases) and you want to write JUnit 3 tests in Scala, look at
 * <a href="junit/AssertionsForJUnit.html"><code>AssertionsForJUnit</code></a>, 
 * <a href="junit/ShouldMatchersForJUnit.html"><code>ShouldMatchersForJUnit</code></a>, and
 * <a href="junit/JUnit3Suite.html"><code>JUnit3Suite</code></a>. 
 * </p>
 *
 * <p>
 * <em>For JUnit 4 users</em>
 * </p>
 *
 * <p>
 * If you are using JUnit 4 and you want to write JUnit 4 tests in Scala, look at
 * <a href="junit/JUnitSuite.html"><code>JUnitSuite</code></a>, and
 * <a href="junit/JUnitRunner.html"><code>JUnitRunner</code></a>. With <code>JUnitRunner</code>,
 * you can use any of the traits described here and still run your tests with JUnit 4.
 * </p>
 *
 * <p>
 * <em>For TestNG users</em>
 * </p>
 *
 * <p>
 * If you are using TestNG and you want to write TestNG tests in Scala, look at
 * <a href="testng/TestNGSuite.html"><code>TestNGSuite</code></a>.
 * </p>
 *
 * <p>
 * <em>For high-level testing</em>
 * </p>
 *
 * <p>
 * If you want to write tests at a higher level than unit tests, such as integration tests, acceptance tests,
 * or functional tests, check out <a href="FeatureSpec.html"><code>FeatureSpec</code></a>.
 * </p>
 *
 * <p>
 * <em>For unit testing</em>
 * </p>
 *
 * <p>
 * If you prefer a behavior-driven development (BDD) style, in which tests are combined with text that
 * specifies the behavior being tested, look at
 * <a href="Spec.html"><code>Spec</code></a>, 
 * <a href="FlatSpec.html"><code>FlatSpec</code></a>,
 * <a href="FreeSpec.html"><code>FreeSpec</code></a>, and
 * <a href="WordSpec.html"><code>WordSpec</code></a>. Otherwise, if you just want to write tests
 * and don't want to combine testing with specifying, look at 
 * <a href="FunSuite.html"><code>FunSuite</code></a> or read on to learn how to write
 * tests using this base trait, <code>Suite</code>. 
 * </p>
 *
 * <p>
 * To use this trait's approach to writing tests, simply create classes that
 * extend <code>Suite</code> and define test methods. Test methods have names of the form <code>testX</code>, 
 * where <code>X</code> is some unique, hopefully meaningful, string. A test method must be public and
 * can have any result type, but the most common result type is <code>Unit</code>. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 *
 * class ExampleSuite extends Suite {
 *
 *   def testAddition {
 *     val sum = 1 + 1
 *     assert(sum === 2)
 *   }
 *
 *   def testSubtraction {
 *     val diff = 4 - 1
 *     assert(diff === 3)
 *   }
 * }
 * </pre>
 *
 * <p>
 * You can run a <code>Suite</code> by invoking <code>execute</code> on it.
 * This method, which prints test results to the standard output, is intended to serve as a
 * convenient way to run tests from within the Scala interpreter. For example,
 * to run <code>ExampleSuite</code> from within the Scala interpreter, you could write:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new ExampleSuite).execute()
 * </pre>
 *
 * <p>
 * And you would see:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">ExampleSuite:
 * - testAddition
 * - testSubtraction</span>
 * </pre>
 *
 * <p>
 * Or, to run just the <code>testAddition</code> method, you could write:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new ExampleSuite).execute("testAddition")
 * </pre>
 *
 * <p>
 * And you would see:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">ExampleSuite:
 * - testAddition</span>
 * </pre>
 *
 * <p>
 * You can also pass to <code>execute</code> a <a href="#configMapSection"><em>config map</em></a> of key-value
 * pairs, which will be passed down into suites and tests, as well as other parameters that configure the run itself.
 * For more information on running in the Scala interpreter, see the documentation for <code>execute</code> (below) and the
 * <a href="Shell.html">ScalaTest shell</a>.</code>
 * </p>
 *
 * <p>
 * The <code>execute</code> method invokes a <code>run</code> method takes seven
 * parameters. This <code>run</code> method, which actually executes the suite, will usually be invoked by a test runner, such
 * as <code>org.scalatest.tools.Runner</code> or an IDE. See the <a href="tools/Runner$.html">documentation
 * for <code>Runner</code></a> for more details.
 * </p>
 *
 * <h2>Assertions and <code>=</code><code>=</code><code>=</code></h2>
 *
 * <p>
 * Inside test methods in a <code>Suite</code>, you can write assertions by invoking <code>assert</code> and passing in a <code>Boolean</code> expression,
 * such as:
 * </p>
 *
 * <pre class="stHighlight">
 * val left = 2
 * val right = 1
 * assert(left == right)
 * </pre>
 *
 * <p>
 * If the passed expression is <code>true</code>, <code>assert</code> will return normally. If <code>false</code>,
 * <code>assert</code> will complete abruptly with a <code>TestFailedException</code>. This exception is usually not caught
 * by the test method, which means the test method itself will complete abruptly by throwing the <code>TestFailedException</code>. Any
 * test method that completes abruptly with an exception is considered a failed
 * test. A test method that returns normally is considered a successful test.
 * </p>
 *
 * <p>
 * If you pass a <code>Boolean</code> expression to <code>assert</code>, a failed assertion will be reported, but without
 * reporting the left and right values. You can alternatively encode these values in a <code>String</code> passed as
 * a second argument to <code>assert</code>, as in:
 * </p>
 * 
 * <pre class="stHighlight">
 * val left = 2
 * val right = 1
 * assert(left == right, left + " did not equal " + right)
 * </pre>
 *
 * <p>
 * Using this form of <code>assert</code>, the failure report will include the left and right values, 
 * helping you debug the problem. However, ScalaTest provides the <code>===</code> operator to make this easier.
 * (The <code>===</code> operator is defined in trait <a href="Assertions.html"><code>Assertions</code></a> which trait <code>Suite</code> extends.)
 * You use it like this:
 * </p>
 *
 * <pre class="stHighlight">
 * val left = 2
 * val right = 1
 * assert(left === right)
 * </pre>
 *
 * <p>
 * Because you use <code>===</code> here instead of <code>==</code>, the failure report will include the left
 * and right values. For example, the detail message in the thrown <code>TestFailedException</code> from the <code>assert</code>
 * shown previously will include, "2 did not equal 1".
 * From this message you will know that the operand on the left had the value 2, and the operand on the right had the value 1.
 * </p>
 *
 * <p>
 * If you're familiar with JUnit, you would use <code>===</code>
 * in a ScalaTest <code>Suite</code> where you'd use <code>assertEquals</code> in a JUnit <code>TestCase</code>.
 * The <code>===</code> operator is made possible by an implicit conversion from <code>Any</code>
 * to <code>Equalizer</code>. If you're curious to understand the mechanics, see the <a href="Assertions$Equalizer.html">documentation for
 * <code>Equalizer</code></a> and the <code>convertToEqualizer</code> method.
 * </p>
 *
 * <h2>Expected results</h2>
 *
 * Although <code>===</code> provides a natural, readable extension to Scala's <code>assert</code> mechanism,
 * as the operands become lengthy, the code becomes less readable. In addition, the <code>===</code> comparison
 * doesn't distinguish between actual and expected values. The operands are just called <code>left</code> and <code>right</code>,
 * because if one were named <code>expected</code> and the other <code>actual</code>, it would be difficult for people to
 * remember which was which. To help with these limitations of assertions, <code>Suite</code> includes a method called <code>expect</code> that
 * can be used as an alternative to <code>assert</code> with <code>===</code>. To use <code>expect</code>, you place
 * the expected value in parentheses after <code>expect</code>, followed by curly braces containing code 
 * that should result in the expected value. For example:
 *
 * <pre class="stHighlight">
 * val a = 5
 * val b = 2
 * expect(2) {
 *   a - b
 * }
 * </pre>
 *
 * <p>
 * In this case, the expected value is <code>2</code>, and the code being tested is <code>a - b</code>. This expectation will fail, and
 * the detail message in the <code>TestFailedException</code> will read, "Expected 2, but got 3."
 * </p>
 *
 * <h2>Intercepted exceptions</h2>
 *
 * <p>
 * Sometimes you need to test whether a method throws an expected exception under certain circumstances, such
 * as when invalid arguments are passed to the method. You can do this in the JUnit style, like this:
 * </p>
 *
 * <pre class="stHighlight">
 * val s = "hi"
 * try {
 *   s.charAt(-1)
 *   fail()
 * }
 * catch {
 *   case _: IndexOutOfBoundsException => // Expected, so continue
 * }
 * </pre>
 *
 * <p>
 * If <code>charAt</code> throws <code>IndexOutOfBoundsException</code> as expected, control will transfer
 * to the catch case, which does nothing. If, however, <code>charAt</code> fails to throw an exception,
 * the next statement, <code>fail()</code>, will be executed. The <code>fail</code> method always completes abruptly with
 * a <code>TestFailedException</code>, thereby signaling a failed test.
 * </p>
 *
 * <p>
 * To make this common use case easier to express and read, ScalaTest provides an <code>intercept</code>
 * method. You use it like this:
 * </p>
 *
 * <pre class="stHighlight">
 * val s = "hi"
 * intercept[IndexOutOfBoundsException] {
 *   s.charAt(-1)
 * }
 * </pre>
 *
 * <p>
 * This code behaves much like the previous example. If <code>charAt</code> throws an instance of <code>IndexOutOfBoundsException</code>,
 * <code>intercept</code> will return that exception. But if <code>charAt</code> completes normally, or throws a different
 * exception, <code>intercept</code> will complete abruptly with a <code>TestFailedException</code>. The <code>intercept</code> method returns the
 * caught exception so that you can inspect it further if you wish, for example, to ensure that data contained inside
 * the exception has the expected values. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * val s = "hi"
 * val caught =
 *   intercept[IndexOutOfBoundsException] {
 *     s.charAt(-1)
 *   }
 * assert(caught.getMessage === "String index out of range: -1")
 * </pre>
 *
 * <h2>Using other assertions</h2>
 *
 * <p>
 * ScalaTest also supports another style of assertions via its matchers DSL. By mixing in
 * trait <a href="matchers/ShouldMatchers.html"><code>ShouldMatchers</code></a>, you can 
 * write suites that look like:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 * import org.scalatest.matchers.ShouldMatchers
 *
 * class ExampleSuite extends Suite with ShouldMatchers {
 *
 *   def testAddition {
 *     val sum = 1 + 1
 *     sum should equal (2)
 *   }
 *
 *   def testSubtraction {
 *     val diff = 4 - 1
 *     diff should equal (3)
 *   }
 * }
 * </pre>
 * 
 * <p>If you prefer the word "<code>must</code>" to the word "<code>should</code>," you can alternatively mix in
 * trait <a href="matchers/MustMatchers.html"><code>MustMatchers</code></a>.
 * </p>
 *
 * <p>
 * If you are comfortable with assertion mechanisms from other test frameworks, chances
 * are you can use them with ScalaTest. Any assertion mechanism that indicates a failure with an exception
 * can be used as is with ScalaTest. For example, to use the <code>assertEquals</code>
 * methods provided by JUnit or TestNG, simply import them and use them. (You will of course need
 * to include the relevant JAR file for the framework whose assertions you want to use on either the
 * classpath or runpath when you run your tests.) Here's an example in which JUnit's assertions are
 * imported, then used within a ScalaTest suite:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 * import org.junit.Assert._
 *
 * class ExampleSuite extends Suite {
 *
 *   def testAddition {
 *     val sum = 1 + 1
 *     assertEquals(2, sum)
 *   }
 *
 *   def testSubtraction {
 *     val diff = 4 - 1
 *     assertEquals(3, diff)
 *   }
 * }
 * </pre>
 *
 * <h2>Nested suites</h2>
 *
 * <p>
 * A <code>Suite</code> can refer to a collection of other <code>Suite</code>s,
 * which are called <em>nested</em> <code>Suite</code>s. Those nested  <code>Suite</code>s can in turn have
 * their own nested  <code>Suite</code>s, and so on. Large test suites can be organized, therefore, as a tree of
 * nested <code>Suite</code>s.
 * This trait's <code>run</code> method, in addition to invoking its
 * test methods, invokes <code>run</code> on each of its nested <code>Suite</code>s.
 * </p>
 *
 * <p>
 * A <code>List</code> of a <code>Suite</code>'s nested <code>Suite</code>s can be obtained by invoking its
 * <code>nestedSuites</code> method. If you wish to create a <code>Suite</code> that serves as a
 * container for nested <code>Suite</code>s, whether or not it has test methods of its own, simply override <code>nestedSuites</code>
 * to return a <code>List</code> of the nested <code>Suite</code>s. Because this is a common use case, ScalaTest provides
 * a convenience <code>Suites</code> class, which takes a variable number of nested <code>Suite</code>s as constructor
 * parameters. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 * import org.scalatest.Suites
 *
 * class ASuite extends Suite {
 *   def testA {}
 * }
 * class BSuite extends Suite {
 *   def testB {}
 * }
 * class CSuite extends Suite {
 *   def testC {}
 * }
 *
 * class AlphabetSuite extends Suites(
 *   new ASuite,
 *   new BSuite,
 *   new CSuite
 * )
 * </pre>
 *
 * <p>
 * If you now run <code>AlphabetSuite</code>:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new AlphabetSuite).execute()
 * </pre>
 *
 * <p>
 * You will see reports printed to the standard output that indicate the nested
 * suites&#8212;<code>ASuite</code>, <code>BSuite</code>, and
 * <code>CSuite</code>&#8212;were run:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">AlphabetSuite:
 * ASuite:
 * - testA
 * BSuite:
 * - testB
 * CSuite:
 * - testC</span>
 * </pre>
 *
 * <p>
 * Note that <code>Runner</code> can discover <code>Suite</code>s automatically, so you need not
 * necessarily define nested <code>Suites</code> explicitly. See the <a href="tools/Runner$.html$membersOnlyWildcard">documentation
 * for <code>Runner</code></a> for more information.
 * </p>
 *
 * <a name="configMapSection"></a><h2>The config map</h2>
 *
 * <p>
 * In some cases you may need to pass information to a suite of tests.
 * For example, perhaps a suite of tests needs to grab information from a file, and you want
 * to be able to specify a different filename during different runs.  You can accomplish this in ScalaTest by passing
 * the filename in a <em>config map</em> of key-value pairs, which is passed to <code>run</code> as a <code>Map[String, Any]</code>.
 * The values in the config map are called "config objects," because they can be used to <em>configure</em>
 * suites, reporters, and tests.
 * </p>
 *
 * <p>
 * You can specify a string config object is via the ScalaTest <code>Runner</code>, either via the command line
 * or ScalaTest's ant task.
 * (See the <a href="tools/Runner$.html#configMapSection">documentation for Runner</a> for information on how to specify 
 * config objects on the command line.)
 * The config map is passed to <code>run</code>, <code>runNestedSuites</code>, <code>runTests</code>, and <code>runTest</code>,
 * so one way to access it in your suite is to override one of those methods. If you need to use the config map inside your tests, you
 * can access it from the <code>NoArgTest</code> passed to <code>withFixture</code>, or the <code>OneArgTest</code> passed to
 * <code>withFixture</code> in the traits in the <code>org.scalatest.fixture</code> package. (See the
 * <a href="fixture/FixtureSuite.html">documentation for <code>FixtureSuite</code></a>
 * for instructions on how to access the config map in tests.)
 * </p>
 *
 * <h2>Ignored tests</h2>
 *
 * <p>
 * Another common use case is that tests must be &#8220;temporarily&#8221; disabled, with the
 * good intention of resurrecting the test at a later time. ScalaTest provides an <code>Ignore</code>
 * annotation for this purpose. You use it like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 * import org.scalatest.Ignore
 *
 * class ExampleSuite extends Suite {
 *
 *   def testAddition {
 *     val sum = 1 + 1
 *     assert(sum === 2)
 *   }
 *
 *   @Ignore
 *   def testSubtraction {
 *     val diff = 4 - 1
 *     assert(diff === 3)
 *   }
 * }
 * </pre>
 *
 * <p>
 * If you run this version of <code>ExampleSuite</code> with:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new ExampleSuite).run()
 * </pre>
 *
 * <p>
 * It will run only <code>testAddition</code> and report that <code>testSubtraction</code> was ignored. You'll see:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">ExampleSuite:
 * - testAddition</span>
 * <span class="stYellow">- testSubtraction !!! IGNORED !!!</span>
 * </pre>
 * 
 * <p>
 * <code>Ignore</code> is implemented as a tag. The <code>Filter</code> class effectively 
 * adds <code>org.scalatest.Ignore</code> to the <code>tagsToExclude</code> <code>Set</code> if it not already
 * in the <code>tagsToExclude</code> set passed to its primary constructor.  The only difference between
 * <code>org.scalatest.Ignore</code> and the tags you may define and exclude is that ScalaTest reports
 * ignored tests to the <code>Reporter</code>. The reason ScalaTest reports ignored tests is 
 * to encourage ignored tests to be eventually fixed and added back into the active suite of tests.
 * </p>
 *
 * <h2>Pending tests</h2>
 *
 * <p>
 * A <em>pending test</em> is one that has been given a name but is not yet implemented. The purpose of
 * pending tests is to facilitate a style of testing in which documentation of behavior is sketched
 * out before tests are written to verify that behavior (and often, before the behavior of
 * the system being tested is itself implemented). Such sketches form a kind of specification of
 * what tests and functionality to implement later.
 * </p>
 *
 * <p>
 * To support this style of testing, a test can be given a name that specifies one
 * bit of behavior required by the system being tested. The test can also include some code that
 * sends more information about the behavior to the reporter when the tests run. At the end of the test,
 * it can call method <code>pending</code>, which will cause it to complete abruptly with <code>TestPendingException</code>.
 * </p>
 *
 * <p>
 * Because tests in ScalaTest can be designated as pending with <code>TestPendingException</code>, both the test name and any information
 * sent to the reporter when running the test can appear in the report of a test run. (In other words,
 * the code of a pending test is executed just like any other test.) However, because the test completes abruptly
 * with <code>TestPendingException</code>, the test will be reported as pending, to indicate
 * the actual test, and possibly the functionality it is intended to test, has not yet been implemented.
 * </p>
 *
 * <p>
 * Although pending tests may be used more often in specification-style suites, such as
 * <code>org.scalatest.Spec</code>, you can also use it in <code>Suite</code>, like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 *
 * class ExampleSuite extends Suite {
 *
 *   def testAddition {
 *     val sum = 1 + 1
 *     assert(sum === 2)
 *   }
 *
 *   def testSubtraction { pending }
 * }
 * </pre>
 *
 * <p>
 * If you run this version of <code>ExampleSuite</code> with:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new ExampleSuite).run()
 * </pre>
 *
 * <p>
 * It will run both tests but report that <code>testSubtraction</code> is pending. You'll see:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">ExampleSuite:
 * - testAddition</span>
 * <span class="stYellow">- testSubtraction (pending)</span>
 * </pre>
 * 
 * <h2>Informers</h2>
 *
 * <p>
 * One of the parameters to <code>run</code> is a <code>Reporter</code>, which
 * will collect and report information about the running suite of tests.
 * Information about suites and tests that were run, whether tests succeeded or failed, 
 * and tests that were ignored will be passed to the <code>Reporter</code> as the suite runs.
 * Most often the reporting done by default by <code>Suite</code>'s methods will be sufficient, but
 * occasionally you may wish to provide custom information to the <code>Reporter</code> from a test method.
 * For this purpose, you can optionally include an <code>Informer</code> parameter in a test method, and then
 * pass the extra information to the <code>Informer</code> via its <code>apply</code> method. The <code>Informer</code>
 * will then pass the information to the <code>Reporter</code> by sending an <code>InfoProvided</code> event.
 * Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest._
 * 
 * class ExampleSuite extends Suite {
 *
 *   def testAddition(info: Informer) {
 *     assert(1 + 1 === 2)
 *     info("Addition seems to work")
 *   }
 * }
 * </pre>
 *
 * If you run this <code>Suite</code> from the interpreter, you will see the message
 * included in the printed report:
 *
 * <pre class="stREPL">
 * scala> (new ExampleSuite).run()
 * <span class="stGreen">ExampleSuite:
 * - testAddition(Informer)
 *   + Addition seems to work </span>
 * </pre>
 *
 * <h2>Executing suites in parallel</h2>
 *
 * <p>
 * The <code>run</code> method takes as one of its parameters an optional <code>Distributor</code>. If 
 * a <code>Distributor</code> is passed in, this trait's implementation of <code>run</code> puts its nested
 * <code>Suite</code>s into the distributor rather than executing them directly. The caller of <code>run</code>
 * is responsible for ensuring that some entity runs the <code>Suite</code>s placed into the 
 * distributor. The <code>-c</code> command line parameter to <code>Runner</code>, for example, will cause
 * <code>Suite</code>s put into the <code>Distributor</code> to be run in parallel via a pool of threads.
 * </p>
 *
 * <a name="TaggingTests"></a><h2>Tagging tests</h2>
 *
 * <p>
 * A <code>Suite</code>'s tests may be classified into groups by <em>tagging</em> them with string names. When executing
 * a <code>Suite</code>, groups of tests can optionally be included and/or excluded. In this
 * trait's implementation, tags are indicated by annotations attached to the test method. To
 * create a new tag type to use in <code>Suite</code>s, simply define a new Java annotation that itself is annotated with the <code>org.scalatest.TagAnnotation</code> annotation.
 * (Currently, for annotations to be
 * visible in Scala programs via Java reflection, the annotations themselves must be written in Java.) For example,
 * to create a tag named <code>SlowAsMolasses</code>, to use to mark slow tests, you would
 * write in Java:
 * </p>
 *
 * <p><b>Because of a Scaladoc bug in Scala 2.8, I had to put a space after the at sign in one the target annotation example below. If you
 * want to copy and paste from this example, you'll need to remove the space by hand.  - Bill Venners</b></p>
 *
 * <pre>
 * import java.lang.annotation.*; 
 * import org.scalatest.TagAnnotation
 * 
 * @TagAnnotation
 * @Retention(RetentionPolicy.RUNTIME)
 * @ Target({ElementType.METHOD, ElementType.TYPE})
 * public @interface SlowAsMolasses {}
 * </pre>
 *
 * <p>
 * Given this new annotation, you could place a <code>Suite</code> test method into the <code>SlowAsMolasses</code> group
 * (<em>i.e.</em>, tag it as being <code>SlowAsMolasses</code>) like this:
 * </p>
 *
 * <pre class="stHighlight">
 * @SlowAsMolasses
 * def testSleeping { sleep(1000000) }
 * </pre>
 *
 * <p>
 * The <code>run</code> method takes a <code>Filter</code>, whose constructor takes an optional
 * <code>Set[String]</code> called <code>tagsToInclude</code> and a <code>Set[String]</code> called
 * <code>tagsToExclude</code>. If <code>tagsToInclude</code> is <code>None</code>, all tests will be run
 * except those those belonging to tags listed in the
 * <code>tagsToExclude</code> <code>Set</code>. If <code>tagsToInclude</code> is defined, only tests
 * belonging to tags mentioned in the <code>tagsToInclude</code> set, and not mentioned in <code>tagsToExclude</code>,
 * will be run.
 * </p>
 *
 * <a name="sharedFixtures"></a><h2>Shared fixtures</h2>
 *
 * <p>
 * A test <em>fixture</em> is objects or other artifacts (such as files, sockets, database
 * connections, <em>etc.</em>) used by tests to do their work.
 * If a fixture is used by only one test method, then the definitions of the fixture objects can
 * be local to the method, such as the objects assigned to <code>sum</code> and <code>diff</code> in the
 * previous <code>ExampleSuite</code> examples. If multiple methods need to share an immutable fixture, one approach
 * is to assign them to instance variables.
 * </p>
 *
 * <p>
 * In some cases, however, shared <em>mutable</em> fixture objects may be changed by test methods such that
 * they need to be recreated or reinitialized before each test. Shared resources such
 * as files or database connections may also need to 
 * be created and initialized before, and cleaned up after, each test. JUnit 3 offered methods <code>setUp</code> and
 * <code>tearDown</code> for this purpose. In ScalaTest, you can use the <code>BeforeAndAfterEach</code> trait,
 * which will be described later, to implement an approach similar to JUnit's <code>setUp</code>
 * and <code>tearDown</code>, however, this approach usually involves reassigning <code>var</code>s or mutating objects
 * between tests. Before going that route, you may wish to consider some more functional approaches that
 * avoid side effects.
 * </p>
 *
 * <h4>Calling create-fixture methods</h4>
 *
 * <p>
 * One approach is to write one or more <em>create-fixture</em> methods
 * that return a new instance of a needed fixture object (or an holder object containing multiple needed fixture objects) each time it
 * is called. You can then call a create-fixture method at the beginning of each
 * test method that needs the fixture, storing the returned object or objects in local variables. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 * import collection.mutable.ListBuffer
 *
 * class ExampleSuite extends Suite {
 * 
 *   def fixture =
 *     new {
 *       val builder = new StringBuilder("ScalaTest is ")
 *       val buffer = new ListBuffer[String]
 *     }
 * 
 *   def testEasy {
 *     val f = fixture
 *     f.builder.append("easy!")
 *     assert(f.builder.toString === "ScalaTest is easy!")
 *     assert(f.buffer.isEmpty)
 *     f.buffer += "sweet"
 *   }
 * 
 *   def testFun {
 *     val f = fixture
 *     f.builder.append("fun!")
 *     assert(f.builder.toString === "ScalaTest is fun!")
 *     assert(f.buffer.isEmpty)
 *   }
 * }
 * </pre>
 *
 * <p>
 * The &ldquo;<code>f.</code>&rdquo; in front of each use of a fixture object provides a visual indication of which objects 
 * are part of the fixture, but if you prefer, you can import the the members with &ldquo;<code>import f._</code>&rdquo; and use the names directly.
 * </p>
 *
 * <h4>Instantiating fixture traits</h4>
 *
 * <p>
 * A related technique is to place
 * the fixture objects in a <em>fixture trait</em> and run your test code in the context of a new anonymous class instance that mixes in
 * the fixture trait, like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSuite extends Suite {
 * 
 *   trait Fixture {
 *     val builder = new StringBuilder("ScalaTest is ")
 *     val buffer = new ListBuffer[String]
 *   }
 * 
 *   def testEasy {
 *     new Fixture {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 *   }
 * 
 *   def testFun {
 *     new Fixture {
 *       builder.append("fun!")
 *       assert(builder.toString === "ScalaTest is fun!")
 *       assert(buffer.isEmpty)
 *     }
 *   }
 * }
 * </pre>
 *
 * <h4>Mixing in <code>OneInstancePerTest</code></h4>
 *
 * <p>
 * If every test method requires the same set of
 * mutable fixture objects, one other approach you can take is make them simply <code>val</code>s and mix in trait
 * <a href="OneInstancePerTest.html"><code>OneInstancePerTest</code></a>.  If you mix in <code>OneInstancePerTest</code>, each test
 * will be run in its own instance of the <code>Suite</code>, similar to the way JUnit tests are executed. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 * import org.scalatest.OneInstancePerTest
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSuite extends Suite with OneInstancePerTest {
 * 
 *   val builder = new StringBuilder("ScalaTest is ")
 *   val buffer = new ListBuffer[String]
 * 
 *   def testEasy {
 *     builder.append("easy!")
 *     assert(builder.toString === "ScalaTest is easy!")
 *     assert(buffer.isEmpty)
 *     buffer += "sweet"
 *   }
 * 
 *   def testFun {
 *     builder.append("fun!")
 *     assert(builder.toString === "ScalaTest is fun!")
 *     assert(buffer.isEmpty)
 *   }
 * }
 * </pre>
 *
 * <p>
 * Although the create-fixture, fixture-trait, and <code>OneInstancePerTest</code> approaches take care of setting up a fixture before each
 * test, they don't address the problem of cleaning up a fixture after the test completes. In this situation, you'll need to either
 * use side effects or the <em>loan pattern</em>.
 * </p>
 *
 * <h4>Mixing in <code>BeforeAndAfter</code></h4>
 *
 * <p>
 * One way to use side effects is to mix in the <a href="BeforeAndAfter.html"><code>BeforeAndAfter</code></a> trait.
 * With this trait you can denote a bit of code to run before each test with <code>before</code> and/or after each test
 * each test with <code>after</code>, like this:
 * </p>
 * 
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 * import org.scalatest.BeforeAndAfter
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSuite extends Suite with BeforeAndAfter {
 * 
 *   val builder = new StringBuilder
 *   val buffer = new ListBuffer[String]
 * 
 *   before {
 *     builder.append("ScalaTest is ")
 *   }
 * 
 *   after {
 *     builder.clear()
 *     buffer.clear()
 *   }
 * 
 *   def testEasy {
 *     builder.append("easy!")
 *     assert(builder.toString === "ScalaTest is easy!")
 *     assert(buffer.isEmpty)
 *     buffer += "sweet"
 *   }
 * 
 *   def testFun {
 *     builder.append("fun!")
 *     assert(builder.toString === "ScalaTest is fun!")
 *     assert(buffer.isEmpty)
 *   }
 * }
 * </pre>
 * 
 * <h4>Overriding <code>withFixture(NoArgTest)</code></h4>
 *
 * <p>
 * An alternate way to take care of setup and cleanup via side effects
 * is to override <code>withFixture</code>. Trait <code>Suite</code>'s implementation of
 * <code>runTest</code> passes a no-arg test function to <code>withFixture</code>. It is <code>withFixture</code>'s
 * responsibility to invoke that test function.  <code>Suite</code>'s implementation of <code>withFixture</code> simply
 * invokes the function, like this:
 * </p>
 *
 * <pre class="stHighlight">
 * // Default implementation
 * protected def withFixture(test: NoArgTest) {
 *   test()
 * }
 * </pre>
 *
 * <p>
 * You can, therefore, override <code>withFixture</code> to perform setup before, and cleanup after, invoking the test function. If
 * you have cleanup to perform, you should invoke the test function
 * inside a <code>try</code> block and perform the cleanup in a <code>finally</code> clause.
 * Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 * import collection.mutable.ListBuffer
 *
 * class ExampleSuite extends Suite {
 *
 *   val builder = new StringBuilder
 *   val buffer = new ListBuffer[String]
 *
 *   override def withFixture(test: NoArgTest) {
 *     builder.append("ScalaTest is ") // perform setup
 *     try {
 *       test() // invoke the test function
 *     }
 *     finally {
 *       builder.clear() // perform cleanup
 *       buffer.clear()
 *     }
 *   }
 *
 *   def testEasy {
 *     builder.append("easy!")
 *     assert(builder.toString === "ScalaTest is easy!")
 *     assert(buffer.isEmpty)
 *     buffer += "sweet"
 *   }
 *
 *   def testFun {
 *     builder.append("fun!")
 *     assert(builder.toString === "ScalaTest is fun!")
 *     assert(buffer.isEmpty)
 *     buffer += "clear"
 *   }
 * }
 * </pre>
 *
 * <p>
 * Note that the <a href="Suite$NoArgTest.html"><code>NoArgTest</code></a> passed to <code>withFixture</code>, in addition to
 * an <code>apply</code> method that executes the test, also includes the test name as well as the <a href="#configMapSection">config
 * map</a> passed to <code>runTest</code>. Thus you can also use the test name and configuration objects in <code>withFixture</code>.
 * </p>
 *
 * <p>
 * The reason you should perform cleanup in a <code>finally</code> clause is that <code>withFixture</code> is called by
 * <code>runTest</code>, which expects an exception to be thrown to indicate a failed test. Thus when you invoke
 * the <code>test</code> function inside <code>withFixture</code>, it may complete abruptly with an exception. The <code>finally</code>
 * clause will ensure the fixture cleanup happens as that exception propagates back up the call stack to <code>runTest</code>.
 * </p>
 *
 * <h4>Overriding <code>withFixture(OneArgTest)</code></h4>
 *
 * <p>
 * To use the loan pattern, you can extend <code>FixtureSuite</code> (from the <code>org.scalatest.fixture</code> package) instead of
 * <code>Suite</code>. Each test in a <code>FixtureSuite</code> takes a fixture as a parameter, allowing you to pass the fixture into
 * the test. You must indicate the type of the fixture parameter by specifying <code>FixtureParam</code>, and implement a
 * <code>withFixture</code> method that takes a <code>OneArgTest</code>. This <code>withFixture</code> method is responsible for
 * invoking the one-arg test function, so you can perform fixture set up before, and clean up after, invoking and passing
 * the fixture into the test function. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.fixture.FixtureSuite
 * import java.io.FileWriter
 * import java.io.File
 * 
 * class ExampleSuite extends FixtureSuite {
 * 
 *   final val tmpFile = "temp.txt"
 * 
 *   type FixtureParam = FileWriter
 * 
 *   def withFixture(test: OneArgTest) {
 * 
 *     val writer = new FileWriter(tmpFile) // set up the fixture
 *     try {
 *       test(writer) // "loan" the fixture to the test
 *     }
 *     finally {
 *       writer.close() // clean up the fixture
 *     }
 *   }
 * 
 *   def testEasy(writer: FileWriter) {
 *     writer.write("Hello, test!")
 *     writer.flush()
 *     assert(new File(tmpFile).length === 12)
 *   }
 * 
 *   def testFun(writer: FileWriter) {
 *     writer.write("Hi, test!")
 *     writer.flush()
 *     assert(new File(tmpFile).length === 9)
 *   }
 * }
 * </pre>
 *
 * <p>
 * For more information, see the <a href="fixture/FixtureSuite.html">documentation for <code>FixtureSuite</code></a>.
 * </p>
 *
 * <a name="differentFixtures"></a><h2>Providing different fixtures to different tests</h2>
 * 
 * <p>
 * If different tests in the same <code>Suite</code> require different fixtures, you can combine the previous techniques and
 * provide each test with just the fixture or fixtures it needs. Here's an example in which a <code>StringBuilder</code> and a
 * <code>ListBuffer</code> are provided via fixture traits, and file writer (that requires cleanup) is provided via the loan pattern:
 * </p>
 *
 * <pre class="stHighlight">
 * import java.io.FileWriter
 * import java.io.File
 * import collection.mutable.ListBuffer
 * import org.scalatest.Suite
 * 
 * class ExampleSuite extends Suite {
 * 
 *   final val tmpFile = "temp.txt"
 * 
 *   trait Builder {
 *     val builder = new StringBuilder("ScalaTest is ")
 *   }
 * 
 *   trait Buffer {
 *     val buffer = ListBuffer("ScalaTest", "is")
 *   }
 * 
 *   def withWriter(testCode: FileWriter => Any) {
 *     val writer = new FileWriter(tmpFile) // set up the fixture
 *     try {
 *       testCode(writer) // "loan" the fixture to the test
 *     }
 *     finally {
 *       writer.close() // clean up the fixture
 *     }
 *   }
 * 
 *   def testProductive { // This test needs the StringBuilder fixture
 *     new Builder {
 *       builder.append("productive!")
 *       assert(builder.toString === "ScalaTest is productive!")
 *     }
 *   }
 * 
 *   def testReadable { // This test needs the ListBuffer[String] fixture
 *     new Buffer {
 *       buffer += ("readable!")
 *       assert(buffer === List("ScalaTest", "is", "readable!"))
 *     }
 *   }
 * 
 *   def testFriendly { // This test needs the FileWriter fixture
 *     withWriter { writer =>
 *       writer.write("Hello, user!")
 *       writer.flush()
 *       assert(new File(tmpFile).length === 12)
 *     }
 *   }
 * 
 *   def testClearAndConcise { // This test needs the StringBuilder and ListBuffer
 *     new Builder with Buffer {
 *       builder.append("clear!")
 *       buffer += ("concise!")
 *       assert(builder.toString === "ScalaTest is clear!")
 *       assert(buffer === List("ScalaTest", "is", "concise!"))
 *     }
 *   }
 * 
 *   def testComposable { // This test needs all three fixtures
 *     new Builder with Buffer {
 *       builder.append("clear!")
 *       buffer += ("concise!")
 *       assert(builder.toString === "ScalaTest is clear!")
 *       assert(buffer === List("ScalaTest", "is", "concise!"))
 *       withWriter { writer =>
 *         writer.write(builder.toString)
 *         writer.flush()
 *         assert(new File(tmpFile).length === 19)
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * In the previous example, <code>testProductive</code> uses only the <code>StringBuilder</code> fixture, so it just instantiates
 * a <code>new Builder</code>, whereas <code>testReadable</code> uses only the <code>ListBuffer</code> fixture, so it just intantiates
 * a <code>new Buffer</code>. <code>testFriendly</code> needs just the <code>FileWriter</code> fixture, so it invokes
 * <code>withWriter</code>, which prepares and passes a <code>FileWriter</code> to the test (and takes care of closing it afterwords).
 * </p>
 *
 * <p>
 * Two tests need multiple fixtures: <code>testClearAndConcise</code> needs both the <code>StringBuilder</code> and the
 * <code>ListBuffer</code>, so it instantiates a class that mixes in both fixture traits with <code>new Builder with Buffer</code>.
 * <code>testComposable</code> needs all three fixtures, so in addition to <code>new Builder with Buffer</code> it also invokes
 * <code>withWriter</code>, wrapping just the of the test code that needs the fixture.
 * </p>
 *
 * <p>
 * Note that in this case, the loan pattern is being implemented via the <code>withWriter</code> method that takes a function, not
 * by overriding <code>FixtureSuite</code>'s <code>withFixture(OneArgTest)</code> method. <code>FixtureSuite</code> makes the most sense
 * if all (or at least most) tests need the same fixture, whereas in this <code>Suite</code> only two tests need the
 * <code>FileWriter</code>.
 * </p>
 *
 * <p>
 * Note also that two test methods, <code>testFriendly</code> and <code>testComposable</code>, are declared as parameterless methods even
 * though they have a side effect. In production code you would normally declare these as <em>empty-paren</em> methods, and call them with
 * empty parentheses, to make it more obvious to readers of the code that they have a side effect. Whether or not a test method has
 * a side effect, however, is a less important distinction than it is for methods in production code. Moreover, test methods are not
 * normally invoked directly by client code, but rather through reflection by running the <code>Suite</code> that contains them, so a
 * lack of parentheses on an invocation of a side-effecting test method would not normally appear in any client code. Given the empty
 * parentheses do not add much value in the test methods case, the recommended style is to simply always leave them off.
 * </p>
 *
 * <p>
 * In the previous example, the <code>withWriter</code> method passed an object into
 * the tests. Passing fixture objects into tests is generally a good idea when possible, but sometimes a side affect is unavoidable.
 * For example, if you need to initialize a database running on a server across a network, your with-fixture 
 * method will likely have nothing to pass. In such cases, simply create a with-fixture method that takes a by-name parameter and
 * performs setup and cleanup via side effects, like this:
 * </p>
 *
 * <pre class="stHighlight">
 * def withDataInDatabase(test: => Any) {
 *   // initialize the database across the network
 *   try {
 *     test // "loan" the initialized database to the test
 *   }
 *   finally {
 *     // clean up the database
 *   }
 * }
 * </pre>
 * 
 * <p>
 * You can then use it like:
 * </p>
 * 
 * <pre class="stHighlight">
 * def testUserLogsIn {
 *   withDataInDatabase {
 *     // test user logging in scenario
 *   }
 * }
 * </pre>
 * 
 * <a name="composingFixtures"></a><h2>Composing stackable fixture traits</h2>
 *
 * <p>
 * In larger projects, teams often end up with several different fixtures that test classes need in different combinations,
 * and possibly initialized (and cleaned up) in different orders. A good way to accomplish this in ScalaTest is to factor the individual
 * fixtures into traits that can be composed using the <em>stackable trait</em> pattern. This can be done, for example, by placing
 * <code>withFixture</code> methods in several traits, each of which call <code>super.withFixture</code>. Here's an example in
 * which the <code>StringBuilder</code> and <code>ListBuffer[String]</code> fixtures used in the previous examples have been
 * factored out into two <em>stackable fixture traits</em> named <code>Builder</code> and <code>Buffer</code>:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 * import org.scalatest.AbstractSuite
 * import collection.mutable.ListBuffer
 * 
 * trait Builder extends AbstractSuite { this: Suite =>
 * 
 *   val builder = new StringBuilder
 * 
 *   abstract override def withFixture(test: NoArgTest) {
 *     builder.append("ScalaTest is ")
 *     try {
 *       super.withFixture(test) // To be stackable, must call super.withFixture
 *     }
 *     finally {
 *       builder.clear()
 *     }
 *   }
 * }
 * 
 * trait Buffer extends AbstractSuite { this: Suite =>
 * 
 *   val buffer = new ListBuffer[String]
 * 
 *   abstract override def withFixture(test: NoArgTest) {
 *     try {
 *       super.withFixture(test) // To be stackable, must call super.withFixture
 *     }
 *     finally {
 *       buffer.clear()
 *     }
 *   }
 * }
 * 
 * class ExampleSuite extends Suite with Builder with Buffer {
 * 
 *   def testEasy {
 *     builder.append("easy!")
 *     assert(builder.toString === "ScalaTest is easy!")
 *     assert(buffer.isEmpty)
 *     buffer += "sweet"
 *   }
 * 
 *   def testFun {
 *     builder.append("fun!")
 *     assert(builder.toString === "ScalaTest is fun!")
 *     assert(buffer.isEmpty)
 *     buffer += "clear"
 *   }
 * }
 * </pre>
 *
 * <p>
 * By mixing in both the <code>Builder</code> and <code>Buffer</code> traits, <code>ExampleSuite</code> gets both fixtures, which will be
 * initialized before each test and cleaned up after. The order the traits are mixed together determines the order of execution.
 * In this case, <code>Builder</code> is "super" to </code>Buffer</code>. If you wanted <code>Buffer</code> to be "super"
 * to <code>Builder</code>, you need only switch the order you mix them together, like this: 
 * </p>
 *
 * <pre class="stHighlight">
 * class Example2Suite extends Suite with Buffer with Builder
 * </pre>
 *
 * <p>
 * And if you only need one fixture you mix in only that trait:
 * </p>
 *
 * <pre class="stHighlight">
 * class Example3Suite extends Suite with Builder
 * </pre>
 *
 * <p>
 * Another way to create stackable fixture traits is by extending the <a href="BeforeAndAfterEach.html"><code>BeforeAndAfterEach</code></a>
 * and/or <a href="BeforeAndAfterAll.html"><code>BeforeAndAfterAll</code></a> traits.
 * <code>BeforeAndAfterEach</code> has a <code>beforeEach</code> method that will be run before each test (like JUnit's <code>setUp</code>),
 * and an <code>afterEach</code> method that will be run after (like JUnit's <code>tearDown</code>).
 * Similarly, <code>BeforeAndAfterAll</code> has a <code>beforeAll</code> method that will be run before all tests,
 * and an <code>afterAll</code> method that will be run after all tests. Here's what the previously shown example would look like if it
 * were rewritten to use the <code>BeforeAndAfterEach</code> methods instead of <code>withFixture</code>:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Suite
 * import org.scalatest.BeforeAndAfterEach
 * import collection.mutable.ListBuffer
 * 
 * trait Builder extends BeforeAndAfterEach { this: Suite =>
 * 
 *   val builder = new StringBuilder
 * 
 *   override def beforeEach() {
 *     builder.append("ScalaTest is ")
 *     super.beforeEach() // To be stackable, must call super.beforeEach
 *   }
 * 
 *   override def afterEach() {
 *     try {
 *       super.afterEach() // To be stackable, must call super.afterEach
 *     }
 *     finally {
 *       builder.clear()
 *     }
 *   }
 * }
 * 
 * trait Buffer extends BeforeAndAfterEach { this: Suite =>
 * 
 *   val buffer = new ListBuffer[String]
 * 
 *   override def afterEach() {
 *     try {
 *       super.afterEach() // To be stackable, must call super.afterEach
 *     }
 *     finally {
 *       buffer.clear()
 *     }
 *   }
 * }
 * 
 * class ExampleSuite extends Suite with Builder with Buffer {
 * 
 *   def testEasy {
 *     builder.append("easy!")
 *     assert(builder.toString === "ScalaTest is easy!")
 *     assert(buffer.isEmpty)
 *     buffer += "sweet"
 *   }
 * 
 *   def testFun {
 *     builder.append("fun!")
 *     assert(builder.toString === "ScalaTest is fun!")
 *     assert(buffer.isEmpty)
 *     buffer += "clear"
 *   }
 * }
 * </pre>
 *
 * <p>
 * To get the same ordering as <code>withFixture</code>, place your <code>super.beforeEach</code> call at the end of each
 * <code>beforeEach</code> method, and the <code>super.afterEach</code> call at the beginning of each <code>afterEach</code>
 * method, as shown in the previous example. It is a good idea to invoke <code>super.afterEach</code> in a <code>try</code>
 * block and perform cleanup in a <code>finally</code> clause, as shown in the previous example, because this ensures the
 * cleanup code is performed even if <code>super.afterAll</code> throws an exception.
 * </p>
 *
 * <p>
 * One difference to bear in mind between the before-and-after traits and the <code>withFixture</code> methods, is that if
 * a <code>withFixture</code> method completes abruptly with an exception, it is considered a failed test. By contrast, if any of the
 * methods on the before-and-after traits (<em>i.e.</em>, <code>before</code>  and <code>after</code> of <code>BeforeAndAfter</code>,
 * <code>beforeEach</code> and <code>afterEach</code> of <code>BeforeAndAfterEach</code>,
 * and <code>beforeAll</code> and <code>afterAll</code> of <code>BeforeAndAfterAll</code>) complete abruptly, it is considered a
 * failed suite, which will result in a <a href="events/SuiteAborted.html"><code>SuiteAborted</code></a> event.
 * </p>
 * 
 * <a name="errorHandling"></a>
 * <h2>Treatment of <code>java.lang.Error</code>s</h2>
 *
 * <p>
 * The Javadoc documentation for <code>java.lang.Error</code> states:
 * </p>
 *
 * <blockquote>
 * An <code>Error</code> is a subclass of <code>Throwable</code> that indicates serious problems that a reasonable application should not try to catch. Most
 * such errors are abnormal conditions.
 * </blockquote>
 *
 * <p>
 * Because <code>Error</code>s are used to denote serious errors, trait <code>Suite</code> and its subtypes in the ScalaTest API do not always treat a test
 * that completes abruptly with an <code>Error</code> as a test failure, but sometimes as an indication that serious problems
 * have arisen that should cause the run to abort. For example, if a test completes abruptly with an <code>OutOfMemoryError</code>, 
 * it will not be reported as a test failure, but will instead cause the run to abort. Because not everyone uses <code>Error</code>s only to represent serious
 * problems, however, ScalaTest only behaves this way for the following exception types (and their subclasses):
 * </p>
 *
 * <ul>
 * <li><code>java.lang.annotation.AnnotationFormatError</code></li>
 * <li><code>java.awt.AWTError</code></li>
 * <li><code>java.nio.charset.CoderMalfunctionError</code></li>
 * <li><code>javax.xml.parsers.FactoryConfigurationError</code></li>
 * <li><code>java.lang.LinkageError</code></li>
 * <li><code>java.lang.ThreadDeath</code></li>
 * <li><code>javax.xml.transform.TransformerFactoryConfigurationError</code></li>
 * <li><code>java.lang.VirtualMachineError</code></li>
 * </ul>
 *
 * <p>
 * The previous list includes all <code>Error</code>s that exist as part of Java 1.5 API, excluding <code>java.lang.AssertionError</code>. ScalaTest
 * does treat a thrown <code>AssertionError</code> as an indication of a test failure. In addition, any other <code>Error</code> that is not an instance of a
 * type mentioned in the previous list will be caught by the <code>Suite</code> traits in the ScalaTest API and reported as the cause of a test failure. 
 * </p>
 *
 * <p>
 * Although trait <code>Suite</code> and all its subtypes in the ScalaTest API consistently behave this way with regard to <code>Error</code>s,
 * this behavior is not required by the contract of <code>Suite</code>. Subclasses and subtraits that you define, for example, may treat all
 * <code>Error</code>s as test failures, or indicate errors in some other way that has nothing to do with exceptions.
 * </p>
 *
 * <h2>Extensibility</h2>
 *
 * <p>
 * Trait <code>Suite</code> provides default implementations of its methods that should
 * be sufficient for most applications, but many methods can be overridden when desired. Here's
 * a summary of the methods that are intended to be overridden:
 * </p>
 *
 * <ul>
 * <li><code>run</code> - override this method to define custom ways to run suites of
 *   tests.</li>
 * <li><code>runNestedSuites</code> - override this method to define custom ways to run nested suites.</li>
 * <li><code>runTests</code> - override this method to define custom ways to run a suite's tests.</li>
 * <li><code>runTest</code> - override this method to define custom ways to run a single named test.</li>
 * <li><code>testNames</code> - override this method to specify the <code>Suite</code>'s test names in a custom way.</li>
 * <li><code>tags</code> - override this method to specify the <code>Suite</code>'s test tags in a custom way.</li>
 * <li><code>nestedSuites</code> - override this method to specify the <code>Suite</code>'s nested <code>Suite</code>s in a custom way.</li>
 * <li><code>suiteName</code> - override this method to specify the <code>Suite</code>'s name in a custom way.</li>
 * <li><code>expectedTestCount</code> - override this method to count this <code>Suite</code>'s expected tests in a custom way.</li>
 * </ul>
 *
 * <p>
 * For example, this trait's implementation of <code>testNames</code> performs reflection to discover methods starting with <code>test</code>,
 * and places these in a <code>Set</code> whose iterator returns the names in alphabetical order. If you wish to run tests in a different
 * order in a particular <code>Suite</code>, perhaps because a test named <code>testAlpha</code> can only succeed after a test named
 * <code>testBeta</code> has run, you can override <code>testNames</code> so that it returns a <code>Set</code> whose iterator returns
 * <code>testBeta</code> <em>before</em> <code>testAlpha</code>. (This trait's implementation of <code>run</code> will invoke tests
 * in the order they come out of the <code>testNames</code> <code>Set</code> iterator.)
 * </p>
 *
 * <p>
 * Alternatively, you may not like starting your test methods with <code>test</code>, and prefer using <code>@Test</code> annotations in
 * the style of Java's JUnit 4 or TestNG. If so, you can override <code>testNames</code> to discover tests using either of these two APIs
 * <code>@Test</code> annotations, or one of your own invention. (This is in fact
 * how <code>org.scalatest.junit.JUnitSuite</code> and <code>org.scalatest.testng.TestNGSuite</code> work.)
 * </p>
 *
 * <p>
 * Moreover, <em>test</em> in ScalaTest does not necessarily mean <em>test method</em>. A test can be anything that can be given a name,
 * that starts and either succeeds or fails, and can be ignored. In <code>org.scalatest.FunSuite</code>, for example, tests are represented
 * as function values. This
 * approach might look foreign to JUnit users, but may feel more natural to programmers with a functional programming background.
 * To facilitate this style of writing tests, <code>FunSuite</code> overrides <code>testNames</code>, <code>runTest</code>, and <code>run</code> such that you can 
 * define tests as function values.
 * </p>
 *
 * <p>
 * You can also model existing JUnit 3, JUnit 4, or TestNG tests as suites of tests, thereby incorporating tests written in Java into a ScalaTest suite.
 * The "wrapper" classes in packages <code>org.scalatest.junit</code> and <code>org.scalatest.testng</code> exist to make this easy.
 * No matter what legacy tests you may have, it is likely you can create or use an existing <code>Suite</code> subclass that allows you to model those tests
 * as ScalaTest suites and tests and incorporate them into a ScalaTest suite. You can then write new tests in Scala and continue supporting
 * older tests in Java.
 * </p>
 *
 * @author Bill Venners
 */
@serializable
trait Suite extends Assertions with AbstractSuite { thisSuite =>

  import Suite.TestMethodPrefix, Suite.InformerInParens, Suite.IgnoreAnnotation

  /**
   * A test function taking no arguments, which also provides a test name and config map.
   *
   * <p>
   * <code>Suite</code>'s implementation of <code>runTest</code> passes instances of this trait
   * to <code>withFixture</code> for every test method it executes. It invokes <code>withFixture</code>
   * for every test, including test methods that take an <code>Informer</code>. For the latter case,
   * the <code>Informer</code> to pass to the test method is already contained inside the
   * <code>NoArgTest</code> instance passed to <code>withFixture</code>.
   * </p>
   */
  protected trait NoArgTest extends (() => Unit) {

    /**
     * The name of this test.
     */
    def name: String

    /**
     * Runs the code of the test.
     */
    def apply()

    /**
     * A <code>Map[String, Any]</code> containing objects that can be used
     * to configure the fixture and test.
     */
    def configMap: Map[String, Any]
  }

  /**
  * A <code>List</code> of this <code>Suite</code> object's nested <code>Suite</code>s. If this <code>Suite</code> contains no nested <code>Suite</code>s,
  * this method returns an empty <code>List</code>. This trait's implementation of this method returns an empty <code>List</code>.
  */
  def nestedSuites: List[Suite] = Nil
  
  /**
   * Executes this <code>Suite</code>, printing results to the standard output.
   *
   * <p>
   * This method implementation calls <code>run</code> on this <code>Suite</code>, passing in:
   * </p>
   *
   * <ul>
   * <li><code>testName</code> - <code>None</code></li>
   * <li><code>reporter</code> - a reporter that prints to the standard output</li>
   * <li><code>stopper</code> - a <code>Stopper</code> whose <code>apply</code> method always returns <code>false</code></li>
   * <li><code>filter</code> - a <code>Filter</code> constructed with <code>None</code> for <code>tagsToInclude</code> and <code>Set()</code>
   *   for <code>tagsToExclude</code></li>
   * <li><code>configMap</code> - an empty <code>Map[String, Any]</code></li>
   * <li><code>distributor</code> - <code>None</code></li>
   * <li><code>tracker</code> - a new <code>Tracker</code></li>
   * </ul>
   *
   * <p>
   * This method serves as a convenient way to execute a <code>Suite</code>, especially from
   * within the Scala interpreter.
   * </p>
   *
   * <p>
   * Note:  In ScalaTest, the terms "execute" and "run" basically mean the same thing and
   * can be used interchangably. The reason this convenience method and its three overloaded forms
   * aren't named <code>run</code>
   * is because <code>junit.framework.TestCase</code> declares a <code>run</code> method
   * that takes no arguments but returns a <code>junit.framework.TestResult</code>. That
   * <code>run</code> method would not overload with this method if it were named <code>run</code>,
   * because it would have the same parameters but a different return type than the one
   * defined in <code>TestCase</code>. To facilitate integration with JUnit 3, therefore,
   * these convenience "run" methods are named <code>execute</code>. In particular, this allows trait
   * <code>org.scalatest.junit.JUnit3Suite</code> to extend both <code>org.scalatest.Suite</code> and
   * <code>junit.framework.TestCase</code>, which enables the creating of classes that
   * can be run with either ScalaTest or JUnit 3.
   * </p>
   *
  final def execute() {
    run(None, new StandardOutReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
  }

   * Executes this <code>Suite</code> with the specified <code>configMap</code>, printing results to the standard output.
   *
   * <p>
   * This method implementation calls <code>run</code> on this <code>Suite</code>, passing in:
   * </p>
   *
   * <ul>
   * <li><code>testName</code> - <code>None</code></li>
   * <li><code>reporter</code> - a reporter that prints to the standard output</li>
   * <li><code>stopper</code> - a <code>Stopper</code> whose <code>apply</code> method always returns <code>false</code></li>
   * <li><code>filter</code> - a <code>Filter</code> constructed with <code>None</code> for <code>tagsToInclude</code> and <code>Set()</code>
   *   for <code>tagsToExclude</code></li>
   * <li><code>configMap</code> - the specified <code>configMap</code> <code>Map[String, Any]</code></li>
   * <li><code>distributor</code> - <code>None</code></li>
   * <li><code>tracker</code> - a new <code>Tracker</code></li>
   * </ul>
   *
   * <p>
   * This method serves as a convenient way to execute a <code>Suite</code>, passing in some objects via the <code>configMap</code>, especially from within the Scala interpreter.
   * </p>
   *
   * <p>
   * Note:  In ScalaTest, the terms "execute" and "run" basically mean the same thing and
   * can be used interchangably. The reason this convenience method and its three overloaded forms
   * aren't named <code>run</code> is described the documentation of the overloaded form that
   * takes no parameters: <a href="#execute%28%29">execute()</a>.
   * </p>
   *
   * @param configMap a <code>Map</code> of key-value pairs that can be used by the executing <code>Suite</code> of tests.
   *
   * @throws NullPointerException if the passed <code>configMap</code> parameter is <code>null</code>.
   *
  final def execute(configMap: Map[String, Any]) {
    run(None, new StandardOutReporter, new Stopper {}, Filter(), configMap, None, new Tracker)
  }

   * Executes the test specified as <code>testName</code> in this <code>Suite</code>, printing results to the standard output.
   *
   * <p>
   * This method implementation calls <code>run</code> on this <code>Suite</code>, passing in:
   * </p>
   *
   * <ul>
   * <li><code>testName</code> - <code>Some(testName)</code></li>
   * <li><code>reporter</code> - a reporter that prints to the standard output</li>
   * <li><code>stopper</code> - a <code>Stopper</code> whose <code>apply</code> method always returns <code>false</code></li>
   * <li><code>filter</code> - a <code>Filter</code> constructed with <code>None</code> for <code>tagsToInclude</code> and <code>Set()</code>
   *   for <code>tagsToExclude</code></li>
   * <li><code>configMap</code> - an empty <code>Map[String, Any]</code></li>
   * <li><code>distributor</code> - <code>None</code></li>
   * <li><code>tracker</code> - a new <code>Tracker</code></li>
   * </ul>
   *
   * <p>
   * This method serves as a convenient way to run a single test, especially from within the Scala interpreter.
   * </p>
   *
   * <p>
   * Note:  In ScalaTest, the terms "execute" and "run" basically mean the same thing and
   * can be used interchangably. The reason this convenience method and its three overloaded forms
   * aren't named <code>run</code> is described the documentation of the overloaded form that
   * takes no parameters: <a href="#execute%28%29">execute()</a>.
   * </p>
   *
   * @param testName the name of one test to run.
   *
   * @throws NullPointerException if the passed <code>testName</code> parameter is <code>null</code>.
   * @throws IllegalArgumentException if <code>testName</code> is defined, but no test with the specified test name
   *     exists in this <code>Suite</code>
   *
  final def execute(testName: String) {
    run(Some(testName), new StandardOutReporter, new Stopper {}, Filter(), Map(), None, new Tracker)
  }
*/

  /**
   * Executes one or more tests in this <code>Suite</code>, printing results to the standard output.
   *
   * <p>
   * This method invokes <code>run</code> on itself, passing in values that can be configured via the parameters to this
   * method, all of which have default values. This behavior is convenient when working with ScalaTest in the Scala interpreter.
   * Here's a summary of this method's parameters and how you can use them:
   * </p>
   *
   * <p>
   * <strong>The <code>testName</code> parameter</strong>
   * </p>
   *
   * <p>
   * If you leave <code>testName</code> at its default value (of <code>null</code>), this method will pass <code>None</code> to
   * the <code>testName</code> parameter of <code>run</code>, and as a result all the tests in this suite will be executed. If you
   * specify a <code>testName</code>, this method will pass <code>Some(testName)</code> to <code>run</code>, and only that test
   * will be run. Thus to run all tests in a suite from the Scala interpreter, you can write:
   * </p>
   *
   * <pre class="stREPL">
   * scala> (new ExampleSuite).execute()
   * </pre>
   *
   * <p>
   * To run just the test named <code>"my favorite test"</code> in a suite from the Scala interpreter, you would write:
   * </p>
   *
   * <pre class="stREPL">
   * scala> (new ExampleSuite).execute("my favorite test")
   * </pre>
   *
   * <p>
   * Or:
   * </p>
   *
   * <pre class="stREPL">
   * scala> (new ExampleSuite).execute(testName = "my favorite test")
   * </pre>
   *
   * <p>
   * <strong>The <code>configMap</code> parameter</strong>
   * </p>
   *
   * <p>
   * If you provide a value for the <code>configMap</code> parameter, this method will pass it to <code>run</code>. If not, the default value
   * of an empty <code>Map</code> will be passed. For more information on how to use a config map to configure your test suites, see
   * the <a href="#configMapSection">config map section</a> in the main documentation for this trait. Here's an example in which you configure
   * a run with the name of an input file:
   * </p>
   *
   * <pre class="stREPL">
   * scala> (new ExampleSuite).execute(configMap = Map("inputFileName" -> "in.txt")
   * </pre>
   *
   * <p>
   * <strong>The <code>color</code> parameter</strong>
   * </p>
   *
   * <p>
   * If you leave the <code>color</code> parameter unspecified, this method will configure the reporter it passes to <code>run</code> to print
   * to the standard output in color (via ansi escape characters). If you don't want color output, specify false for <code>color</code>, like this:
   * </p>
   *
   * <pre class="stREPL">
   * scala> (new ExampleSuite).execute(color = false)
   * </pre>
   *
   * <p>
   * <strong>The <code>durations</code> parameter</strong>
   * </p>
   *
   * <p>
   * If you leave the <code>durations</code> parameter unspecified, this method will configure the reporter it passes to <code>run</code> to
   * <em>not</em> print durations for tests and suites to the standard output. If you want durations printed, specify true for <code>durations</code>,
   * like this:
   * </p>
   *
   * <pre class="stREPL">
   * scala> (new ExampleSuite).execute(durations = true)
   * </pre>
   *
   * <p>
   * <strong>The <code>shortstacks</code> and <code>fullstacks</code> parameters</strong>
   * </p>
   *
   * <p>
   * If you leave both the <code>shortstacks</code> and <code>fullstacks</code> parameters unspecified, this method will configure the reporter
   * it passes to <code>run</code> to <em>not</em> print stack traces for failed tests if it has a stack depth that identifies the offending
   * line of test code. If you prefer a short stack trace (10 to 15 stack frames) to be printed with any test failure, specify true for
   * <code>shortstacks</code>:
   * </p>
   *
   * <pre class="stREPL">
   * scala> (new ExampleSuite).execute(shortstacks = true)
   * </pre>
   *
   * <p>
   * For full stack traces, set <code>fullstacks</code> to true:
   * </p>
   *
   * <pre class="stREPL">
   * scala> (new ExampleSuite).execute(fullstacks = true)
   * </pre>
   *
   * <p>
   * If you specify true for both <code>shortstacks</code> and <code>fullstacks</code>, you'll get full stack traces.
   * </p>
   *
   * <p>
   * <strong>The <code>stats</code> parameter</strong>
   * </p>
   *
   * <p>
   * If you leave the <code>stats</code> parameter unspecified, this method will <em>not</em> fire <code>RunStarting</code> and either <code>RunCompleted</code>
   * or <code>RunAborted</code> events to the reporter it passes to <code>run</code>.
   * If you specify true for <code>stats</code>, this method will fire the run events to the reporter, and the reporter will print the
   * expected test count before the run, and various statistics after, including the number of suites completed and number of tests that
   * succeeded, failed, were ignored or marked pending. Here's how you get the stats:
   * </p>
   *
   * <pre class="stREPL">
   * scala> (new ExampleSuite).execute(stats = true)
   * </pre>
   *
   *
   * <p>
   * To summarize, this method will pass to <code>run</code>:
   * </p>
   * <ul>
   * <li><code>testName</code> - <code>None</code> if this method's <code>testName</code> parameter is left at its default value of <code>null</code>, else <code>Some(testName)</code>.
   * <li><code>reporter</code> - a reporter that prints to the standard output</li>
   * <li><code>stopper</code> - a <code>Stopper</code> whose <code>apply</code> method always returns <code>false</code></li>
   * <li><code>filter</code> - a <code>Filter</code> constructed with <code>None</code> for <code>tagsToInclude</code> and <code>Set()</code>
   *   for <code>tagsToExclude</code></li>
   * <li><code>configMap</code> - the <code>configMap</code> passed to this method</li>
   * <li><code>distributor</code> - <code>None</code></li>
   * <li><code>tracker</code> - a new <code>Tracker</code></li>
   * </ul>
   *
   * <p>
   * Note:  In ScalaTest, the terms "execute" and "run" basically mean the same thing and
   * can be used interchangably. The reason this method isn't named <code>run</code> is that it takes advantage of
   * default arguments, and you can't mix overloaded methods and default arguments in Scala. (If named <code>run</code>,
   * this method would have the same name but different arguments than the main <a href="#run"><code>run</code> method</a> that
   * takes seven arguments. Thus it would overload and couldn't be used with default argument values.)
   * </p>
   *
   * <p>
   * Design note: This method has two "features" that may seem unidiomatic. First, the default value of <code>testName</code> is <code>null</code>.
   * Normally in Scala the type of <code>testName</code> would be <code>Option[String]</code> and the default value would
   * be <code>None</code>, as it is in this trait's <code>run</code> method. The <code>null</code> value is used here for two reasons. First, in
   * ScalaTest 1.5, <code>execute</code> was changed from four overloaded methods to one method with default values, taking advantage of
   * the default and named parameters feature introduced in Scala 2.8.
   * To not break existing source code, <code>testName</code> needed to have type <code>String</code>, as it did in two of the overloaded
   * <code>execute</code> methods prior to 1.5. The other reason is that <code>execute</code> has always been designed to be called primarily
   * from an interpeter environment, such as the Scala REPL (Read-Evaluate-Print-Loop). In an interpreter environment, minimizing keystrokes is king.
   * A <code>String</code> type with a <code>null</code> default value lets users type <code>suite.execute("my test name")</code> rather than
   * <code>suite.execute(Some("my test name"))</code>, saving several keystrokes.
   * </p>
   *
   * <p>
   * The second non-idiomatic feature is that <code>shortstacks</code> and <code>fullstacks</code> are all lower case rather than
   * camel case. This is done to be consistent with the <a href="Shell.html"><code>Shell</code></a>, which also uses those forms. The reason 
   * lower case is used in the <code>Shell</code> is to save keystrokes in an interpreter environment.  Most Unix commands, for
   * example, are all lower case, making them easier and quicker to type.  In the ScalaTest
   * <code>Shell</code>, methods like <code>shortstacks</code>, <code>fullstacks</code>, and <code>nostats</code>, <em>etc.</em>, are 
   * designed to be all lower case so they feel more like shell commands than methods.
   * </p>
   *
   * @param testName the name of one test to run.
   * @param configMap a <code>Map</code> of key-value pairs that can be used by the executing <code>Suite</code> of tests.
   * @param color a boolean that configures whether output is printed in color
   * @param durations a boolean that configures whether test and suite durations are printed to the standard output
   * @param shortstacks a boolean that configures whether short stack traces should be printed for test failures
   * @param fullstacks a boolean that configures whether full stack traces should be printed for test failures
   * @param stats a boolean that configures whether test and suite statistics are printed to the standard output
   *
   * @throws NullPointerException if the passed <code>configMap</code> parameter is <code>null</code>.
   * @throws IllegalArgumentException if <code>testName</code> is defined, but no test with the specified test name
   *     exists in this <code>Suite</code>
   */
  final def execute(
    testName: String = null,
    configMap: Map[String, Any] = Map(),
    color: Boolean = true,
    durations: Boolean = false,
    shortstacks: Boolean = false,
    fullstacks: Boolean = false,
    stats: Boolean = false
  ) {
    if (configMap == null)
      throw new NullPointerException("configMap was null")
    if (testName != null && !testNames.contains(testName))
      throw new IllegalArgumentException(Resources("testNotFound", testName))

    val dispatch = new DispatchReporter(List(new StandardOutReporter(durations, color, shortstacks, fullstacks)))
    val tracker = new Tracker
    val filter = Filter()
    val runStartTime = System.currentTimeMillis
    if (stats)
      dispatch(RunStarting(tracker.nextOrdinal(), expectedTestCount(filter), configMap))

    val suiteStartTime = System.currentTimeMillis
    def dispatchSuiteAborted(e: Throwable) {
      val rawString = Resources("runOnSuiteException")
      val formatter = formatterForSuiteAborted(thisSuite, rawString)
      val duration = System.currentTimeMillis - suiteStartTime
      dispatch(SuiteAborted(tracker.nextOrdinal(), rawString, thisSuite.suiteName, Some(thisSuite.getClass.getName), Some(e), Some(duration), formatter, None))
    }

    try {

      val formatter = formatterForSuiteStarting(thisSuite)
      dispatch(SuiteStarting(tracker.nextOrdinal(), thisSuite.suiteName, Some(thisSuite.getClass.getName), formatter))

      run(
        //if (testName != null) Some(testName) else None,
        Option(testName),
        dispatch,
        new Stopper {},
        filter,
        configMap,
        None,
        tracker
      )

      val suiteCompletedFormatter = formatterForSuiteCompleted(thisSuite)
      val duration = System.currentTimeMillis - suiteStartTime
      dispatch(SuiteCompleted(tracker.nextOrdinal(), thisSuite.suiteName, Some(thisSuite.getClass.getName), Some(duration), suiteCompletedFormatter))
      if (stats) {
        val duration = System.currentTimeMillis - runStartTime
        dispatch(RunCompleted(tracker.nextOrdinal(), Some(duration)))
      }
    }
    catch {
      case e: InstantiationException =>
        dispatchSuiteAborted(e)
        dispatch(RunAborted(tracker.nextOrdinal(), Resources("cannotInstantiateSuite", e.getMessage), Some(e), Some(System.currentTimeMillis - runStartTime)))
      case e: IllegalAccessException =>
        dispatchSuiteAborted(e)
        dispatch(RunAborted(tracker.nextOrdinal(), Resources("cannotInstantiateSuite", e.getMessage), Some(e), Some(System.currentTimeMillis - runStartTime)))
      case e: NoClassDefFoundError =>
        dispatchSuiteAborted(e)
        dispatch(RunAborted(tracker.nextOrdinal(), Resources("cannotLoadClass", e.getMessage), Some(e), Some(System.currentTimeMillis - runStartTime)))
      case e: Throwable =>
        dispatchSuiteAborted(e)
        dispatch(RunAborted(tracker.nextOrdinal(), Resources.bigProblems(e), Some(e), Some(System.currentTimeMillis - runStartTime)))
    }
    finally {
      dispatch.dispatchDisposeAndWaitUntilDone()
    }
  }

  /**
   * A <code>Map</code> whose keys are <code>String</code> tag names with which tests in this <code>Suite</code> are marked, and
   * whose values are the <code>Set</code> of test names marked with each tag.  If this <code>Suite</code> contains no tags, this
   * method returns an empty <code>Map</code>.
   *
   * <p>
   * This trait's implementation of this method uses Java reflection to discover any Java annotations attached to its test methods. The
   * fully qualified name of each unique annotation that extends <code>TagAnnotation</code> is considered a tag. This trait's
   * implementation of this method, therefore, places one key/value pair into to the
   * <code>Map</code> for each unique tag annotation name discovered through reflection. The mapped value for each tag name key will contain
   * the test method name, as provided via the <code>testNames</code> method. 
   * </p>
   *
   * <p>
   * Subclasses may override this method to define and/or discover tags in a custom manner, but overriding method implementations
   * should never return an empty <code>Set</code> as a value. If a tag has no tests, its name should not appear as a key in the
   * returned <code>Map</code>.
   * </p>
   */
  def tags: Map[String, Set[String]] = {

    def getTags(testName: String) =
      for {
        a <- getMethodForTestName(testName).getDeclaredAnnotations
        annotationClass = a.annotationType
        if annotationClass.isAnnotationPresent(classOf[TagAnnotation])
      } yield annotationClass.getName

    val elements =
      for (testName <- testNames; if !getTags(testName).isEmpty)
        yield testName -> (Set() ++ getTags(testName))

    Map() ++ elements
  }

  /**
  * A <code>Set</code> of test names. If this <code>Suite</code> contains no tests, this method returns an empty <code>Set</code>.
  *
  * <p>
  * This trait's implementation of this method uses Java reflection to discover all public methods whose name starts with <code>"test"</code>,
  * which take either nothing or a single <code>Informer</code> as parameters. For each discovered test method, it assigns a test name
  * comprised of just the method name if the method takes no parameters, or the method name plus <code>(Informer)</code> if the
  * method takes a <code>Informer</code>. Here are a few method signatures and the names that this trait's implementation assigns them:
  * </p>
  *
  * <pre class="stHighlight">
  * def testCat() {}         // test name: "testCat"
  * def testCat(Informer) {} // test name: "testCat(Informer)"
  * def testDog() {}         // test name: "testDog"
  * def testDog(Informer) {} // test name: "testDog(Informer)"
  * def test() {}            // test name: "test"
  * def test(Informer) {}    // test name: "test(Informer)"
  * </pre>
  *
  * <p>
  * This trait's implementation of this method returns an immutable <code>Set</code> of all such names, excluding the name
  * <code>testNames</code>. The iterator obtained by invoking <code>elements</code> on this
  * returned <code>Set</code> will produce the test names in their <em>natural order</em>, as determined by <code>String</code>'s
  * <code>compareTo</code> method.
  * </p>
  *
  * <p>
  * This trait's implementation of <code>runTests</code> invokes this method
  * and calls <code>runTest</code> for each test name in the order they appear in the returned <code>Set</code>'s iterator.
  * Although this trait's implementation of this method returns a <code>Set</code> whose iterator produces <code>String</code>
  * test names in a well-defined order, the contract of this method does not required a defined order. Subclasses are free to
  * override this method and return test names in an undefined order, or in a defined order that's different from <code>String</code>'s
  * natural order.
  * </p>
  *
  * <p>
  * Subclasses may override this method to produce test names in a custom manner. One potential reason to override <code>testNames</code> is
  * to run tests in a different order, for example, to ensure that tests that depend on other tests are run after those other tests.
  * Another potential reason to override is allow tests to be defined in a different manner, such as methods annotated <code>@Test</code> annotations
  * (as is done in <code>JUnitSuite</code> and <code>TestNGSuite</code>) or test functions registered during construction (as is
  * done in <code>FunSuite</code> and <code>Spec</code>).
  * </p>
  */
  def testNames: Set[String] = {

    def isTestMethod(m: Method) = {

      // Factored out to share code with FixtureSuite.testNames
      val (isInstanceMethod, simpleName, firstFour, paramTypes, hasNoParams, isTestNames) = isTestMethodGoodies(m)

      isInstanceMethod && (firstFour == "test") && ((hasNoParams && !isTestNames) || takesInformer(m))
    }

    val testNameArray =
      for (m <- getClass.getMethods; if isTestMethod(m)) 
        yield if (takesInformer(m)) m.getName + InformerInParens else m.getName

    TreeSet[String]() ++ testNameArray
  }

  private[scalatest] def getMethodForTestName(testName: String) =
    try {
      getClass.getMethod(
        simpleNameForTest(testName),
        (if (testMethodTakesAnInformer(testName)) Array(classOf[Informer]) else new Array[Class[_]](0)): _*
      )
    }
    catch {
      case e: NoSuchMethodException =>
        throw new IllegalArgumentException(Resources("testNotFound", testName))
      case e =>
        throw e
    }

  /**
   *  Run the passed test function in the context of a fixture established by this method.
   *
   * <p>
   * This method should set up the fixture needed by the tests of the
   * current suite, invoke the test function, and if needed, perform any clean
   * up needed after the test completes. Because the <code>NoArgTest</code> function
   * passed to this method takes no parameters, preparing the fixture will require
   * side effects, such as reassigning instance <code>var</code>s in this <code>Suite</code> or initializing
   * a globally accessible external database. If you want to avoid reassigning instance <code>var</code>s
   * you can use <a href="fixture/FixtureSuite.html">FixtureSuite</a>.
   * </p>
   *
   * <p>
   * This trait's implementation of <code>runTest</code> invokes this method for each test, passing
   * in a <code>NoArgTest</code> whose <code>apply</code> method will execute the code of the test.
   * </p>
   *
   * <p>
   * This trait's implementation of this method simply invokes the passed <code>NoArgTest</code> function.
   * </p>
   *
   * @param test the no-arg test function to run with a fixture
   */
  protected def withFixture(test: NoArgTest) {
    test()
  }

  // Factored out to share this with FixtureSuite.runTest
  private[scalatest] def getSuiteRunTestGoodies(stopper: Stopper, reporter: Reporter, testName: String) = {
    val (stopRequested, report, hasPublicNoArgConstructor, rerunnable, testStartTime) = getRunTestGoodies(stopper, reporter, testName)
    val method = getMethodForTestName(testName)
    (stopRequested, report, method, hasPublicNoArgConstructor, rerunnable, testStartTime)
  }

  // Sharing this with FunSuite and FixtureFunSuite as well as Suite and FixtureSuite
  private[scalatest] def getRunTestGoodies(stopper: Stopper, reporter: Reporter, testName: String) = {

    val stopRequested = stopper
    val report = wrapReporterIfNecessary(reporter)

    // Create a Rerunner if the Suite has a no-arg constructor
    val hasPublicNoArgConstructor = checkForPublicNoArgConstructor(getClass)

    val rerunnable =
      if (hasPublicNoArgConstructor)
        Some(new TestRerunner(getClass.getName, testName))
      else
        None

    val testStartTime = System.currentTimeMillis

    (stopRequested, report, hasPublicNoArgConstructor, rerunnable, testStartTime)
  }

  /**
   * Run a test.
   *
   * <p>
   * This trait's implementation uses Java reflection to invoke on this object the test method identified by the passed <code>testName</code>.
   * </p>
   *
   * <p>
   * Implementations of this method are responsible for ensuring a <code>TestStarting</code> event
   * is fired to the <code>Reporter</code> before executing any test, and either <code>TestSucceeded</code>,
   * <code>TestFailed</code>, or <code>TestPending</code> after executing any nested
   * <code>Suite</code>. (If a test is marked with the <code>org.scalatest.Ignore</code> tag, the
   * <code>runTests</code> method is responsible for ensuring a <code>TestIgnored</code> event is fired and that
   * this <code>runTest</code> method is not invoked for that ignored test.)
   * </p>
   *
   * @param testName the name of one test to run.
   * @param reporter the <code>Reporter</code> to which results will be reported
   * @param stopper the <code>Stopper</code> that will be consulted to determine whether to stop execution early.
   * @param configMap a <code>Map</code> of key-value pairs that can be used by the executing <code>Suite</code> of tests.
   * @param tracker a <code>Tracker</code> tracking <code>Ordinal</code>s being fired by the current thread.
   * @throws NullPointerException if any of <code>testName</code>, <code>reporter</code>, <code>stopper</code>, <code>configMap</code>
   *     or <code>tracker</code> is <code>null</code>.
   * @throws IllegalArgumentException if <code>testName</code> is defined, but no test with the specified test name
   *     exists in this <code>Suite</code>
   */
  protected def runTest(testName: String, reporter: Reporter, stopper: Stopper, configMap: Map[String, Any], tracker: Tracker) {

    checkRunTestParamsForNull(testName, reporter, stopper, configMap, tracker)

    val (stopRequested, report, method, hasPublicNoArgConstructor, rerunnable, testStartTime) =
      getSuiteRunTestGoodies(stopper, reporter, testName)

    reportTestStarting(this, report, tracker, testName, rerunnable)

    val formatter = getIndentedText(testName, 1, true)

    val informerForThisTest =
      MessageRecordingInformer2(
        (message, isConstructingThread, testWasPending) => reportInfoProvided(thisSuite, report, tracker, Some(testName), message, 2, isConstructingThread, true, Some(testWasPending))
      )

    val args: Array[Object] =
      if (testMethodTakesAnInformer(testName)) {
/*
        val informer =
          new Informer {
            def apply(message: String) {
              if (message == null)
                throw new NullPointerException
              reportInfoProvided(thisSuite, report, tracker, Some(testName), message, 2, true)
            }
          }
*/
        Array(informerForThisTest)  
      }
      else Array()

    var testWasPending = false
    try {
      val theConfigMap = configMap
      withFixture(
        new NoArgTest {
          def name = testName
          def apply() { method.invoke(thisSuite, args: _*) }
          def configMap = theConfigMap
        }
      )
      val duration = System.currentTimeMillis - testStartTime
      reportTestSucceeded(this, report, tracker, testName, duration, formatter, rerunnable)
    }
    catch { 
      case ite: InvocationTargetException =>
        val t = ite.getTargetException
        t match {
          case _: TestPendingException =>
            reportTestPending(this, report, tracker, testName, formatter)
            testWasPending = true // Set so info's printed out in the finally clause show up yellow
          case e if !anErrorThatShouldCauseAnAbort(e) =>
            val duration = System.currentTimeMillis - testStartTime
            handleFailedTest(t, hasPublicNoArgConstructor, testName, rerunnable, report, tracker, duration)
          case e => throw e
        }
      case e if !anErrorThatShouldCauseAnAbort(e) =>
        val duration = System.currentTimeMillis - testStartTime
        handleFailedTest(e, hasPublicNoArgConstructor, testName, rerunnable, report, tracker, duration)
      case e => throw e  
    }
    finally {
      informerForThisTest.fireRecordedMessages(testWasPending)
    }
  }

  /**
   * Run zero to many of this <code>Suite</code>'s tests.
   *
   * <p>
   * This method takes a <code>testName</code> parameter that optionally specifies a test to invoke.
   * If <code>testName</code> is defined, this trait's implementation of this method 
   * invokes <code>runTest</code> on this object, passing in:
   * </p>
   *
   * <ul>
   * <li><code>testName</code> - the <code>String</code> value of the <code>testName</code> <code>Option</code> passed
   *   to this method</li>
   * <li><code>reporter</code> - the <code>Reporter</code> passed to this method, or one that wraps and delegates to it</li>
   * <li><code>stopper</code> - the <code>Stopper</code> passed to this method, or one that wraps and delegates to it</li>
   * <li><code>configMap</code> - the <code>configMap</code> <code>Map</code> passed to this method, or one that wraps and delegates to it</li>
   * </ul>
   *
   * <p>
   * This method takes a <code>Filter</code>, which encapsulates an optional <code>Set</code> of tag names that should be included
   * (<code>tagsToInclude</code>) and a <code>Set</code> that should be excluded (<code>tagsToExclude</code>), when deciding which
   * of this <code>Suite</code>'s tests to run.
   * If <code>tagsToInclude</code> is <code>None</code>, all tests will be run
   * except those those belonging to tags listed in the <code>tagsToExclude</code> <code>Set</code>. If <code>tagsToInclude</code> is defined, only tests
   * belonging to tags mentioned in the <code>tagsToInclude</code> <code>Set</code>, and not mentioned in the <code>tagsToExclude</code <code>Set</code>
   * will be run. However, if <code>testName</code> is defined, <code>tagsToInclude</code> and <code>tagsToExclude</code> are essentially ignored.
   * Only if <code>testName</code> is <code>None</code> will <code>tagsToInclude</code> and <code>tagsToExclude</code> be consulted to
   * determine which of the tests named in the <code>testNames</code> <code>Set</code> should be run. This trait's implementation
   * behaves this way, and it is part of the general contract of this method, so all overridden forms of this method should behave
   * this way as well.  For more information on test tags, see the main documentation for this trait and for class <a href="Filter"><code>Filter</code></a>.
   * Note that this means that even if a test is marked as ignored, for example a test method in a <code>Suite</code> annotated with
   * <code>org.scalatest.Ignore</code>, if that test name is passed as <code>testName</code> to <code>runTest</code>, it will be invoked
   * despite the <code>Ignore</code> annotation.
   * </p>
   *
   * <p>
   * If <code>testName</code> is <code>None</code>, this trait's implementation of this method
   * invokes <code>testNames</code> on this <code>Suite</code> to get a <code>Set</code> of names of tests to potentially run.
   * (A <code>testNames</code> value of <code>None</code> essentially acts as a wildcard that means all tests in
   * this <code>Suite</code> that are selected by <code>tagsToInclude</code> and <code>tagsToExclude</code> should be run.)
   * For each test in the <code>testName</code> <code>Set</code>, in the order
   * they appear in the iterator obtained by invoking the <code>elements</code> method on the <code>Set</code>, this trait's implementation
   * of this method checks whether the test should be run based on the <code>Filter</code>.
   * If so, this implementation invokes <code>runTest</code>, passing in:
   * </p>
   *
   * <ul>
   * <li><code>testName</code> - the <code>String</code> name of the test to run (which will be one of the names in the <code>testNames</code> <code>Set</code>)</li>
   * <li><code>reporter</code> - the <code>Reporter</code> passed to this method, or one that wraps and delegates to it</li>
   * <li><code>stopper</code> - the <code>Stopper</code> passed to this method, or one that wraps and delegates to it</li>
   * <li><code>configMap</code> - the <code>configMap</code> passed to this method, or one that wraps and delegates to it</li>
   * </ul>
   *
   * <p>
   * If a test is marked with the <code>org.scalatest.Ignore</code> tag, implementations
   * of this method are responsible for ensuring a <code>TestIgnored</code> event is fired for that test
   * and that <code>runTest</code> is not called for that test.
   * </p>
   *
   * @param testName an optional name of one test to run. If <code>None</code>, all relevant tests should be run.
   *                 I.e., <code>None</code> acts like a wildcard that means run all relevant tests in this <code>Suite</code>.
   * @param reporter the <code>Reporter</code> to which results will be reported
   * @param stopper the <code>Stopper</code> that will be consulted to determine whether to stop execution early.
   * @param filter a <code>Filter</code> with which to filter tests based on their tags
   * @param configMap a <code>Map</code> of key-value pairs that can be used by the executing <code>Suite</code> of tests.
   * @param distributor an optional <code>Distributor</code>, into which to put nested <code>Suite</code>s to be run
   *              by another entity, such as concurrently by a pool of threads. If <code>None</code>, nested <code>Suite</code>s will be run sequentially.
   * @param tracker a <code>Tracker</code> tracking <code>Ordinal</code>s being fired by the current thread.
   * @throws NullPointerException if any of the passed parameters is <code>null</code>.
   * @throws IllegalArgumentException if <code>testName</code> is defined, but no test with the specified test name
   *     exists in this <code>Suite</code>
   */
  protected def runTests(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
                             configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

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
    val report = wrapReporterIfNecessary(reporter)

    // If a testName is passed to run, just run that, else run the tests returned
    // by testNames.
    testName match {
      case Some(tn) => runTest(tn, report, stopRequested, configMap, tracker)
      case None =>

      for ((tn, ignoreTest) <- filter(testNames, tags)) {
        if (!stopRequested()) {
          if (ignoreTest) {
            reportTestIgnored(thisSuite, report, tracker, tn, tn, 1)
          }
          else
            runTest(tn, report, stopRequested, configMap, tracker)
        }
      }
    }
  }

  /**
   * Runs this suite of tests.
   *
   * <p>If <code>testName</code> is <code>None</code>, this trait's implementation of this method
   * calls these two methods on this object in this order:</p>
   *
   * <ol>
   * <li><code>runNestedSuites(report, stopper, tagsToInclude, tagsToExclude, configMap, distributor)</code></li>
   * <li><code>runTests(testName, report, stopper, tagsToInclude, tagsToExclude, configMap)</code></li>
   * </ol>
   *
   * <p>
   * If <code>testName</code> is defined, then this trait's implementation of this method
   * calls <code>runTests</code>, but does not call <code>runNestedSuites</code>. This behavior
   * is part of the contract of this method. Subclasses that override <code>run</code> must take
   * care not to call <code>runNestedSuites</code> if <code>testName</code> is defined. (The
   * <code>OneInstancePerTest</code> trait depends on this behavior, for example.)
   * </p>
   *
   * <p>
   * Subclasses and subtraits that override this <code>run</code> method can implement them without
   * invoking either the <code>runTests</code> or <code>runNestedSuites</code> methods, which
   * are invoked by this trait's implementation of this method. It is recommended, but not required,
   * that subclasses and subtraits that override <code>run</code> in a way that does not
   * invoke <code>runNestedSuites</code> also override <code>runNestedSuites</code> and make it
   * final. Similarly it is recommended, but not required,
   * that subclasses and subtraits that override <code>run</code> in a way that does not
   * invoke <code>runTests</code> also override <code>runTests</code> (and <code>runTest</code>,
   * which this trait's implementation of <code>runTests</code> calls) and make it
   * final. The implementation of these final methods can either invoke the superclass implementation
   * of the method, or throw an <code>UnsupportedOperationException</code> if appropriate. The
   * reason for this recommendation is that ScalaTest includes several traits that override
   * these methods to allow behavior to be mixed into a <code>Suite</code>. For example, trait
   * <code>BeforeAndAfterEach</code> overrides <code>runTests</code>s. In a <code>Suite</code>
   * subclass that no longer invokes <code>runTests</code> from <code>run</code>, the
   * <code>BeforeAndAfterEach</code> trait is not applicable. Mixing it in would have no effect.
   * By making <code>runTests</code> final in such a <code>Suite</code> subtrait, you make
   * the attempt to mix <code>BeforeAndAfterEach</code> into a subclass of your subtrait
   * a compiler error. (It would fail to compile with a complaint that <code>BeforeAndAfterEach</code>
   * is trying to override <code>runTests</code>, which is a final method in your trait.) 
   * </p>
   *
   * @param testName an optional name of one test to run. If <code>None</code>, all relevant tests should be run.
   *                 I.e., <code>None</code> acts like a wildcard that means run all relevant tests in this <code>Suite</code>.
   * @param reporter the <code>Reporter</code> to which results will be reported
   * @param stopper the <code>Stopper</code> that will be consulted to determine whether to stop execution early.
   * @param filter a <code>Filter</code> with which to filter tests based on their tags
   * @param configMap a <code>Map</code> of key-value pairs that can be used by the executing <code>Suite</code> of tests.
   * @param distributor an optional <code>Distributor</code>, into which to put nested <code>Suite</code>s to be run
   *              by another entity, such as concurrently by a pool of threads. If <code>None</code>, nested <code>Suite</code>s will be run sequentially.
   * @param tracker a <code>Tracker</code> tracking <code>Ordinal</code>s being fired by the current thread.
   *         
   * @throws NullPointerException if any passed parameter is <code>null</code>.
   * @throws IllegalArgumentException if <code>testName</code> is defined, but no test with the specified test name
   *     exists in this <code>Suite</code>
   */
  def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
              configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

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
    val report = wrapReporterIfNecessary(reporter)

    testName match {
      case None => runNestedSuites(report, stopRequested, filter, configMap, distributor, tracker)
      case Some(_) =>
    }
    runTests(testName, report, stopRequested, filter, configMap, distributor, tracker)

    if (stopRequested()) {
      val rawString = Resources("executeStopping")
      report(InfoProvided(tracker.nextOrdinal(), rawString, Some(NameInfo(thisSuite.suiteName, Some(thisSuite.getClass.getName), testName))))
    }
  }

  private[scalatest] def handleFailedTest(throwable: Throwable, hasPublicNoArgConstructor: Boolean, testName: String,
      rerunnable: Option[Rerunner], report: Reporter, tracker: Tracker, duration: Long) {

    val message = getMessageForException(throwable)
    val formatter = getIndentedText(testName, 1, true)
    report(TestFailed(tracker.nextOrdinal(), message, thisSuite.suiteName, Some(thisSuite.getClass.getName), testName, Some(throwable), Some(duration), Some(formatter), rerunnable))
  }

  /**
   *
   * Run zero to many of this <code>Suite</code>'s nested <code>Suite</code>s.
   *
   * <p>
   * If the passed <code>distributor</code> is <code>None</code>, this trait's
   * implementation of this method invokes <code>run</code> on each
   * nested <code>Suite</code> in the <code>List</code> obtained by invoking <code>nestedSuites</code>.
   * If a nested <code>Suite</code>'s <code>run</code>
   * method completes abruptly with an exception, this trait's implementation of this
   * method reports that the <code>Suite</code> aborted and attempts to run the
   * next nested <code>Suite</code>.
   * If the passed <code>distributor</code> is defined, this trait's implementation
   * puts each nested <code>Suite</code> 
   * into the <code>Distributor</code> contained in the <code>Some</code>, in the order in which the
   * <code>Suite</code>s appear in the <code>List</code> returned by <code>nestedSuites</code>, passing
   * in a new <code>Tracker</code> obtained by invoking <code>nextTracker</code> on the <code>Tracker</code>
   * passed to this method.
   * </p>
   *
   * <p>
   * Implementations of this method are responsible for ensuring <code>SuiteStarting</code> events
   * are fired to the <code>Reporter</code> before executing any nested <code>Suite</code>, and either <code>SuiteCompleted</code>
   * or <code>SuiteAborted</code> after executing any nested <code>Suite</code>.
   * </p>
   *
   * @param reporter the <code>Reporter</code> to which results will be reported
   * @param stopper the <code>Stopper</code> that will be consulted to determine whether to stop execution early.
   * @param filter a <code>Filter</code> with which to filter tests based on their tags
   * @param configMap a <code>Map</code> of key-value pairs that can be used by the executing <code>Suite</code> of tests.
   * @param distributor an optional <code>Distributor</code>, into which to put nested <code>Suite</code>s to be run
   *              by another entity, such as concurrently by a pool of threads. If <code>None</code>, nested <code>Suite</code>s will be run sequentially.
   * @param tracker a <code>Tracker</code> tracking <code>Ordinal</code>s being fired by the current thread.
   *         
   * @throws NullPointerException if any passed parameter is <code>null</code>.
   */
  protected def runNestedSuites(reporter: Reporter, stopper: Stopper, filter: Filter,
                                configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

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
    val report = wrapReporterIfNecessary(reporter)

    def callExecuteOnSuite(nestedSuite: Suite) {

      if (!stopRequested()) {

        // Create a Rerunner if the Suite has a no-arg constructor 
        val hasPublicNoArgConstructor = Suite.checkForPublicNoArgConstructor(nestedSuite.getClass)

        val rerunnable =
          if (hasPublicNoArgConstructor)
            Some(new SuiteRerunner(nestedSuite.getClass.getName))
          else
            None

        val rawString = Resources("suiteExecutionStarting")
        val formatter = formatterForSuiteStarting(nestedSuite)

        val suiteStartTime = System.currentTimeMillis

        report(SuiteStarting(tracker.nextOrdinal(), nestedSuite.suiteName, Some(nestedSuite.getClass.getName), formatter, rerunnable))

        try {
          // Same thread, so OK to send same tracker
          nestedSuite.run(None, report, stopRequested, filter, configMap, distributor, tracker)

          val rawString = Resources("suiteCompletedNormally")
          val formatter = formatterForSuiteCompleted(nestedSuite)

          val duration = System.currentTimeMillis - suiteStartTime
          report(SuiteCompleted(tracker.nextOrdinal(), nestedSuite.suiteName, Some(nestedSuite.getClass.getName), Some(duration), formatter, rerunnable))
        }
        catch {       
          case e: RuntimeException => {

            val rawString = Resources("executeException")
            val formatter = formatterForSuiteAborted(nestedSuite, rawString)

            val duration = System.currentTimeMillis - suiteStartTime
            report(SuiteAborted(tracker.nextOrdinal(), rawString, nestedSuite.suiteName, Some(nestedSuite.getClass.getName), Some(e), Some(duration), formatter, rerunnable))
          }
        }
      }
    }

    distributor match {
      case None =>
        val nestedSuitesArray = nestedSuites.toArray
        for (i <- 0 until nestedSuitesArray.length) {
          if (!stopRequested()) {
            callExecuteOnSuite(nestedSuitesArray(i))
          }
        }
      case Some(distribute) =>
        for (nestedSuite <- nestedSuites)
          distribute(nestedSuite, tracker.nextTracker())
    }
  }

  /**
   * A user-friendly suite name for this <code>Suite</code>.
   *
   * <p>
   * This trait's
   * implementation of this method returns the simple name of this object's class. This
   * trait's implementation of <code>runNestedSuites</code> calls this method to obtain a
   * name for <code>Report</code>s to pass to the <code>suiteStarting</code>, <code>suiteCompleted</code>,
   * and <code>suiteAborted</code> methods of the <code>Reporter</code>.
   * </p>
   *
   * @return this <code>Suite</code> object's suite name.
   */
  def suiteName = getSimpleNameOfAnObjectsClass(thisSuite)

  /**
   * Throws <code>TestPendingException</code> to indicate a test is pending.
   *
   * <p>
   * A <em>pending test</em> is one that has been given a name but is not yet implemented. The purpose of
   * pending tests is to facilitate a style of testing in which documentation of behavior is sketched
   * out before tests are written to verify that behavior (and often, the before the behavior of
   * the system being tested is itself implemented). Such sketches form a kind of specification of
   * what tests and functionality to implement later.
   * </p>
   *
   * <p>
   * To support this style of testing, a test can be given a name that specifies one
   * bit of behavior required by the system being tested. The test can also include some code that
   * sends more information about the behavior to the reporter when the tests run. At the end of the test,
   * it can call method <code>pending</code>, which will cause it to complete abruptly with <code>TestPendingException</code>.
   * Because tests in ScalaTest can be designated as pending with <code>TestPendingException</code>, both the test name and any information
   * sent to the reporter when running the test can appear in the report of a test run. (In other words,
   * the code of a pending test is executed just like any other test.) However, because the test completes abruptly
   * with <code>TestPendingException</code>, the test will be reported as pending, to indicate
   * the actual test, and possibly the functionality it is intended to test, has not yet been implemented.
   * </p>
   *
   * <p>
   * Note: This method always completes abruptly with a <code>TestPendingException</code>. Thus it always has a side
   * effect. Methods with side effects are usually invoked with parentheses, as in <code>pending()</code>. This
   * method is defined as a parameterless method, in flagrant contradiction to recommended Scala style, because it 
   * forms a kind of DSL for pending tests. It enables tests in suites such as <code>FunSuite</code> or <code>Spec</code>
   * to be denoted by placing "<code>(pending)</code>" after the test name, as in:
   * </p>
   *
   * <pre class="stHighlight">
   * test("that style rules are not laws") (pending)
   * </pre>
   *
   * <p>
   * Readers of the code see "pending" in parentheses, which looks like a little note attached to the test name to indicate
   * it is pending. Whereas "<code>(pending())</code> looks more like a method call, "<code>(pending)</code>" lets readers
   * stay at a higher level, forgetting how it is implemented and just focusing on the intent of the programmer who wrote the code.
   * </p>
   */
  def pending: PendingNothing = { throw new TestPendingException }

  /**
   * Execute the passed block of code, and if it completes abruptly, throw <code>TestPendingException</code>, else
   * throw <code>TestFailedException</code>.
   *
   * <p>
   * This method can be used to temporarily change a failing test into a pending test in such a way that it will
   * automatically turn back into a failing test once the problem originally causing the test to fail has been fixed.
   * At that point, you need only remove the <code>pendingUntilFixed</code> call. In other words, a
   * <code>pendingUntilFixed</code> surrounding a block of code that isn't broken is treated as a test failure.
   * The motivation for this behavior is to encourage people to remove <code>pendingUntilFixed</code> calls when
   * there are no longer needed.
   * </p>
   *
   * <p>
   * This method facilitates a style of testing in which tests are written before the code they test. Sometimes you may
   * encounter a test failure that requires more functionality than you want to tackle without writing more tests. In this
   * case you can mark the bit of test code causing the failure with <code>pendingUntilFixed</code>. You can then write more
   * tests and functionality that eventually will get your production code to a point where the original test won't fail anymore.
   * At this point the code block marked with <code>pendingUntilFixed</code> will no longer throw an exception (because the
   * problem has been fixed). This will in turn cause <code>pendingUntilFixed</code> to throw <code>TestFailedException</code>
   * with a detail message explaining you need to go back and remove the <code>pendingUntilFixed</code> call as the problem orginally
   * causing your test code to fail has been fixed.
   * </p>
   *
   * @param f a block of code, which if it completes abruptly, should trigger a <code>TestPendingException</code> 
   * @throws TestPendingException if the passed block of code completes abruptly with an <code>Exception</code> or <code>AssertionError</code>
   */
  def pendingUntilFixed(f: => Unit) {
    val isPending =
      try {
        f
        false
      }
      catch {
        case _: Exception => true
        case _: AssertionError => true
      }
      if (isPending)
        throw new TestPendingException
      else
        throw new TestFailedException(Resources("pendingUntilFixed"), 2)
  }

  /**
   * The total number of tests that are expected to run when this <code>Suite</code>'s <code>run</code> method is invoked.
   *
   * <p>
   * This trait's implementation of this method returns the sum of:
   * </p>
   *
   * <ul>
   * <li>the size of the <code>testNames</code> <code>List</code>, minus the number of tests marked as ignored
   * <li>the sum of the values obtained by invoking
   *     <code>expectedTestCount</code> on every nested <code>Suite</code> contained in
   *     <code>nestedSuites</code>
   * </ul>
   *
   * @param filter a <code>Filter</code> with which to filter tests to count based on their tags
   */
  def expectedTestCount(filter: Filter): Int = {

    // [bv: here was another tricky refactor. How to increment a counter in a loop]
    def countNestedSuiteTests(nestedSuites: List[Suite], filter: Filter): Int =
      nestedSuites match {
        case List() => 0
        case nestedSuite :: nestedSuites => nestedSuite.expectedTestCount(filter) +
            countNestedSuiteTests(nestedSuites, filter)
    }

    filter.runnableTestCount(testNames, tags) + countNestedSuiteTests(nestedSuites, filter)
  }

  // Wrap any non-DispatchReporter, non-CatchReporter in a CatchReporter,
  // so that exceptions are caught and transformed
  // into error messages on the standard error stream.
  private[scalatest] def wrapReporterIfNecessary(reporter: Reporter) = reporter match {
    case dr: DispatchReporter => dr
    case cr: CatchReporter => cr
    case _ => new CatchReporter(reporter)
  }
}

private[scalatest] object Suite {

  private[scalatest] val TestMethodPrefix = "test"
  private[scalatest] val InformerInParens = "(Informer)"
  private[scalatest] val IgnoreAnnotation = "org.scalatest.Ignore"

  private[scalatest] def getSimpleNameOfAnObjectsClass(o: AnyRef) = stripDollars(parseSimpleName(o.getClass().getName()))

  // [bv: this is a good example of the expression type refactor. I moved this from SuiteClassNameListCellRenderer]
  // this will be needed by the GUI classes, etc.
  private[scalatest] def parseSimpleName(fullyQualifiedName: String) = {

    val dotPos = fullyQualifiedName.lastIndexOf('.')

    // [bv: need to check the dotPos != fullyQualifiedName.length]
    if (dotPos != -1 && dotPos != fullyQualifiedName.length)
      fullyQualifiedName.substring(dotPos + 1)
    else
      fullyQualifiedName
  }
  
  private[scalatest] def checkForPublicNoArgConstructor(clazz: java.lang.Class[_]) = {
    
    try {
      val constructor = clazz.getConstructor(new Array[java.lang.Class[T] forSome { type T }](0): _*)

      Modifier.isPublic(constructor.getModifiers)
    }
    catch {
      case nsme: NoSuchMethodException => false
    }
  }

  private[scalatest] def stripDollars(s: String): String = {
    val lastDollarIndex = s.lastIndexOf('$')
    if (lastDollarIndex < s.length - 1)
      if (lastDollarIndex == -1 || !s.startsWith("line")) s else s.substring(lastDollarIndex + 1)
    else {
      // The last char is a dollar sign
      val lastNonDollarChar = s.reverse.find(_ != '$')
      lastNonDollarChar match {
        case None => s
        case Some(c) => {
          val lastNonDollarIndex = s.lastIndexOf(c)
          if (lastNonDollarIndex == -1) s
          else stripDollars(s.substring(0, lastNonDollarIndex + 1))
        }
      }
    }
  }
  
  private[scalatest] def diffStrings(s: String, t: String): Tuple2[String, String] = {
    def findCommonPrefixLength(s: String, t: String): Int = {
      val max = s.length.min(t.length) // the maximum potential size of the prefix
      var i = 0
      var found = false
      while (i < max & !found) {
        found = (s.charAt(i) != t.charAt(i))
        if (!found)
          i = i + 1
      }
      i
    }
    def findCommonSuffixLength(s: String, t: String): Int = {
      val max = s.length.min(t.length) // the maximum potential size of the suffix
      var i = 0
      var found = false
      while (i < max & !found) {
        found = (s.charAt(s.length - 1 - i) != t.charAt(t.length - 1 - i))
        if (!found)
          i = i + 1
      }
      i
    }
    val commonPrefixLength = findCommonPrefixLength(s, t)
    val commonSuffixLength = findCommonSuffixLength(s.substring(commonPrefixLength), t.substring(commonPrefixLength))
    val prefix = s.substring(0, commonPrefixLength)
    val suffix = if (s.length - commonSuffixLength < 0) "" else s.substring(s.length - commonSuffixLength)
    val sMiddleEnd = s.length - commonSuffixLength
    val tMiddleEnd = t.length - commonSuffixLength
    val sMiddle = s.substring(commonPrefixLength, sMiddleEnd)
    val tMiddle = t.substring(commonPrefixLength, tMiddleEnd)
    val MaxContext = 20
    val shortPrefix = if (commonPrefixLength > MaxContext) "..." + prefix.substring(prefix.length - MaxContext) else prefix
    val shortSuffix = if (commonSuffixLength > MaxContext) suffix.substring(0, MaxContext) + "..." else suffix
    (shortPrefix + "[" + sMiddle + "]" + shortSuffix, shortPrefix + "[" + tMiddle + "]" + shortSuffix)
  }
  
  // If the objects are two strings, replace them with whatever is returned by diffStrings.
  // Otherwise, use the same objects.
  private[scalatest] def getObjectsForFailureMessage(a: Any, b: Any) = 
    a match {
      case aStr: String => {
        b match {
          case bStr: String => {
            Suite.diffStrings(aStr, bStr)    
          }
          case _ => (a, b)
        }
      } 
      case _ => (a, b)
    }

  private[scalatest] def formatterForSuiteStarting(suite: Suite): Option[Formatter] =
      Some(IndentedText(suite.suiteName + ":", suite.suiteName, 0))

  private[scalatest] def formatterForSuiteCompleted(suite: Suite): Option[Formatter] =
      Some(MotionToSuppress)

  private[scalatest] def formatterForSuiteAborted(suite: Suite, message: String): Option[Formatter] =
      Some(IndentedText(message, message, 0))

  private def simpleNameForTest(testName: String) =
    if (testName.endsWith(InformerInParens))
      testName.substring(0, testName.length - InformerInParens.length)
    else
      testName

  private[scalatest] def anErrorThatShouldCauseAnAbort(throwable: Throwable) =
    throwable match {
      case _: AnnotationFormatError => true
      case _: AWTError => true
      case _: CoderMalfunctionError => true
      case _: FactoryConfigurationError => true
      case _: LinkageError => true
      case _: ThreadDeath => true
      case _: TransformerFactoryConfigurationError => true
      case _: VirtualMachineError => true
      case _ => false
    }

  def takesInformer(m: Method) = {
    val paramTypes = m.getParameterTypes
    paramTypes.length == 1 && classOf[Informer].isAssignableFrom(paramTypes(0))
  }

  def isTestMethodGoodies(m: Method) = {

    val isInstanceMethod = !Modifier.isStatic(m.getModifiers())

    // name must have at least 4 chars (minimum is "test")
    val simpleName = m.getName
    val firstFour = if (simpleName.length >= 4) simpleName.substring(0, 4) else "" 

    val paramTypes = m.getParameterTypes
    val hasNoParams = paramTypes.length == 0

    // Discover testNames(Informer) because if we didn't it might be confusing when someone
    // actually wrote a testNames(Informer) method and it was silently ignored.
    val isTestNames = simpleName == "testNames"

    (isInstanceMethod, simpleName, firstFour, paramTypes, hasNoParams, isTestNames)
  }

  def testMethodTakesAnInformer(testName: String) = testName.endsWith(InformerInParens)

  def checkRunTestParamsForNull(testName: String, reporter: Reporter, stopper: Stopper, configMap: Map[String, Any], tracker: Tracker) {
    if (testName == null)
      throw new NullPointerException("testName was null")
    if (reporter == null)
      throw new NullPointerException("reporter was null")
    if (stopper == null)
      throw new NullPointerException("stopper was null")
    if (configMap == null)
      throw new NullPointerException("configMap was null")
    if (tracker == null)
      throw new NullPointerException("tracker was null")
  }
 
  /*
   For info and test names, the formatted text should have one level shaved off so that the text will
   line up correctly, and the icon is over to the left of that even with the enclosing level.

   If a test is at the top level (not nested inside a describe, it's level is 0. So no need to subtract 1
   to make room for the icon in that case. An info inside such a test will have level 1. And agin, in that
   case no need to subtract 1. Such a test is "outermost test" and the info inside is "in outermost test" in:

class ArghSpec extends Spec with GivenWhenThen {
  info("in ArghSpec")
  it("outermost test") {
    info("in outermost test")
  }
  describe("Apple") {
    info("in Apple")
    describe("Boat") {
      info("in Boat")
      describe("Cat") {
        info("in Cat")
        describe("Dog") {
          info("in Dog")
          describe("Elephant") {
            info("in Elephant")
            it("Factory") {
              info("in Factory (test)")
              given("an empty Stack")
              when("push is invoked")
              then("it should have size 1")
              and("pop should return the pushed value")
            }
          }
        }
      }
    }
  }
}

It should (and at this point does) output this:

[scalatest] ArghSpec:
[scalatest] + in ArghSpec 
[scalatest] - outermost test (5 milliseconds)
[scalatest]   + in outermost test 
[scalatest] Apple 
[scalatest] + in Apple 
[scalatest]   Boat 
[scalatest]   + in Boat 
[scalatest]     Cat 
[scalatest]     + in Cat 
[scalatest]       Dog 
[scalatest]       + in Dog 
[scalatest]         Elephant 
[scalatest]         + in Elephant 
[scalatest]         - Factory (1 millisecond)
[scalatest]           + in Factory (test) 
[scalatest]           + Given an empty Stack 
[scalatest]           + When push is invoked 
[scalatest]           + Then it should have size 1 
[scalatest]           + And pop should return the pushed value 

FeatureSpec doesn't want any icons printed out. So adding includeIcon here. It
was already in getIndentedTextForInfo because of descriptions being printed out
without icons.

This should really be named getIndentedTextForTest maybe, because I think it is just
used for test events like succeeded/failed, etc.
  */
  def getIndentedText(testText: String, level: Int, includeIcon: Boolean) = {
    val formattedText =
      if (includeIcon) {
        val testSucceededIcon = Resources("testSucceededIconChar")
        ("  " * (if (level == 0) 0 else (level - 1))) + Resources("iconPlusShortName", testSucceededIcon, testText)
      }
      else {
        ("  " * level) + testText
      }
    IndentedText(formattedText, testText, level)
  }

  // The icon is not included for branch description text, but is included for things sent via info(), given(),
  // when(), then(), etc. When it is included, reduce the level by 1, unless it is already 1 or 0.
  def getIndentedTextForInfo(message: String, level: Int, includeIcon: Boolean, infoIsInsideATest: Boolean) = {
    val formattedText =
      if (includeIcon) {
        val infoProvidedIcon = Resources("infoProvidedIconChar")
        //
        // Inside a test, you want level 1 to stay 1
        // [scalatest] - outermost test (5 milliseconds)
        // [scalatest]   + in outermost test
        //
        // But outside a test, level 1 should be transformed to 0
        // [scalatest] Apple
        // [scalatest] + in Apple
        //
        val indentationLevel =
          level match {
            case 0 => 0
            case 1 if infoIsInsideATest => 1
            case _ => level - 1
          }
        ("  " * indentationLevel) + Resources("iconPlusShortName", infoProvidedIcon, message)
        // ("  " * (if (level <= 1) level else (level - 1))) + Resources("iconPlusShortName", infoProvidedIcon, message)
      }
      else {
        ("  " * level) + message
      }
    IndentedText(formattedText, message, level)
  }

  def getMessageForException(e: Throwable): String =
    if (e.getMessage != null)
      e.getMessage
    else
      Resources("exceptionThrown", e.getClass.getName) // Say something like, "java.lang.Exception was thrown."

  def indentation(level: Int) = "  " * level

  def reportTestFailed(theSuite: Suite, report: Reporter, throwable: Throwable, testName: String, testText: String,
      rerunnable: Option[Rerunner], tracker: Tracker, duration: Long, level: Int, includeIcon: Boolean) {

    val message = getMessageForException(throwable)
    val formatter = getIndentedText(testText, level, includeIcon)
    report(TestFailed(tracker.nextOrdinal(), message, theSuite.suiteName, Some(theSuite.getClass.getName), testName, Some(throwable), Some(duration), Some(formatter), rerunnable))
  }

  def reportTestStarting(theSuite: Suite, report: Reporter, tracker: Tracker, testName: String, rerunnable: Option[Rerunner]) {
    report(TestStarting(tracker.nextOrdinal(), theSuite.suiteName, Some(theSuite.getClass.getName), testName, Some(MotionToSuppress), rerunnable))
  }

  def reportTestPending(theSuite: Suite, report: Reporter, tracker: Tracker, testName: String, formatter: Formatter) {
    report(TestPending(tracker.nextOrdinal(), theSuite.suiteName, Some(theSuite.getClass.getName), testName, Some(formatter)))
  }

  def reportTestSucceeded(theSuite: Suite, report: Reporter, tracker: Tracker, testName: String, duration: Long, formatter: Formatter, rerunnable: Option[Rerunner]) {
    report(TestSucceeded(tracker.nextOrdinal(), theSuite.suiteName, Some(theSuite.getClass.getName), testName, Some(duration), Some(formatter), rerunnable))
  }

  def reportTestIgnored(theSuite: Suite, report: Reporter, tracker: Tracker, testName: String, testText: String, level: Int) {
    val testSucceededIcon = Resources("testSucceededIconChar")
    val formattedText = indentation(level - 1) + Resources("iconPlusShortName", testSucceededIcon, testText)
    report(TestIgnored(tracker.nextOrdinal(), theSuite.suiteName, Some(theSuite.getClass.getName), testName, Some(IndentedText(formattedText, testText, level))))
  }

  // If not fired in the context of a test, then testName will be None
  def reportInfoProvided(
    theSuite: Suite,
    report: Reporter,
    tracker: Tracker,
    testName: Option[String],
    message: String,
    level: Int,
    includeNameInfo: Boolean,
    includeIcon: Boolean = true,
    aboutAPendingTest: Option[Boolean] = None
  ) {
    report(
      InfoProvided(
        tracker.nextOrdinal(),
        message,
        if (includeNameInfo)
          Some(NameInfo(theSuite.suiteName, Some(theSuite.getClass.getName), testName))
        else
          None,
        aboutAPendingTest,
        None,
        Some(getIndentedTextForInfo(message, level, includeIcon, testName.isDefined))
      )
    )
  }
}
