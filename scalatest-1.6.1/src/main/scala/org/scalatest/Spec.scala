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

import scala.collection.immutable.ListSet
import java.util.ConcurrentModificationException
import java.util.concurrent.atomic.AtomicReference
import org.scalatest.StackDepthExceptionHelper.getStackDepth
import org.scalatest.events._
import Suite.anErrorThatShouldCauseAnAbort
import Suite.checkRunTestParamsForNull
import Suite.indentation
import verb.BehaveWord
import Suite.reportInfoProvided
import Suite.reportTestIgnored

/**
 * Trait that facilitates a &#8220;behavior-driven&#8221; style of development (BDD), in which tests
 * are combined with text that specifies the behavior the tests verify.
 * (Note: In BDD, the word <em>example</em> is usually used instead of <em>test</em>. The word test will not appear
 * in your code if you use <code>WordSpec</code>, so if you prefer the word <em>example</em> you can use it. However, in this documentation
 * the word <em>test</em> will be used, for clarity and to be consistent with the rest of ScalaTest.)
 * Here's an example <code>Spec</code>:
 *
 * <pre class="stHighlight">
 * import org.scalatest.Spec
 * import scala.collection.mutable.Stack
 *
 * class StackSpec extends Spec {
 *
 *   describe("A Stack") {
 *
 *     it("should pop values in last-in-first-out order") {
 *       val stack = new Stack[Int]
 *       stack.push(1)
 *       stack.push(2)
 *       assert(stack.pop() === 2)
 *       assert(stack.pop() === 1)
 *     }
 *
 *     it("should throw NoSuchElementException if an empty stack is popped") {
 *       val emptyStack = new Stack[String]
 *       intercept[NoSuchElementException] {
 *         emptyStack.pop()
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * A <code>Spec</code> contains <em>describe clauses</em> and tests. You define a describe clause
 * with <code>describe</code>, and a test with <code>it</code>. Both
 * <code>describe</code> and <code>it</code> are methods, defined in
 * <code>Spec</code>, which will be invoked
 * by the primary constructor of <code>StackSpec</code>. 
 * A describe clause names, or gives more information about, the <em>subject</em> (class or other entity) you are specifying
 * and testing. In the previous example, "A Stack"
 * is the subject under specification and test. With each test you provide a string (the <em>spec text</em>) that specifies
 * one bit of behavior of the subject, and a block of code that tests that behavior.
 * You place the spec text between the parentheses, followed by the test code between curly
 * braces.  The test code will be wrapped up as a function passed as a by-name parameter to
 * <code>it</code>, which will register the test for later execution.
 * </p>
 *
 * <p>
 * A <code>Spec</code>'s lifecycle has two phases: the <em>registration</em> phase and the
 * <em>ready</em> phase. It starts in registration phase and enters ready phase the first time
 * <code>run</code> is called on it. It then remains in ready phase for the remainder of its lifetime.
 * </p>
 *
 * <p>
 * Tests can only be registered with the <code>it</code> method while the <code>Spec</code> is
 * in its registration phase. Any attempt to register a test after the <code>Spec</code> has
 * entered its ready phase, <em>i.e.</em>, after <code>run</code> has been invoked on the <code>Spec</code>,
 * will be met with a thrown <code>TestRegistrationClosedException</code>. The recommended style
 * of using <code>Spec</code> is to register tests during object construction as is done in all
 * the examples shown here. If you keep to the recommended style, you should never see a
 * <code>TestRegistrationClosedException</code>.
 * </p>
 *
 * <p>
 * When you execute a <code>Spec</code>, it will send <code>Formatter</code>s in the events it sends to the
 * <code>Reporter</code>. ScalaTest's built-in reporters will report these events in such a way
 * that the output is easy to read as an informal specification of the <em>subject</em> being tested.
 * For example, if you ran <code>StackSpec</code> from within the Scala interpreter:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new StackSpec).execute()
 * </pre>
 *
 * <p>
 * You would see:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">A Stack
 * - should pop values in last-in-first-out order
 * - should throw NoSuchElementException if an empty stack is popped</span>
 * </pre>
 *
 * <p>
 * <em>Note: Trait <code>Spec</code>'s syntax is in great part inspired by <a href="http://rspec.info/" target="_blank">RSpec</a>, a Ruby BDD framework.</em>
 *</p>
 *
 * <p>
 * See also: <a href="http://www.scalatest.org/getting_started_with_spec" target="_blank">Getting started with <code>Spec</code>.</a>
 * </p>
 *
 * <h2>Ignored tests</h2>
 *
 * <p>
 * To support the common use case of &#8220;temporarily&#8221; disabling a test, with the
 * good intention of resurrecting the test at a later time, <code>Spec</code> provides registration
 * methods that start with <code>ignore</code> instead of <code>it</code>. For example, to temporarily
 * disable the test with the name <code>"should pop values in last-in-first-out order"</code>, just change &#8220;<code>it</code>&#8221; into &#8220;<code>ignore</code>,&#8221; like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Spec
 * import scala.collection.mutable.Stack
 *
 * class StackSpec extends Spec {
 *
 *   describe("A Stack") {
 *
 *     ignore("should pop values in last-in-first-out order") {
 *       val stack = new Stack[Int]
 *       stack.push(1)
 *       stack.push(2)
 *       assert(stack.pop() === 2)
 *       assert(stack.pop() === 1)
 *     }
 *
 *     it("should throw NoSuchElementException if an empty stack is popped") {
 *       val emptyStack = new Stack[String]
 *       intercept[NoSuchElementException] {
 *         emptyStack.pop()
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * If you run this version of <code>StackSpec</code> with:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new StackSpec).execute()
 * </pre>
 *
 * <p>
 * It will run only the second test and report that the first test was ignored:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">A Stack</span>
 * <span class="stYellow">- should pop values in last-in-first-out order !!! IGNORED !!!</span>
 * <span class="stGreen">- should throw NoSuchElementException if an empty stack is popped</span>
 * </pre>
 *
 * <h2>Informers</h2>
 *
 * <p>
 * One of the parameters to the <code>run</code> method is a <code>Reporter</code>, which
 * will collect and report information about the running suite of tests.
 * Information about suites and tests that were run, whether tests succeeded or failed, 
 * and tests that were ignored will be passed to the <code>Reporter</code> as the suite runs.
 * Most often the reporting done by default by <code>FunSuite</code>'s methods will be sufficient, but
 * occasionally you may wish to provide custom information to the <code>Reporter</code> from a test.
 * For this purpose, an <code>Informer</code> that will forward information to the current <code>Reporter</code>
 * is provided via the <code>info</code> parameterless method.
 * You can pass the extra information to the <code>Informer</code> via one of its <code>apply</code> methods.
 * The <code>Informer</code> will then pass the information to the <code>Reporter</code> via an <code>InfoProvided</code> event.
 * Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Spec
 *
 * class ExampleSpec extends Spec {
 *
 *   describe("The Scala language") {
 *
 *     it("should add correctly") {
 *       val sum = 1 + 1
 *       assert(sum === 2)
 *       info("Addition seems to work")
 *     }
 *
 *     it("should subtract correctly") {
 *       val diff = 7 - 2
 *       assert(diff === 5)
 *     }
 *   }
 * }
 * </pre>
 *
 * If you run this <code>Spec</code> from the interpreter, you will see the following message
 * included in the printed report:
 *
 * <pre class="stREPL">
 * <span class="stGreen">ExampleSpec:
 * The Scala language
 * - should add correctly
 *   + Addition seems to work
 * - should subtract correctly</span> 
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
 * </p>
 *
 * <p>
 * You can mark a test as pending in <code>Spec</code> by placing "<code>(pending)</code>" after the 
 * test name, like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Spec
 * import scala.collection.mutable.Stack
 *
 * class StackSpec extends Spec {
 *
 *   describe("A Stack") {
 *
 *     it("should pop values in last-in-first-out order") {
 *       val stack = new Stack[Int]
 *       stack.push(1)
 *       stack.push(2)
 *       assert(stack.pop() === 2)
 *       assert(stack.pop() === 1)
 *     }
 *
 *     it("should throw NoSuchElementException if an empty stack is popped") (pending)
 *   }
 * }
 * </pre>
 *
 * <p>
 * (Note: "<code>(pending)</code>" is the body of the test. Thus the test contains just one statement, an invocation
 * of the <code>pending</code> method, which throws <code>TestPendingException</code>.)
 * If you run this version of <code>StackSpec</code> with:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new StackSpec).execute()
 * </pre>
 *
 * <p>
 * It will run both tests, but report that the test named "<code>A stack should pop values in last-in-first-out order</code>" is pending. You'll see:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">A Stack 
 * - should pop values in last-in-first-out order</span>
 * <span class="stYellow">- should throw NoSuchElementException if an empty stack is popped (pending)</span>
 * </pre>
 * 
 * <h2>Tagging tests</h2>
 *
 * <p>
 * A <code>Spec</code>'s tests may be classified into groups by <em>tagging</em> them with string names.
 * As with any suite, when executing a <code>Spec</code>, groups of tests can
 * optionally be included and/or excluded. To tag a <code>Spec</code>'s tests,
 * you pass objects that extend abstract class <code>org.scalatest.Tag</code> to the methods
 * that register tests, <code>it</code> and <code>ignore</code>. Class <code>Tag</code> takes one parameter,
 * a string name.  If you have
 * created Java annotation interfaces for use as group names in direct subclasses of <code>org.scalatest.Suite</code>,
 * then you will probably want to use group names on your <code>Spec</code>s that match. To do so, simply 
 * pass the fully qualified names of the Java interfaces to the <code>Tag</code> constructor. For example, if you've
 * defined Java annotation interfaces with fully qualified names, <code>com.mycompany.tags.SlowTest</code> and <code>com.mycompany.tags.DbTest</code>, then you could
 * create matching groups for <code>Spec</code>s like this:
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
 * Given these definitions, you could place <code>Spec</code> tests into groups like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.Spec
 *
 * class ExampleSpec extends Spec {
 *
 *   it("should add correctly", SlowTest) {
 *     val sum = 1 + 1
 *     assert(sum === 2)
 *   }
 *
 *   it("should subtract correctly", SlowTest, DbTest) {
 *     val diff = 4 - 1
 *     assert(diff === 3)
 *   }
 * }
 * </pre>
 *
 * <p>
 * This code marks both tests with the <code>com.mycompany.tags.SlowTest</code> tag, 
 * and test <code>"should subtract correctly"</code> with the <code>com.mycompany.tags.DbTest</code> tag.
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
 * import org.scalatest.Spec
 * import collection.mutable.ListBuffer
 *
 * class ExampleSpec extends Spec {
 * 
 *   def fixture =
 *     new {
 *       val builder = new StringBuilder("ScalaTest is ")
 *       val buffer = new ListBuffer[String]
 *     }
 * 
 *   describe("Testing") {
 *
 *     it("should be easy") {
 *       val f = fixture
 *       f.builder.append("easy!")
 *       assert(f.builder.toString === "ScalaTest is easy!")
 *       assert(f.buffer.isEmpty)
 *       f.buffer += "sweet"
 *     }
 * 
 *     it("should be fun") {
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
 * import org.scalatest.Spec
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSpec extends Spec {
 * 
 *   trait Fixture {
 *     val builder = new StringBuilder("ScalaTest is ")
 *     val buffer = new ListBuffer[String]
 *   }
 * 
 *   describe("Testing") {
 *
 *     it("should be easy") {
 *       new Fixture {
 *         builder.append("easy!")
 *         assert(builder.toString === "ScalaTest is easy!")
 *         assert(buffer.isEmpty)
 *         buffer += "sweet"
 *       }
 *     }
 * 
 *     it("should be fun") {
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
 * import org.scalatest.Spec
 * import org.scalatest.OneInstancePerTest
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSpec extends Spec with OneInstancePerTest {
 * 
 *   val builder = new StringBuilder("ScalaTest is ")
 *   val buffer = new ListBuffer[String]
 * 
 *   describe("Testing") {
 *
 *     it("should be easy") {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 * 
 *     it("should be fun") {
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
 * import org.scalatest.Spec
 * import org.scalatest.BeforeAndAfter
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSpec extends Spec with BeforeAndAfter {
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
 *   describe("Testing") {
 *
 *     it("should be easy") {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 * 
 *     it("should be fun") {
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
 * import org.scalatest.Spec
 * import collection.mutable.ListBuffer
 *
 * class ExampleSpec extends Spec {
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
 *   describe("Testing") {
 *
 *     it("should be easy") {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 *
 *     it("should be fun") {
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
 * To use the loan pattern, you can extend <code>FixtureSpec</code> (from the <code>org.scalatest.fixture</code> package) instead of
 * <code>Spec</code>. Each test in a <code>FixtureSpec</code> takes a fixture as a parameter, allowing you to pass the fixture into
 * the test. You must indicate the type of the fixture parameter by specifying <code>FixtureParam</code>, and implement a
 * <code>withFixture</code> method that takes a <code>OneArgTest</code>. This <code>withFixture</code> method is responsible for
 * invoking the one-arg test function, so you can perform fixture set up before, and clean up after, invoking and passing
 * the fixture into the test function. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.fixture.FixtureSpec
 * import java.io.FileWriter
 * import java.io.File
 * 
 * class ExampleSpec extends FixtureSpec {
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
 *   describe("Testing") {
 *
 *     it("should be easy") { writer =>
 *       writer.write("Hello, test!")
 *       writer.flush()
 *       assert(new File(tmpFile).length === 12)
 *     }
 * 
 *     it("should be fun") { writer =>
 *       writer.write("Hi, test!")
 *       writer.flush()
 *       assert(new File(tmpFile).length === 9)
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * For more information, see the <a href="fixture/FixtureSpec.html">documentation for <code>FixtureSpec</code></a>.
 * </p>
 *
 * <a name="differentFixtures"></a><h2>Providing different fixtures to different tests</h2>
 * 
 * <p>
 * If different tests in the same <code>Spec</code> require different fixtures, you can combine the previous techniques and
 * provide each test with just the fixture or fixtures it needs. Here's an example in which a <code>StringBuilder</code> and a
 * <code>ListBuffer</code> are provided via fixture traits, and file writer (that requires cleanup) is provided via the loan pattern:
 * </p>
 *
 * <pre class="stHighlight">
 * import java.io.FileWriter
 * import java.io.File
 * import collection.mutable.ListBuffer
 * import org.scalatest.Spec
 * 
 * class ExampleSpec extends Spec {
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
 *   describe("Testing") {
 *
 *     it("should be productive") { // This test needs the StringBuilder fixture
 *       new Builder {
 *         builder.append("productive!")
 *         assert(builder.toString === "ScalaTest is productive!")
 *       }
 *     }
 * 
 *     it("should be readable") { // This test needs the ListBuffer[String] fixture
 *       new Buffer {
 *         buffer += ("readable!")
 *         assert(buffer === List("ScalaTest", "is", "readable!"))
 *       }
 *     }
 * 
 *     it("should be friendly") { // This test needs the FileWriter fixture
 *       withWriter { writer =>
 *         writer.write("Hello, user!")
 *         writer.flush()
 *         assert(new File(tmpFile).length === 12)
 *     }
 *   }
 * 
 *     it("should be clear and concise") { // This test needs the StringBuilder and ListBuffer
 *       new Builder with Buffer {
 *         builder.append("clear!")
 *         buffer += ("concise!")
 *         assert(builder.toString === "ScalaTest is clear!")
 *         assert(buffer === List("ScalaTest", "is", "concise!"))
 *       }
 *     }
 * 
 *     it("should be composable") { // This test needs all three fixtures
 *       new Builder with Buffer {
 *         builder.append("clear!")
 *         buffer += ("concise!")
 *         assert(builder.toString === "ScalaTest is clear!")
 *         assert(buffer === List("ScalaTest", "is", "concise!"))
 *         withWriter { writer =>
 *           writer.write(builder.toString)
 *           writer.flush()
 *           assert(new File(tmpFile).length === 19)
 *         }
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * In the previous example, <code>should be productive</code> uses only the <code>StringBuilder</code> fixture, so it just instantiates
 * a <code>new Builder</code>, whereas <code>should be readable</code> uses only the <code>ListBuffer</code> fixture, so it just intantiates
 * a <code>new Buffer</code>. <code>should be friendly</code> needs just the <code>FileWriter</code> fixture, so it invokes
 * <code>withWriter</code>, which prepares and passes a <code>FileWriter</code> to the test (and takes care of closing it afterwords).
 * </p>
 *
 * <p>
 * Two tests need multiple fixtures: <code>should be clear and concise</code> needs both the <code>StringBuilder</code> and the
 * <code>ListBuffer</code>, so it instantiates a class that mixes in both fixture traits with <code>new Builder with Buffer</code>.
 * <code>should be composable</code> needs all three fixtures, so in addition to <code>new Builder with Buffer</code> it also invokes
 * <code>withWriter</code>, wrapping just the of the test code that needs the fixture.
 * </p>
 *
 * <p>
 * Note that in this case, the loan pattern is being implemented via the <code>withWriter</code> method that takes a function, not
 * by overriding <code>FixtureSpec</code>'s <code>withFixture(OneArgTest)</code> method. <code>FixtureSpec</code> makes the most sense
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
 * describe("A user") {
 *   it("should be able to log in") {
 *     withDataInDatabase {
 *       // test user logging in scenario
 *     }
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
 * import org.scalatest.Spec
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
 * class ExampleSpec extends Spec with Builder with Buffer {
 * 
 *   describe("Testing") {
 *
 *     it("should be easy") {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 * 
 *     it("should be fun") {
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
 * class Example2Spec extends Spec with Buffer with Builder
 * </pre>
 *
 * <p>
 * And if you only need one fixture you mix in only that trait:
 * </p>
 *
 * <pre class="stHighlight">
 * class Example3Spec extends Spec with Builder
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
 * import org.scalatest.Spec
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
 * class ExampleSpec extends Spec with Builder with Buffer {
 * 
 *   describe("Testing") {
 *
 *     it("should be easy") {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 * 
 *     it("should be fun") {
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
 * <a name="SharedTests"></a><h2>Shared tests</h2>
 *
 * <p>
 * Sometimes you may want to run the same test code on different fixture objects. In other words, you may want to write tests that are "shared"
 * by different fixture objects.
 * To accomplish this in a <code>Spec</code>, you first place shared tests in <em>behavior functions</em>. These behavior functions will be
 * invoked during the construction phase of any <code>Spec</code> that uses them, so that the tests they contain will be registered as tests in that <code>Spec</code>.
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
 * <em>etc</em>. You may find you have several tests that make sense any time the stack is non-empty. Thus you'd ideally want to run
 * those same tests for three stack fixture objects: a full stack, a stack with a one item, and a stack with one item less than
 * capacity. With shared tests, you can factor these tests out into a behavior function, into which you pass the
 * stack fixture to use when running the tests. So in your <code>Spec</code> for stack, you'd invoke the
 * behavior function three times, passing in each of the three stack fixtures so that the shared tests are run for all three fixtures. You
 * can define a behavior function that encapsulates these shared tests inside the <code>Spec</code> that uses them. If they are shared
 * between different <code>Spec</code>s, however, you could also define them in a separate trait that is mixed into each <code>Spec</code> that uses them.
 * </p>
 *
 * <p>
 * <a name="StackBehaviors">For</a> example, here the <code>nonEmptyStack</code> behavior function (in this case, a behavior <em>method</em>) is defined in a trait along with another
 * method containing shared tests for non-full stacks:
 * </p>
 * 
 * <pre class="stHighlight">
 * trait StackBehaviors { this: Spec =>
 * 
 *   def nonEmptyStack(stack: Stack[Int], lastItemAdded: Int) {
 * 
 *     it("should be non-empty") {
 *       assert(!stack.empty)
 *     }  
 * 
 *     it("should return the top item on peek") {
 *       assert(stack.peek === lastItemAdded)
 *     }
 *   
 *     it("should not remove the top item on peek") {
 *       val size = stack.size
 *       assert(stack.peek === lastItemAdded)
 *       assert(stack.size === size)
 *     }
 *   
 *     it("should remove the top item on pop") {
 *       val size = stack.size
 *       assert(stack.pop === lastItemAdded)
 *       assert(stack.size === size - 1)
 *     }
 *   }
 *   
 *   def nonFullStack(stack: Stack[Int]) {
 *       
 *     it("should not be full") {
 *       assert(!stack.full)
 *     }
 *       
 *     it("should add to the top on push") {
 *       val size = stack.size
 *       stack.push(7)
 *       assert(stack.size === size + 1)
 *       assert(stack.peek === 7)
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * Given these behavior functions, you could invoke them directly, but <code>Spec</code> offers a DSL for the purpose,
 * which looks like this:
 * </p>
 *
 * <pre class="stHighlight">
 * it should behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
 * it should behave like nonFullStack(stackWithOneItem)
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
 * it should behave like nonEmptyStack // assuming lastValuePushed is also in scope inside nonEmptyStack
 * it should behave like nonFullStack
 * </pre>
 *
 * <p>
 * The recommended style, however, is the functional, pass-all-the-needed-values-in style. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * class SharedTestExampleSpec extends Spec with StackBehaviors {
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
 *   describe("A Stack") {
 * 
 *     describe("(when empty)") {
 *       
 *       it("should be empty") {
 *         assert(emptyStack.empty)
 *       }
 * 
 *       it("should complain on peek") {
 *         intercept[IllegalStateException] {
 *           emptyStack.peek
 *         }
 *       }
 * 
 *       it("should complain on pop") {
 *         intercept[IllegalStateException] {
 *           emptyStack.pop
 *         }
 *       }
 *     }
 * 
 *     describe("(with one item)") {
 *       it should behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
 *       it should behave like nonFullStack(stackWithOneItem)
 *     }
 *     
 *     describe("(with one item less than capacity)") {
 *       it should behave like nonEmptyStack(stackWithOneItemLessThanCapacity, lastValuePushed)
 *       it should behave like nonFullStack(stackWithOneItemLessThanCapacity)
 *     }
 * 
 *     describe("(full)") {
 *       
 *       it("should be full") {
 *         assert(fullStack.full)
 *       }
 * 
 *       it should behave like nonEmptyStack(fullStack, lastValuePushed)
 * 
 *       it("should complain on a push") {
 *         intercept[IllegalStateException] {
 *           fullStack.push(10)
 *         }
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
 * scala> (new StackSpec).execute()
 * <span class="stGreen">A Stack (when empty) 
 * - should be empty
 * - should complain on peek
 * - should complain on pop
 * A Stack (with one item) 
 * - should be non-empty
 * - should return the top item on peek
 * - should not remove the top item on peek
 * - should remove the top item on pop
 * - should not be full
 * - should add to the top on push
 * A Stack (with one item less than capacity) 
 * - should be non-empty
 * - should return the top item on peek
 * - should not remove the top item on peek
 * - should remove the top item on pop
 * - should not be full
 * - should add to the top on push
 * A Stack (full) 
 * - should be full
 * - should be non-empty
 * - should return the top item on peek
 * - should not remove the top item on peek
 * - should remove the top item on pop
 * - should complain on a push</span>
 * </pre>
 * 
 * <p>
 * One thing to keep in mind when using shared tests is that in ScalaTest, each test in a suite must have a unique name.
 * If you register the same tests repeatedly in the same suite, one problem you may encounter is an exception at runtime
 * complaining that multiple tests are being registered with the same test name. A good way to solve this problem in a <code>Spec</code> is to surround
 * each invocation of a behavior function with a <code>describe</code> clause, which will prepend a string to each test name.
 * For example, the following code in a <code>Spec</code> would register a test with the name <code>"A Stack (when empty) should be empty"</code>:
 * </p>
 *
 * <pre class="stHighlight">
 *   describe("A Stack") {
 * 
 *     describe("(when empty)") {
 *       
 *       it("should be empty") {
 *         assert(emptyStack.empty)
 *       }
 *       // ...
 * </pre>
 *
 * <p>
 * If the <code>"should be empty"</code> test was factored out into a behavior function, it could be called repeatedly so long
 * as each invocation of the behavior function is inside a different set of <code>describe</code> clauses.
 *
 * @author Bill Venners
 */
