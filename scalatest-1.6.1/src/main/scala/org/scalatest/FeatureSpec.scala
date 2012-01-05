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

import NodeFamily._
import scala.collection.immutable.ListSet
import org.scalatest.StackDepthExceptionHelper.getStackDepth
import java.util.concurrent.atomic.AtomicReference
import java.util.ConcurrentModificationException
import org.scalatest.events._
import Suite.anErrorThatShouldCauseAnAbort

/**
 * A suite of tests in which each test represents one <em>scenario</em> of a <em>feature</em>. 
 * <code>FeatureSpec</code> is intended for writing tests that are "higher level" than unit tests, for example, integration
 * tests, functional tests, and acceptance tests. You can use <code>FeatureSpec</code> for unit testing if you prefer, however.
 * Here's an example:
 *
 * <pre class="stHighlight">
 * import org.scalatest.FeatureSpec
 * import org.scalatest.GivenWhenThen
 * import scala.collection.mutable.Stack
 * 
 * class StackFeatureSpec extends FeatureSpec with GivenWhenThen {
 * 
 *   feature("The user can pop an element off the top of the stack") {
 * 
 *     info("As a programmer")
 *     info("I want to be able to pop items off the stack")
 *     info("So that I can get them in last-in-first-out order")
 * 
 *     scenario("pop is invoked on a non-empty stack") {
 * 
 *       given("a non-empty stack")
 *       val stack = new Stack[Int]
 *       stack.push(1)
 *       stack.push(2)
 *       val oldSize = stack.size
 * 
 *       when("when pop is invoked on the stack")
 *       val result = stack.pop()
 * 
 *       then("the most recently pushed element should be returned")
 *       assert(result === 2)
 * 
 *       and("the stack should have one less item than before")
 *       assert(stack.size === oldSize - 1)
 *     }
 * 
 *     scenario("pop is invoked on an empty stack") {
 * 
 *       given("an empty stack")
 *       val emptyStack = new Stack[String]
 * 
 *       when("when pop is invoked on the stack")
 *       then("NoSuchElementException should be thrown")
 *       intercept[NoSuchElementException] {
 *         emptyStack.pop()
 *       }
 * 
 *       and("the stack should still be empty")
 *       assert(emptyStack.isEmpty)
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * A <code>FeatureSpec</code> contains <em>feature clauses</em> and <em>scenarios</em>. You define a feature clause
 * with <code>feature</code>, and a scenario with <code>scenario</code>. Both
 * <code>feature</code> and <code>scenario</code> are methods, defined in
 * <code>FeatureSpec</code>, which will be invoked
 * by the primary constructor of <code>StackFeatureSpec</code>. 
 * A feature clause describes a feature of the <em>subject</em> (class or other entity) you are specifying
 * and testing. In the previous example, 
 * the subject under specification and test is a stack. The feature being specified and tested is 
 * the ability for a user (a programmer in this case) to pop an element off the top of the stack. With each scenario you provide a
 * string (the <em>spec text</em>) that specifies the behavior of the subject for
 * one scenario in which the feature may be used, and a block of code that tests that behavior.
 * You place the spec text between the parentheses, followed by the test code between curly
 * braces.  The test code will be wrapped up as a function passed as a by-name parameter to
 * <code>scenario</code>, which will register the test for later execution.
 * </p>
 *
 * <p>
 * A <code>FeatureSpec</code>'s lifecycle has two phases: the <em>registration</em> phase and the
 * <em>ready</em> phase. It starts in registration phase and enters ready phase the first time
 * <code>run</code> is called on it. It then remains in ready phase for the remainder of its lifetime.
 * </p>
 *
 * <p>
 * Scenarios can only be registered with the <code>scenario</code> method while the <code>FeatureSpec</code> is
 * in its registration phase. Any attempt to register a scenario after the <code>FeatureSpec</code> has
 * entered its ready phase, <em>i.e.</em>, after <code>run</code> has been invoked on the <code>FeatureSpec</code>,
 * will be met with a thrown <code>TestRegistrationClosedException</code>. The recommended style
 * of using <code>FeatureSpec</code> is to register tests during object construction as is done in all
 * the examples shown here. If you keep to the recommended style, you should never see a
 * <code>TestRegistrationClosedException</code>.
 * </p>
 *
 * <p>
 * Each scenario represents one test. The name of the test is the spec text passed to the <code>scenario</code> method.
 * The feature name does not appear as part of the test name. In a <code>FeatureSpec</code>, therefore, you must take care
 * to ensure that each test has a unique name (in other words, that each <code>scenario</code> has unique spec text).
 * </p>
 *
 * <p>
 * When you run a <code>FeatureSpec</code>, it will send <code>Formatter</code>s in the events it sends to the
 * <code>Reporter</code>. ScalaTest's built-in reporters will report these events in such a way
 * that the output is easy to read as an informal specification of the <em>subject</em> being tested.
 * For example, if you ran <code>StackFeatureSpec</code> from within the Scala interpreter:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new StackFeatureSpec).execute()
 * </pre>
 *
 * <p>
 * You would see:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">Feature: The user can pop an element off the top of the stack 
 *   As a programmer 
 *   I want to be able to pop items off the stack 
 *   So that I can get them in last-in-first-out order 
 *   Scenario: pop is invoked on a non-empty stack
 *     Given a non-empty stack 
 *     When when pop is invoked on the stack 
 *     Then the most recently pushed element should be returned 
 *     And the stack should have one less item than before 
 *   Scenario: pop is invoked on an empty stack
 *     Given an empty stack 
 *     When when pop is invoked on the stack 
 *     Then NoSuchElementException should be thrown 
 *     And the stack should still be empty</span> 
 * </pre>
 *
 * <p>
 * See also: <a href="http://www.scalatest.org/getting_started_with_feature_spec" target="_blank">Getting started with <code>FeatureSpec</code>.</a>
 * </p>
 * 
 * <h2>Ignored tests</h2>
 *
 * <p>
 * To support the common use case of &#8220;temporarily&#8221; disabling a test, with the
 * good intention of resurrecting the test at a later time, <code>FeatureSpec</code> provides registration
 * methods that start with <code>ignore</code> instead of <code>scenario</code>. For example, to temporarily
 * disable the test named <code>addition</code>, just change &#8220;<code>scenario</code>&#8221; into &#8220;<code>ignore</code>,&#8221; like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.FeatureSpec
 *
 * class ArithmeticSpec extends FeatureSpec {
 *
 *   // Sharing fixture objects via instance variables
 *   val shared = 5
 *
 *   feature("Integer arithmetic") {
 *
 *     ignore("addition") {
 *       val sum = 2 + 3
 *       assert(sum === shared)
 *     }
 *
 *     scenario("subtraction") {
 *       val diff = 7 - 2
 *       assert(diff === shared)
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * If you run this version of <code>ArithmeticSpec</code> with:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new ArithmeticSpec).execute()
 * </pre>
 *
 * <p>
 * It will run only <code>subtraction</code> and report that <code>addition</code> was ignored:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">Feature: Integer arithmetic </span>
 *   <span class="stYellow">Scenario: addition !!! IGNORED !!!</span>
 *   <span class="stGreen">Scenario: subtraction</span>
 * </pre>
 *
 * <h2>Informers</h2>
 *
 * <p>
 * One of the parameters to the <code>run</code> method is a <code>Reporter</code>, which
 * will collect and report information about the running suite of tests.
 * Information about suites and tests that were run, whether tests succeeded or failed, 
 * and tests that were ignored will be passed to the <code>Reporter</code> as the suite runs.
 * Most often the reporting done by default by <code>FeatureSpec</code>'s methods will be sufficient, but
 * occasionally you may wish to provide custom information to the <code>Reporter</code> from a test.
 * For this purpose, an <code>Informer</code> that will forward information to the current <code>Reporter</code>
 * is provided via the <code>info</code> parameterless method.
 * You can pass the extra information to the <code>Informer</code> via its <code>apply</code> method.
 * The <code>Informer</code> will then pass the information to the <code>Reporter</code> via an <code>InfoProvided</code> event.
 * Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.FeatureSpec
 *
 * class ArithmeticSpec extends FeatureSpec {
 *
 *   feature("Integer arithmetic") {
 *
 *     scenario("addition") {
 *       val sum = 2 + 3
 *       assert(sum === 5)
 *       info("Addition seems to work")
 *     }
 *
 *     scenario("subtraction") {
 *       val diff = 7 - 2
 *       assert(diff === 5)
 *     }
 *   }
 * }
 * </pre>
 *
 * If you run this <code>ArithmeticSpec</code> from the interpreter, you will see the following message
 * included in the printed report:
 *
 * <pre class="stREPL">
 * <span class="stGreen">Feature: Integer arithmetic 
 *   Scenario: addition
 *     Addition seems to work</span> 
 * </pre>
 *
 * <p>
 * One use case for the <code>Informer</code> is to pass more information about a scenario to the reporter. For example,
 * the <code>GivenWhenThen</code> trait provides methods that use the implicit <code>info</code> provided by <code>FeatureSpec</code>
 * to pass such information to the reporter. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.FeatureSpec
 * import org.scalatest.GivenWhenThen
 * 
 * class ArithmeticSpec extends FeatureSpec with GivenWhenThen {
 * 
 *   feature("Integer arithmetic") {
 *
 *     scenario("addition") {
 * 
 *       given("two integers")
 *       val x = 2
 *       val y = 3
 * 
 *       when("they are added")
 *       val sum = x + y
 * 
 *       then("the result is the sum of the two numbers")
 *       assert(sum === 5)
 *     }
 *
 *     scenario("subtraction") {
 * 
 *       given("two integers")
 *       val x = 7
 *       val y = 2
 * 
 *       when("one is subtracted from the other")
 *       val diff = x - y
 * 
 *       then("the result is the difference of the two numbers")
 *       assert(diff === 5)
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * If you run this <code>FeatureSpec</code> from the interpreter, you will see the following messages
 * included in the printed report:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new ArithmeticSpec).execute()
 * <span class="stGreen">Feature: Integer arithmetic 
 *   Scenario: addition
 *     Given two integers 
 *     When they are added 
 *     Then the result is the sum of the two numbers 
 *   Scenario: subtraction
 *     Given two integers 
 *     When one is subtracted from the other 
 *     Then the result is the difference of the two numbers</span> 
 * </pre>
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
 * the actual test, and possibly the functionality, has not yet been implemented.
 * You can mark tests as pending in a <code>FeatureSpec</code> like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.FeatureSpec
 *
 * class ArithmeticSpec extends FeatureSpec {
 *
 *   // Sharing fixture objects via instance variables
 *   val shared = 5
 *
 *   feature("Integer arithmetic") {
 *
 *     scenario("addition") {
 *       val sum = 2 + 3
 *       assert(sum === shared)
 *     }
 *
 *     scenario("subtraction") (pending)
 *   }
 * }
 * </pre>
 *
 * <p>
 * (Note: "<code>(pending)</code>" is the body of the test. Thus the test contains just one statement, an invocation
 * of the <code>pending</code> method, which throws <code>TestPendingException</code>.)
 * If you run this version of <code>ArithmeticSpec</code> with:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new ArithmeticSpec).execute()
 * </pre>
 *
 * <p>
 * It will run both tests, but report that <code>subtraction</code> is pending. You'll see:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">Feature: Integer arithmetic 
 *   Scenario: addition</span>
 *   <span class="stYellow">Scenario: subtraction (pending)</span>
 * </pre>
 * 
 * <p>
 * One difference between an ignored test and a pending one is that an ignored test is intended to be used during a
 * significant refactorings of the code under test, when tests break and you don't want to spend the time to fix
 * all of them immediately. You can mark some of those broken tests as ignored temporarily, so that you can focus the red
 * bar on just failing tests you actually want to fix immediately. Later you can go back and fix the ignored tests.
 * In other words, by ignoring some failing tests temporarily, you can more easily notice failed tests that you actually
 * want to fix. By contrast, a pending test is intended to be used before a test and/or the code under test is written.
 * Pending indicates you've decided to write a test for a bit of behavior, but either you haven't written the test yet, or
 * have only written part of it, or perhaps you've written the test but don't want to implement the behavior it tests
 * until after you've implemented a different bit of behavior you realized you need first. Thus ignored tests are designed
 * to facilitate refactoring of existing code whereas pending tests are designed to facilitate the creation of new code.
 * </p>
 *
 * <p>
 * One other difference between ignored and pending tests is that ignored tests are implemented as a test tag that is
 * excluded by default. Thus an ignored test is never executed. By contrast, a pending test is implemented as a
 * test that throws <code>TestPendingException</code> (which is what calling the <code>pending</code> method does). Thus
 * the body of pending tests are executed up until they throw <code>TestPendingException</code>. The reason for this difference
 * is that it enables your unfinished test to send <code>InfoProvided</code> messages to the reporter before it completes
 * abruptly with <code>TestPendingException</code>, as shown in the previous example on <code>Informer</code>s
 * that used the <code>GivenWhenThen</code> trait. For example, the following snippet in a <code>FeatureSpec</code>:
 * </p>
 *
 * <pre class="stHighlight">
 *   feature("Integer arithmetic") {
 *&nbsp;
 *     scenario("addition") {
 *       given("two integers")
 *       when("they are added")
 *       then("the result is the sum of the two numbers")
 *       pending
 *     }
 *     // ...
 * </pre>
 *
 * <p>
 * Would yield the following output when run in the interpreter:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">Feature: Integer arithmetic</span> 
 *   <span class="stYellow">Scenario: addition (pending)
 *     Given two integers 
 *     When they are added 
 *     Then the result is the sum of the two numbers</span> 
 * </pre>
 *
 * <h2>Tagging tests</h2>
 *
 * <p>
 * A <code>FeatureSpec</code>'s tests may be classified into groups by <em>tagging</em> them with string names.
 * As with any suite, when executing a <code>FeatureSpec</code>, groups of tests can
 * optionally be included and/or excluded. To tag a <code>FeatureSpec</code>'s tests,
 * you pass objects that extend abstract class <code>org.scalatest.Tag</code> to methods
 * that register tests, <code>test</code> and <code>ignore</code>. Class <code>Tag</code> takes one parameter, a string name.  If you have
 * created Java annotation interfaces for use as group names in direct subclasses of <code>org.scalatest.Suite</code>,
 * then you will probably want to use group names on your <code>FeatureSpec</code>s that match. To do so, simply 
 * pass the fully qualified names of the Java interfaces to the <code>Tag</code> constructor. For example, if you've
 * defined Java annotation interfaces with fully qualified names, <code>com.mycompany.tags.SlowTest</code> and
 * <code>com.mycompany.tags.DbTest</code>, then you could
 * create matching groups for <code>FeatureSpec</code>s like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Tag
 *
 * object SlowTest extends Tag("com.mycompany.tags.SlowTest")
 * object DbTest extends Tag("com.mycompany.tags.DbTest")
 * </pre>
 *
 * <p>
 * Given these definitions, you could place <code>FeatureSpec</code> tests into groups like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.FeatureSpec
 *
 * class ArithmeticSpec extends FeatureSpec {
 *
 *   // Sharing fixture objects via instance variables
 *   val shared = 5
 *
 *   feature("Integer arithmetic") {
 *
 *     scenario("addition", SlowTest) {
 *       val sum = 2 + 3
 *       assert(sum === shared)
 *     }
 *
 *     scenario("subtraction", SlowTest, DbTest) {
 *       val diff = 7 - 2
 *       assert(diff === shared)
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * This code marks both tests, "addition" and "subtraction," with the <code>com.mycompany.tags.SlowTest</code> tag, 
 * and test "subtraction" with the <code>com.mycompany.tags.DbTest</code> tag.
 * </p>
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
 * previous <code>ExampleSpec</code> examples. If multiple methods need to share an immutable fixture, one approach
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
 * import org.scalatest.FeatureSpec
 * import collection.mutable.ListBuffer
 *
 * class ExampleSpec extends FeatureSpec {
 * 
 *   def fixture =
 *     new {
 *       val builder = new StringBuilder("ScalaTest is ")
 *       val buffer = new ListBuffer[String]
 *     }
 * 
 *   feature("Fixtures can be shared") {
 * 
 *     scenario("user learns how to share fixtures") {
 *       val f = fixture
 *       f.builder.append("easy!")
 *       assert(f.builder.toString === "ScalaTest is easy!")
 *       assert(f.buffer.isEmpty)
 *       f.buffer += "sweet"
 *     }
 * 
 *     scenario("user enjoys writing tests with shared fixtures") {
 *       val f = fixture
 *       f.builder.append("fun!")
 *       assert(f.builder.toString === "ScalaTest is fun!")
 *       assert(f.buffer.isEmpty)
 *     }
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
 * import org.scalatest.FeatureSpec
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSpec extends FeatureSpec {
 * 
 *   trait Fixture {
 *     val builder = new StringBuilder("ScalaTest is ")
 *     val buffer = new ListBuffer[String]
 *   }
 * 
 *   feature("Fixtures can be shared") {
 * 
 *     scenario("user learns how to share fixtures") {
 *       new Fixture {
 *         builder.append("easy!")
 *         assert(builder.toString === "ScalaTest is easy!")
 *         assert(buffer.isEmpty)
 *         buffer += "sweet"
 *       }
 *     }
 * 
 *     scenario("user enjoys writing tests with shared fixtures") {
 *       new Fixture {
 *         builder.append("fun!")
 *         assert(builder.toString === "ScalaTest is fun!")
 *         assert(buffer.isEmpty)
 *       }
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
 * import org.scalatest.FeatureSpec
 * import org.scalatest.OneInstancePerTest
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSpec extends FeatureSpec with OneInstancePerTest {
 * 
 *   val builder = new StringBuilder("ScalaTest is ")
 *   val buffer = new ListBuffer[String]
 * 
 *   feature("Fixtures can be shared") {
 * 
 *     scenario("user learns how to share fixtures") {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 * 
 *     scenario("user enjoys writing tests with shared fixtures") {
 *       builder.append("fun!")
 *       assert(builder.toString === "ScalaTest is fun!")
 *       assert(buffer.isEmpty)
 *     }
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
 * import org.scalatest.FeatureSpec
 * import org.scalatest.BeforeAndAfter
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSpec extends FeatureSpec with BeforeAndAfter {
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
 *   feature("Fixtures can be shared") {
 * 
 *     scenario("user learns how to share fixtures") {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 * 
 *     scenario("user enjoys writing tests with shared fixtures") {
 *       builder.append("fun!")
 *       assert(builder.toString === "ScalaTest is fun!")
 *       assert(buffer.isEmpty)
 *     }
 *   }
 * }
 * </pre>
 * 
 * <h4>Overriding <code>withFixture(NoArgTest)</code></h4>
 *
 * <p>
 * An alternate way to take care of setup and cleanup via side effects
 * is to override <code>withFixture</code>. Trait <code>Suite</code>'s implementation of
 * <code>runTest</code>, which is inherited by this trait, passes a no-arg test function to <code>withFixture</code>. It is <code>withFixture</code>'s
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
 * import org.scalatest.FeatureSpec
 * import collection.mutable.ListBuffer
 *
 * class ExampleSpec extends FeatureSpec {
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
 *   feature("Fixtures can be shared") {
 * 
 *     scenario("user learns how to share fixtures") {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 *
 *     scenario("user enjoys writing tests with shared fixtures") {
 *       builder.append("fun!")
 *       assert(builder.toString === "ScalaTest is fun!")
 *       assert(buffer.isEmpty)
 *       buffer += "clear"
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * Note that the <a href="Suite$NoArgTest.html"><code>NoArgTest</code></a> passed to <code>withFixture</code>, in addition to
 * an <code>apply</code> method that executes the test, also includes the test name as well as the <a href="Suite.html#configMapSection">config
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
 * To use the loan pattern, you can extend <code>FixtureFeatureSpec</code> (from the <code>org.scalatest.fixture</code> package) instead of
 * <code>FeatureSpec</code>. Each test in a <code>FixtureFeatureSpec</code> takes a fixture as a parameter, allowing you to pass the fixture into
 * the test. You must indicate the type of the fixture parameter by specifying <code>FixtureParam</code>, and implement a
 * <code>withFixture</code> method that takes a <code>OneArgTest</code>. This <code>withFixture</code> method is responsible for
 * invoking the one-arg test function, so you can perform fixture set up before, and clean up after, invoking and passing
 * the fixture into the test function. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.fixture.FixtureFeatureSpec
 * import java.io.FileWriter
 * import java.io.File
 * 
 * class ExampleSpec extends FixtureFeatureSpec {
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
 *   feature("Fixtures can be shared") {
 * 
 *     scenario("user learns how to share fixtures") { writer =>
 *       writer.write("Hello, test!")
 *       writer.flush()
 *       assert(new File(tmpFile).length === 12)
 *     }
 * 
 *     scenario("user enjoys writing tests with shared fixtures") { writer =>
 *       writer.write("Hi, test!")
 *       writer.flush()
 *       assert(new File(tmpFile).length === 9)
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * For more information, see the <a href="fixture/FixtureFeatureSpec.html">documentation for <code>FixtureFeatureSpec</code></a>.
 * </p>
 *
 * <a name="differentFixtures"></a><h2>Providing different fixtures to different tests</h2>
 * 
 * <p>
 * If different tests in the same <code>FeatureSpec</code> require different fixtures, you can combine the previous techniques and
 * provide each test with just the fixture or fixtures it needs. Here's an example in which a <code>StringBuilder</code> and a
 * <code>ListBuffer</code> are provided via fixture traits, and file writer (that requires cleanup) is provided via the loan pattern:
 * </p>
 *
 * <pre class="stHighlight">
 * import java.io.FileWriter
 * import java.io.File
 * import collection.mutable.ListBuffer
 * import org.scalatest.FeatureSpec
 * 
 * class ExampleSpec extends FeatureSpec {
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
 *   scenario("user is productive using the test framework") { // This test needs the StringBuilder fixture
 *     new Builder {
 *       builder.append("productive!")
 *       assert(builder.toString === "ScalaTest is productive!")
 *     }
 *   }
 * 
 *   scenario("tests are readable") { // This test needs the ListBuffer[String] fixture
 *     new Buffer {
 *       buffer += ("readable!")
 *       assert(buffer === List("ScalaTest", "is", "readable!"))
 *     }
 *   }
 * 
 *   scenario("the test framework is user-friendly") { // This test needs the FileWriter fixture
 *     withWriter { writer =>
 *       writer.write("Hello, user!")
 *       writer.flush()
 *       assert(new File(tmpFile).length === 12)
 *     }
 *   }
 * 
 *   scenario("test code is clear and concise") { // This test needs the StringBuilder and ListBuffer
 *     new Builder with Buffer {
 *       builder.append("clear!")
 *       buffer += ("concise!")
 *       assert(builder.toString === "ScalaTest is clear!")
 *       assert(buffer === List("ScalaTest", "is", "concise!"))
 *     }
 *   }
 * 
 *   scenario("user composes test artifacts") { // This test needs all three fixtures
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
 * In the previous example, <code>"user is productive using the test framework</code> uses only the <code>StringBuilder</code> fixture, so it just instantiates
 * a <code>new Builder</code>, whereas <code>tests are readable</code> uses only the <code>ListBuffer</code> fixture, so it just intantiates
 * a <code>new Buffer</code>. <code>the test framework is user-friendly</code> needs just the <code>FileWriter</code> fixture, so it invokes
 * <code>withWriter</code>, which prepares and passes a <code>FileWriter</code> to the test (and takes care of closing it afterwords).
 * </p>
 *
 * <p>
 * Two tests need multiple fixtures: <code>test code is clear and concise</code> needs both the <code>StringBuilder</code> and the
 * <code>ListBuffer</code>, so it instantiates a class that mixes in both fixture traits with <code>new Builder with Buffer</code>.
 * <code>user composes test artifacts</code> needs all three fixtures, so in addition to <code>new Builder with Buffer</code> it also invokes
 * <code>withWriter</code>, wrapping just the of the test code that needs the fixture.
 * </p>
 *
 * <p>
 * Note that in this case, the loan pattern is being implemented via the <code>withWriter</code> method that takes a function, not
 * by overriding <code>FixtureFeatureSpec</code>'s <code>withFixture(OneArgTest)</code> method. <code>FixtureFeatureSpec</code> makes the most sense
 * if all (or at least most) tests need the same fixture, whereas in this <code>Suite</code> only two tests need the
 * <code>FileWriter</code>.
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
 * scenario("user logs in") {
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
 * import org.scalatest.FeatureSpec
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
 * class ExampleSpec extends FeatureSpec with Builder with Buffer {
 * 
 *   feature("Fixtures can be shared") {
 * 
 *     scenario("user learns how to share fixtures") {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 * 
 *     scenario("user enjoys writing tests with shared fixtures") {
 *       builder.append("fun!")
 *       assert(builder.toString === "ScalaTest is fun!")
 *       assert(buffer.isEmpty)
 *       buffer += "clear"
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * By mixing in both the <code>Builder</code> and <code>Buffer</code> traits, <code>ExampleSpec</code> gets both fixtures, which will be
 * initialized before each test and cleaned up after. The order the traits are mixed together determines the order of execution.
 * In this case, <code>Builder</code> is "super" to </code>Buffer</code>. If you wanted <code>Buffer</code> to be "super"
 * to <code>Builder</code>, you need only switch the order you mix them together, like this: 
 * </p>
 *
 * <pre class="stHighlight">
 * class Example2Spec extends FeatureSpec with Buffer with Builder
 * </pre>
 *
 * <p>
 * And if you only need one fixture you mix in only that trait:
 * </p>
 *
 * <pre class="stHighlight">
 * class Example3Spec extends FeatureSpec with Builder
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
 * import org.scalatest.FeatureSpec
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
 * class ExampleSpec extends FeatureSpec with Builder with Buffer {
 * 
 *   feature("Fixtures can be shared") {
 * 
 *     scenario("user learns how to share fixtures") {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 * 
 *     scenario("user enjoys writing tests with shared fixtures") {
 *       builder.append("fun!")
 *       assert(builder.toString === "ScalaTest is fun!")
 *       assert(buffer.isEmpty)
 *       buffer += "clear"
 *     }
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
 * <a name="SharedScenarios"></a><h2>Shared scenarios</h2>
 *
 * <p>
 * Sometimes you may want to run the same test code on different fixture objects. In other words, you may want to write tests that are "shared"
 * by different fixture objects.
 * To accomplish this in a <code>FeatureSpec</code>, you first place shared tests (<em>i.e.</em>, shared scenarios) in
 * <em>behavior functions</em>. These behavior functions will be
 * invoked during the construction phase of any <code>FeatureSpec</code> that uses them, so that the scenarios they contain will
 * be registered as scenarios in that <code>FeatureSpec</code>.
 * For example, given this stack class:
 * </p>
 *
 * <pre class="stHighlight">
 * import scala.collection.mutable.ListBuffer
 * 
 * class Stack[T] {
 *
 *   val MAX = 10
 *   private var buf = new ListBuffer[T]
 *
 *   def push(o: T) {
 *     if (!full)
 *       o +: buf
 *     else
 *       throw new IllegalStateException("can't push onto a full stack")
 *   }
 *
 *   def pop(): T = {
 *     if (!empty)
 *       buf.remove(0)
 *     else
 *       throw new IllegalStateException("can't pop an empty stack")
 *   }
 *
 *   def peek: T = {
 *     if (!empty)
 *       buf(0)
 *     else
 *       throw new IllegalStateException("can't pop an empty stack")
 *   }
 *
 *   def full: Boolean = buf.size == MAX
 *   def empty: Boolean = buf.size == 0
 *   def size = buf.size
 *
 *   override def toString = buf.mkString("Stack(", ", ", ")")
 * }
 * </pre>
 *
 * <p>
 * You may want to test the <code>Stack</code> class in different states: empty, full, with one item, with one item less than capacity,
 * <em>etc</em>. You may find you have several scenarios that make sense any time the stack is non-empty. Thus you'd ideally want to run
 * those same scenarios for three stack fixture objects: a full stack, a stack with a one item, and a stack with one item less than
 * capacity. With shared tests, you can factor these scenarios out into a behavior function, into which you pass the
 * stack fixture to use when running the tests. So in your <code>FeatureSpec</code> for stack, you'd invoke the
 * behavior function three times, passing in each of the three stack fixtures so that the shared scenarios are run for all three fixtures.
 * </p>
 *
 * <p>
 * You can define a behavior function that encapsulates these shared scenarios inside the <code>FeatureSpec</code> that uses them. If they are shared
 * between different <code>FeatureSpec</code>s, however, you could also define them in a separate trait that is mixed into
 * each <code>FeatureSpec</code> that uses them.
 * <a name="StackBehaviors">For</a> example, here the <code>nonEmptyStack</code> behavior function (in this case, a
 * behavior <em>method</em>) is defined in a trait along with another
 * method containing shared scenarios for non-full stacks:
 * </p>
 * 
 * <pre class="stHighlight">
 * import org.scalatest.FeatureSpec
 * import org.scalatest.GivenWhenThen
 * import org.scalatestexamples.helpers.Stack
 * 
 * trait FeatureSpecStackBehaviors { this: FeatureSpec with GivenWhenThen =>
 * 
 *   def nonEmptyStack(createNonEmptyStack: => Stack[Int], lastItemAdded: Int) {
 * 
 *     scenario("empty is invoked on this non-empty stack: " + createNonEmptyStack.toString) {
 * 
 *       given("a non-empty stack")
 *       val stack = createNonEmptyStack
 * 
 *       when("empty is invoked on the stack")
 *       then("empty returns false")
 *       assert(!stack.empty)
 *     }
 * 
 *     scenario("peek is invoked on this non-empty stack: " + createNonEmptyStack.toString) {
 * 
 *       given("a non-empty stack")
 *       val stack = createNonEmptyStack
 *       val size = stack.size
 * 
 *       when("peek is invoked on the stack")
 *       then("peek returns the last item added")
 *       assert(stack.peek === lastItemAdded)
 * 
 *       and("the size of the stack is the same as before")
 *       assert(stack.size === size)
 *     }
 * 
 *     scenario("pop is invoked on this non-empty stack: " + createNonEmptyStack.toString) {
 * 
 *       given("a non-empty stack")
 *       val stack = createNonEmptyStack
 *       val size = stack.size
 * 
 *       when("pop is invoked on the stack")
 *       then("pop returns the last item added")
 *       assert(stack.pop === lastItemAdded)
 * 
 *       and("the size of the stack one less than before")
 *       assert(stack.size === size - 1)
 *     }
 *   }
 *   
 *   def nonFullStack(createNonFullStack: => Stack[Int]) {
 *       
 *     scenario("full is invoked on this non-full stack: " + createNonFullStack.toString) {
 * 
 *       given("a non-full stack")
 *       val stack = createNonFullStack
 * 
 *       when("full is invoked on the stack")
 *       then("full returns false")
 *       assert(!stack.full)
 *     }
 *       
 *     scenario("push is invoked on this non-full stack: " + createNonFullStack.toString) {
 * 
 *       given("a non-full stack")
 *       val stack = createNonFullStack
 *       val size = stack.size
 * 
 *       when("push is invoked on the stack")
 *       stack.push(7)
 * 
 *       then("the size of the stack is one greater than before")
 *       assert(stack.size === size + 1)
 * 
 *       and("the top of the stack contains the pushed value")
 *       assert(stack.peek === 7)
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * Given these behavior functions, you could invoke them directly, but <code>FeatureSpec</code> offers a DSL for the purpose,
 * which looks like this:
 * </p>
 *
 * <pre class="stHighlight">
 * scenariosFor(nonEmptyStack(stackWithOneItem, lastValuePushed))
 * scenariosFor(nonFullStack(stackWithOneItem))
 * </pre>
 *
 * <p>
 * If you prefer to use an imperative style to change fixtures, for example by mixing in <code>BeforeAndAfterEach</code> and
 * reassigning a <code>stack</code> <code>var</code> in <code>beforeEach</code>, you could write your behavior functions
 * in the context of that <code>var</code>, which means you wouldn't need to pass in the stack fixture because it would be
 * in scope already inside the behavior function. In that case, your code would look like this:
 * </p>
 *
 * <pre class="stHighlight">
 * scenariosFor(nonEmptyStack) // assuming lastValuePushed is also in scope inside nonEmptyStack
 * scenariosFor(nonFullStack)
 * </pre>
 *
 * <p>
 * The recommended style, however, is the functional, pass-all-the-needed-values-in style. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.FeatureSpec
 * import org.scalatest.GivenWhenThen
 * import org.scalatestexamples.helpers.Stack
 * 
 * class StackFeatureSpec extends FeatureSpec with GivenWhenThen with FeatureSpecStackBehaviors {
 * 
 *   // Stack fixture creation methods
 *   def emptyStack = new Stack[Int]
 *  
 *   def fullStack = {
 *     val stack = new Stack[Int]
 *     for (i <- 0 until stack.MAX)
 *       stack.push(i)
 *     stack
 *   }
 *  
 *   def stackWithOneItem = {
 *     val stack = new Stack[Int]
 *     stack.push(9)
 *     stack
 *   }
 *  
 *   def stackWithOneItemLessThanCapacity = {
 *     val stack = new Stack[Int]
 *     for (i <- 1 to 9)
 *       stack.push(i)
 *     stack
 *   }
 *  
 *   val lastValuePushed = 9
 *  
 *   feature("A Stack is pushed and popped") {
 *  
 *     scenario("empty is invoked on an empty stack") {
 * 
 *       given("an empty stack")
 *       val stack = emptyStack
 * 
 *       when("empty is invoked on the stack")
 *       then("empty returns true")
 *       assert(stack.empty)
 *     }
 *  
 *     scenario("peek is invoked on an empty stack") {
 * 
 *       given("an empty stack")
 *       val stack = emptyStack
 * 
 *       when("peek is invoked on the stack")
 *       then("peek throws IllegalStateException")
 *       intercept[IllegalStateException] {
 *         stack.peek
 *       }
 *     }
 *  
 *     scenario("pop is invoked on an empty stack") {
 * 
 *       given("an empty stack")
 *       val stack = emptyStack
 * 
 *       when("pop is invoked on the stack")
 *       then("pop throws IllegalStateException")
 *       intercept[IllegalStateException] {
 *         emptyStack.pop
 *       }
 *     }
 *  
 *     scenariosFor(nonEmptyStack(stackWithOneItem, lastValuePushed))
 *     scenariosFor(nonFullStack(stackWithOneItem))
 *  
 *     scenariosFor(nonEmptyStack(stackWithOneItemLessThanCapacity, lastValuePushed))
 *     scenariosFor(nonFullStack(stackWithOneItemLessThanCapacity))
 *  
 *     scenario("full is invoked on a full stack") {
 * 
 *       given("an full stack")
 *       val stack = fullStack
 * 
 *       when("full is invoked on the stack")
 *       then("full returns true")
 *       assert(stack.full)
 *     }
 *  
 *     scenariosFor(nonEmptyStack(fullStack, lastValuePushed))
 *  
 *     scenario("push is invoked on a full stack") {
 * 
 *       given("an full stack")
 *       val stack = fullStack
 * 
 *       when("push is invoked on the stack")
 *       then("push throws IllegalStateException")
 *       intercept[IllegalStateException] {
 *         stack.push(10)
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * If you load these classes into the Scala interpreter (with scalatest's JAR file on the class path), and execute it,
 * you'll see:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new StackFeatureSpec).execute()
 * <span class="stGreen">Feature: A Stack is pushed and popped 
 *   Scenario: empty is invoked on an empty stack
 *     Given an empty stack 
 *     When empty is invoked on the stack 
 *     Then empty returns true 
 *   Scenario: peek is invoked on an empty stack
 *     Given an empty stack 
 *     When peek is invoked on the stack 
 *     Then peek throws IllegalStateException 
 *   Scenario: pop is invoked on an empty stack
 *     Given an empty stack 
 *     When pop is invoked on the stack 
 *     Then pop throws IllegalStateException 
 *   Scenario: empty is invoked on this non-empty stack: Stack(9)
 *     Given a non-empty stack 
 *     When empty is invoked on the stack 
 *     Then empty returns false 
 *   Scenario: peek is invoked on this non-empty stack: Stack(9)
 *     Given a non-empty stack 
 *     When peek is invoked on the stack 
 *     Then peek returns the last item added 
 *     And the size of the stack is the same as before 
 *   Scenario: pop is invoked on this non-empty stack: Stack(9)
 *     Given a non-empty stack 
 *     When pop is invoked on the stack 
 *     Then pop returns the last item added 
 *     And the size of the stack one less than before 
 *   Scenario: full is invoked on this non-full stack: Stack(9)
 *     Given a non-full stack 
 *     When full is invoked on the stack 
 *     Then full returns false 
 *   Scenario: push is invoked on this non-full stack: Stack(9)
 *     Given a non-full stack 
 *     When push is invoked on the stack 
 *     Then the size of the stack is one greater than before 
 *     And the top of the stack contains the pushed value 
 *   Scenario: empty is invoked on this non-empty stack: Stack(9, 8, 7, 6, 5, 4, 3, 2, 1)
 *     Given a non-empty stack 
 *     When empty is invoked on the stack 
 *     Then empty returns false 
 *   Scenario: peek is invoked on this non-empty stack: Stack(9, 8, 7, 6, 5, 4, 3, 2, 1)
 *     Given a non-empty stack 
 *     When peek is invoked on the stack 
 *     Then peek returns the last item added 
 *     And the size of the stack is the same as before 
 *   Scenario: pop is invoked on this non-empty stack: Stack(9, 8, 7, 6, 5, 4, 3, 2, 1)
 *     Given a non-empty stack 
 *     When pop is invoked on the stack 
 *     Then pop returns the last item added 
 *     And the size of the stack one less than before 
 *   Scenario: full is invoked on this non-full stack: Stack(9, 8, 7, 6, 5, 4, 3, 2, 1)
 *     Given a non-full stack 
 *     When full is invoked on the stack 
 *     Then full returns false 
 *   Scenario: push is invoked on this non-full stack: Stack(9, 8, 7, 6, 5, 4, 3, 2, 1)
 *     Given a non-full stack 
 *     When push is invoked on the stack 
 *     Then the size of the stack is one greater than before 
 *     And the top of the stack contains the pushed value 
 *   Scenario: full is invoked on a full stack
 *     Given an full stack 
 *     When full is invoked on the stack 
 *     Then full returns true 
 *   Scenario: empty is invoked on this non-empty stack: Stack(9, 8, 7, 6, 5, 4, 3, 2, 1, 0)
 *     Given a non-empty stack 
 *     When empty is invoked on the stack 
 *     Then empty returns false 
 *   Scenario: peek is invoked on this non-empty stack: Stack(9, 8, 7, 6, 5, 4, 3, 2, 1, 0)
 *     Given a non-empty stack 
 *     When peek is invoked on the stack 
 *     Then peek returns the last item added 
 *     And the size of the stack is the same as before 
 *   Scenario: pop is invoked on this non-empty stack: Stack(9, 8, 7, 6, 5, 4, 3, 2, 1, 0)
 *     Given a non-empty stack 
 *     When pop is invoked on the stack 
 *     Then pop returns the last item added 
 *     And the size of the stack one less than before 
 *   Scenario: push is invoked on a full stack
 *     Given an full stack 
 *     When push is invoked on the stack 
 *     Then push throws IllegalStateException</span> 
 * </pre>
 * 
 * <p>
 * One thing to keep in mind when using shared tests is that in ScalaTest, each test in a suite must have a unique name.
 * If you register the same tests repeatedly in the same suite, one problem you may encounter is an exception at runtime
 * complaining that multiple tests are being registered with the same test name.
 * In a <code>FeatureSpec</code> there is no nesting construct analogous to <code>Spec</code>'s <code>describe</code> clause.
 * Therefore, you need to do a bit of
 * extra work to ensure that the test names are unique. If a duplicate test name problem shows up in a
 * <code>FeatureSpec</code>, you'll need to pass in a prefix or suffix string to add to each test name. You can pass this string
 * the same way you pass any other data needed by the shared tests, or just call <code>toString</code> on the shared fixture object.
 * This is the approach taken by the previous <code>FeatureSpecStackBehaviors</code> example.
 * </p>
 *
 * <p>
 * Given this <code>FeatureSpecStackBehaviors</code> trait, calling it with the <code>stackWithOneItem</code> fixture, like this:
 * </p>
 *
 * <pre class="stHighlight">
 * scenariosFor(nonEmptyStack(stackWithOneItem, lastValuePushed))
 * </pre>
 *
 * <p>
 * yields test names:
 * </p>
 *
 * <ul>
 * <li><code>empty is invoked on this non-empty stack: Stack(9)</code></li>
 * <li><code>peek is invoked on this non-empty stack: Stack(9)</code></li>
 * <li><code>pop is invoked on this non-empty stack: Stack(9)</code></li>
 * </ul>
 *
 * <p>
 * Whereas calling it with the <code>stackWithOneItemLessThanCapacity</code> fixture, like this:
 * </p>
 *
 * <pre class="stHighlight">
 * scenariosFor(nonEmptyStack(stackWithOneItemLessThanCapacity, lastValuePushed))
 * </pre>
 *
 * <p>
 * yields different test names:
 * </p>
 *
 * <ul>
 * <li><code>empty is invoked on this non-empty stack: Stack(9, 8, 7, 6, 5, 4, 3, 2, 1)</code></li>
 * <li><code>peek is invoked on this non-empty stack: Stack(9, 8, 7, 6, 5, 4, 3, 2, 1)</code></li>
 * <li><code>pop is invoked on this non-empty stack: Stack(9, 8, 7, 6, 5, 4, 3, 2, 1)</code></li>
 * </ul>
 *
 * @author Bill Venners
 */