trait Spec extends Suite { thisSuite =>

  private final val engine = new Engine("concurrentSpecMod", "Spec")
  import engine._

  /**
   * Returns an <code>Informer</code> that during test execution will forward strings (and other objects) passed to its
   * <code>apply</code> method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * <code>Spec</code> is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * throw an exception. This method can be called safely by any thread.
   */
  implicit protected def info: Informer = atomicInformer.get

  /**
   * Class that, via an instance referenced from the <code>it</code> field,
   * supports test (and shared test) registration in <code>Spec</code>s.
   *
   * <p>
   * This class supports syntax such as the following test registration:
   * </p>
   *
   * <pre class="stExamples">
   * it("should be empty")
   * ^
   * </pre>
   *
   * <p>
   * and the following shared test registration:
   * </p>
   *
   * <pre class="stExamples">
   * it should behave like nonFullStack(stackWithOneItem)
   * ^
   * </pre>
   *
   * <p>
   * For more information and examples, see the <a href="Spec.html">main documentation for <code>Spec</code></a>.
   * </p>
   */
  protected class ItWord {

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
    def apply(specText: String, testTags: Tag*)(testFun: => Unit) {
      registerTest(specText, testFun _, "itCannotAppearInsideAnotherIt", "Spec.scala", "apply", testTags: _*)
    }