trait FeatureSpec extends Suite { thisSuite =>

  private final val engine = new Engine("concurrentFeatureSpecMod", "FeatureSpec")
  import engine._

  /**
   * Returns an <code>Informer</code> that during test execution will forward strings (and other objects) passed to its
   * <code>apply</code> method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * <code>FeatureSpec</code> is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * throw an exception. This method can be called safely by any thread.
   */
  implicit protected def info: Informer = atomicInformer.get

  /**
   * Register a test with the given spec text, optional tags, and test function value that takes no arguments.
   * An invocation of this method is called an &#8220;example.&#8221;
   *
   * This method will register the test for later execution via an invocation of one of the <code>execute</code>
   * methods. The name of the test will be a concatenation of the text of all surrounding describers,
   * from outside in, and the passed spec text, with one space placed between each item. (See the documenation
   * for <code>testNames</code> for an example.) The resulting test name must not have been registered previously on
   * this <code>Spec</code> instance.
   *
   * @param specText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param testTags the optional list of tags for this test
   * @param testFun the test function
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws TestRegistrationClosedException if invoked after <code>run</code> has been invoked on this suite
   * @throws NullPointerException if <code>specText</code> or any passed test tag is <code>null</code>
   */
  protected def scenario(specText: String, testTags: Tag*)(testFun: => Unit) {

    registerTest(Resources("scenario", specText), testFun _, "scenarioCannotAppearInsideAnotherScenario", "FeatureSpec.scala", "scenario", testTags: _*)
  }

  /**
   * Register a test to ignore, which has the given spec text, optional tags, and test function value that takes no arguments.
   * This method will register the test for later ignoring via an invocation of one of the <code>execute</code>
   * methods. This method exists to make it easy to ignore an existing test by changing the call to <code>it</code>
   * to <code>ignore</code> without deleting or commenting out the actual test code. The test will not be executed, but a
   * report will be sent that indicates the test was ignored. The name of the test will be a concatenation of the text of all surrounding describers,
   * from outside in, and the passed spec text, with one space placed between each item. (See the documenation
   * for <code>testNames</code> for an example.) The resulting test name must not have been registered previously on
   * this <code>Spec</code> instance.
   *
   * @param specText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param testTags the optional list of tags for this test
   * @param testFun the test function
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws TestRegistrationClosedException if invoked after <code>run</code> has been invoked on this suite
   * @throws NullPointerException if <code>specText</code> or any passed test tag is <code>null</code>
   */
  protected def ignore(specText: String, testTags: Tag*)(testFun: => Unit) {
    registerIgnoredTest(Resources("scenario", specText), testFun _, "ignoreCannotAppearInsideAScenario", "FeatureSpec.scala", "ignore", testTags: _*)
  }
  
  /**
   * Describe a &#8220;subject&#8221; being specified and tested by the passed function value. The
   * passed function value may contain more describers (defined with <code>describe</code>) and/or tests
   * (defined with <code>it</code>). This trait's implementation of this method will register the
   * description string and immediately invoke the passed function.
   */
  protected def feature(description: String)(fun: => Unit) {

    if (!currentBranchIsTrunk)
      throw new NotAllowedException(Resources("cantNestFeatureClauses"), getStackDepth("FeatureSpec.scala", "feature"))

    registerNestedBranch(description, None, fun, "featureCannotAppearInsideAScenario", "FeatureSpec.scala", "feature")
  }