    /**
     * Supports the registration of shared tests.
     *
     * <p>
     * This method supports syntax such as the following:
     * </p>
     *
     * <pre class="stExamples">
     * it should behave like nonFullStack(stackWithOneItem)
     *    ^
     * </pre>
     *
     * <p>
     * For examples of shared tests, see the <a href="Spec.html#SharedTests">Shared tests section</a>
     * in the main documentation for trait <code>Spec</code>.
     * </p>
     */
    def should(behaveWord: BehaveWord) = behaveWord

    /**
     * Supports the registration of shared tests.
     *
     * <p>
     * This method supports syntax such as the following:
     * </p>
     *
     * <pre class="stExamples">
     * it must behave like nonFullStack(stackWithOneItem)
     *    ^
     * </pre>
     *
     * <p>
     * For examples of shared tests, see the <a href="Spec.html#SharedTests">Shared tests section</a>
     * in the main documentation for trait <code>Spec</code>.
     * </p>
     */
    def must(behaveWord: BehaveWord) = behaveWord
  }

  /**
   * Supports test (and shared test) registration in <code>Spec</code>s.
   *
   * <p>
   * This field supports syntax such as the following:
   * </p>
   *
   * <pre class="stExamples">
   * it("should be empty")
   * ^
   * </pre>
   *
   * <pre> class="stExamples"
   * it should behave like nonFullStack(stackWithOneItem)
   * ^
   * </pre>
   *
   * <p>
   * For more information and examples of the use of the <code>it</code> field, see the main documentation for this trait.
   * </p>
   */
  protected val it = new ItWord

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
  protected def ignore(testText: String, testTags: Tag*)(testFun: => Unit) {
    registerIgnoredTest(testText, testFun _, "ignoreCannotAppearInsideAnIt", "Spec.scala", "ignore", testTags: _*)
  }