  /**
   * A <code>Map</code> whose keys are <code>String</code> tag names to which tests in this <code>Spec</code> belong, and values
   * the <code>Set</code> of test names that belong to each tag. If this <code>FeatureSpec</code> contains no tags, this method returns an empty <code>Map</code>.
   *
   * <p>
   * This trait's implementation returns tags that were passed as strings contained in <code>Tag</code> objects passed to 
   * methods <code>test</code> and <code>ignore</code>. 
   * </p>
   */
  override def tags: Map[String, Set[String]] = atomic.get.tagsMap

  /**
   * Run a test. This trait's implementation runs the test registered with the name specified by
   * <code>testName</code>. Each test's name is a concatenation of the text of all describers surrounding a test,
   * from outside in, and the test's  spec text, with one space placed between each item. (See the documenation
   * for <code>testNames</code> for an example.)
   *
   * @param testName the name of one test to execute.
   * @param reporter the <code>Reporter</code> to which results will be reported
   * @param stopper the <code>Stopper</code> that will be consulted to determine whether to stop execution early.
   * @param configMap a <code>Map</code> of properties that can be used by this <code>Spec</code>'s executing tests.
   * @throws NullPointerException if any of <code>testName</code>, <code>reporter</code>, <code>stopper</code>, or <code>configMap</code>
   *     is <code>null</code>.
   */
  protected override def runTest(testName: String, reporter: Reporter, stopper: Stopper, configMap: Map[String, Any], tracker: Tracker) {

    def invokeWithFixture(theTest: TestLeaf) {
      val theConfigMap = configMap
      withFixture(
        new NoArgTest {
          def name = testName
          def apply() { theTest.testFun() }
          def configMap = theConfigMap
        }
      )
    }

    runTestImpl(thisSuite, testName, reporter, stopper, configMap, tracker, false, invokeWithFixture)
  }