  /**
   * Describe a &#8220;subject&#8221; being specified and tested by the passed function value. The
   * passed function value may contain more describers (defined with <code>describe</code>) and/or tests
   * (defined with <code>it</code>). This trait's implementation of this method will register the
   * description string and immediately invoke the passed function.
   */
  protected def describe(description: String)(fun: => Unit) {

    registerNestedBranch(description, None, fun, "describeCannotAppearInsideAnIt", "Spec.scala", "describe")
  }

  /**
   * An immutable <code>Set</code> of test names. If this <code>Spec</code> contains no tests, this method returns an
   * empty <code>Set</code>.
   *
   * <p>
   * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's
   * iterator will return those names in the order in which the tests were registered. Each test's name is composed
   * of the concatenation of the text of each surrounding describer, in order from outside in, and the text of the
   * example itself, with all components separated by a space. For example, consider this <code>Spec</code>:
   * </p>
   *
   * <pre class="stHighlight">
   * import org.scalatest.Spec
   *
   * class StackSpec extends Spec{
   *   describe("A Stack") {
   *     describe("(when not empty)") {
   *       it("must allow me to pop") {}
   *     }
   *     describe("(when not full)") {
   *       it("must allow me to push") {}
   *     }
   *   }
   * }
   * </pre>
   *
   * <p>
   * Invoking <code>testNames</code> on this <code>Spec</code> will yield a set that contains the following
   * two test name strings:
   * </p>
   *
   * <pre class="stExamples">
   * "A Stack (when not empty) must allow me to pop"
   * "A Stack (when not full) must allow me to push"
   * </pre>
   */
  override def testNames: Set[String] = {
    // I'm returning a ListSet here so that they tests will be run in registration order
    ListSet(atomic.get.testNamesList.toArray: _*)
  }

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