  /**
   * Run zero to many of this <code>FeatureSpec</code>'s tests.
   *
   * <p>
   * This method takes a <code>testName</code> parameter that optionally specifies a test to invoke.
   * If <code>testName</code> is <code>Some</code>, this trait's implementation of this method
   * invokes <code>runTest</code> on this object, passing in:
   * </p>
   *
   * <ul>
   * <li><code>testName</code> - the <code>String</code> value of the <code>testName</code> <code>Option</code> passed
   *   to this method</li>
   * <li><code>reporter</code> - the <code>Reporter</code> passed to this method, or one that wraps and delegates to it</li>
   * <li><code>stopper</code> - the <code>Stopper</code> passed to this method, or one that wraps and delegates to it</li>
   * <li><code>configMap</code> - the <code>configMap</code> passed to this method, or one that wraps and delegates to it</li>
   * </ul>
   *
   * <p>
   * This method takes a <code>Set</code> of tag names that should be included (<code>tagsToInclude</code>), and a <code>Set</code>
   * that should be excluded (<code>tagsToExclude</code>), when deciding which of this <code>Suite</code>'s tests to execute.
   * If <code>tagsToInclude</code> is empty, all tests will be executed
   * except those those belonging to tags listed in the <code>tagsToExclude</code> <code>Set</code>. If <code>tagsToInclude</code> is non-empty, only tests
   * belonging to tags mentioned in <code>tagsToInclude</code>, and not mentioned in <code>tagsToExclude</code>
   * will be executed. However, if <code>testName</code> is <code>Some</code>, <code>tagsToInclude</code> and <code>tagsToExclude</code> are essentially ignored.
   * Only if <code>testName</code> is <code>None</code> will <code>tagsToInclude</code> and <code>tagsToExclude</code> be consulted to
   * determine which of the tests named in the <code>testNames</code> <code>Set</code> should be run. For more information on trait tags, see the main documentation for this trait.
   * </p>
   *
   * <p>
   * If <code>testName</code> is <code>None</code>, this trait's implementation of this method
   * invokes <code>testNames</code> on this <code>Suite</code> to get a <code>Set</code> of names of tests to potentially execute.
   * (A <code>testNames</code> value of <code>None</code> essentially acts as a wildcard that means all tests in
   * this <code>Suite</code> that are selected by <code>tagsToInclude</code> and <code>tagsToExclude</code> should be executed.)
   * For each test in the <code>testName</code> <code>Set</code>, in the order
   * they appear in the iterator obtained by invoking the <code>elements</code> method on the <code>Set</code>, this trait's implementation
   * of this method checks whether the test should be run based on the <code>tagsToInclude</code> and <code>tagsToExclude</code> <code>Set</code>s.
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
  protected override def runTests(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
      configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {
    
    runTestsImpl(thisSuite, testName, reporter, stopper, filter, configMap, distributor, tracker, info, false, runTest)
  }

  /**
   * An immutable <code>Set</code> of test names. If this <code>FeatureSpec</code> contains no tests, this method returns an
   * empty <code>Set</code>.
   *
   * <p>
   * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's
   * iterator will return those names in the order in which the tests were registered. Each test's name is composed
   * of the concatenation of the text of each surrounding describer, in order from outside in, and the text of the
   * example itself, with all components separated by a space. For example, consider this <code>FeatureSpec</code>:
   * </p>
   *
   * <pre class="stHighlight">
   * import org.scalatest.FeatureSpec
   *
   * class StackSpec extends FeatureSpec {
   *   feature("A Stack") {
   *     scenario("(when not empty) must allow me to pop") {}
   *     scenario("(when not full) must allow me to push") {}
   *   }
   * }
   * </pre>
   *
   * <p>
   * Invoking <code>testNames</code> on this <code>Spec</code> will yield a set that contains the following
   * two test name strings:
   * </p>
   *
   * <pre>
   * "A Stack (when not empty) must allow me to pop"
   * "A Stack (when not full) must allow me to push"
   * </pre>
   */
  // override def testNames: Set[String] = ListSet(atomic.get.testsList.map(_.testName): _*)
  override def testNames: Set[String] = {
    // I'm returning a ListSet here so that they tests will be run in registration order
    ListSet(atomic.get.testNamesList.toArray: _*)
  }

  override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
      configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

    runImpl(thisSuite, testName, reporter, stopper, filter, configMap, distributor, tracker, super.run)
  }

  /**
   * Registers shared scenarios.
   *
   * <p>
   * This method enables the following syntax for shared scenarios in a <code>FeatureSpec</code>:
   * </p>
   *
   * <pre class="stHighlight">
   * scenariosFor(nonEmptyStack(lastValuePushed))
   * </pre>
   *
   * <p>
   * This method just provides syntax sugar intended to make the intent of the code clearer.
   * Because the parameter passed to it is
   * type <code>Unit</code>, the expression will be evaluated before being passed, which
   * is sufficient to register the shared scenarios. For examples of shared scenarios, see the
   * <a href="#SharedScenarios">Shared scenarios section</a> in the main documentation for this trait.
   * </p>
   */
  protected def scenariosFor(unit: Unit) {}
}