    runTestImpl(thisSuite, testName, reporter, stopper, configMap, tracker, true, invokeWithFixture)
  }

  /**
   * A <code>Map</code> whose keys are <code>String</code> tag names to which tests in this <code>Spec</code> belong, and values
   * the <code>Set</code> of test names that belong to each tag. If this <code>Spec</code> contains no tags, this method returns an empty <code>Map</code>.
   *
   * <p>
   * This trait's implementation returns tags that were passed as strings contained in <code>Tag</code> objects passed to 
   * methods <code>test</code> and <code>ignore</code>. 
   * </p>
   */
  override def tags: Map[String, Set[String]] = atomic.get.tagsMap

  /**
   * Run zero to many of this <code>Spec</code>'s tests.
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

    runTestsImpl(thisSuite, testName, reporter, stopper, filter, configMap, distributor, tracker, info, true, runTest)
  }

  override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
      configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

    runImpl(thisSuite, testName, reporter, stopper, filter, configMap, distributor, tracker, super.run)
  }

  /**
   * Supports shared test registration in <code>Spec</code>s.
   *
   * <p>
   * This field supports syntax such as the following:
   * </p>
   *
   * <pre class="stExamples">
   * it should behave like nonFullStack(stackWithOneItem)
   *           ^
   * </pre>
   *
   * <p>
   * For more information and examples of the use of <cod>behave</code>, see the <a href="#SharedTests">Shared tests section</a>
   * in the main documentation for this trait.
   * </p>
   */
  protected val behave = new BehaveWord
}
