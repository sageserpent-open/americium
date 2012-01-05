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
import FixtureNodeFamily._
import verb.{CanVerb, ResultOfAfterWordApplication, ShouldVerb, BehaveWord, MustVerb,
  StringVerbBlockRegistration}
import scala.collection.immutable.ListSet
import org.scalatest.StackDepthExceptionHelper.getStackDepth
import java.util.concurrent.atomic.AtomicReference
import java.util.ConcurrentModificationException
import org.scalatest.events._
import Suite.anErrorThatShouldCauseAnAbort

/**
 * A sister trait to <code>org.scalatest.FreeSpec</code> that can pass a fixture object into its tests.
 *
 * <p>
 * The purpose of <code>FixtureFreeSpec</code> and its subtraits is to facilitate writing tests in
 * a functional style. Some users may prefer writing tests in a functional style in general, but one
 * particular use case is parallel test execution (See <a href="../ParallelTestExecution.html">ParallelTestExecution</a>). To run
 * tests in parallel, your test class must
 * be thread safe, and a good way to make it thread safe is to make it functional. A good way to
 * write tests that need common fixtures in a functional style is to pass the fixture objects into the tests,
 * the style enabled by the <code>FixtureSuite</code> family of traits.
 * </p>
 *
 * <p>
 * Trait <code>FixtureFreeSpec</code> behaves similarly to trait <code>org.scalatest.FreeSpec</code>, except that tests may have a
 * fixture parameter. The type of the
 * fixture parameter is defined by the abstract <code>FixtureParam</code> type, which is declared as a member of this trait.
 * This trait also declares an abstract <code>withFixture</code> method. This <code>withFixture</code> method
 * takes a <code>OneArgTest</code>, which is a nested trait defined as a member of this trait.
 * <code>OneArgTest</code> has an <code>apply</code> method that takes a <code>FixtureParam</code>.
 * This <code>apply</code> method is responsible for running a test.
 * This trait's <code>runTest</code> method delegates the actual running of each test to <code>withFixture</code>, passing
 * in the test code to run via the <code>OneArgTest</code> argument. The <code>withFixture</code> method (abstract in this trait) is responsible
 * for creating the fixture argument and passing it to the test function.
 * </p>
 * 
 * <p>
 * Subclasses of this trait must, therefore, do three things differently from a plain old <code>org.scalatest.FreeSpec</code>:
 * </p>
 * 
 * <ol>
 * <li>define the type of the fixture parameter by specifying type <code>FixtureParam</code></li>
 * <li>define the <code>withFixture(OneArgTest)</code> method</li>
 * <li>write tests that take a fixture parameter</li>
 * <li>(You can also define tests that don't take a fixture parameter.)</li>
 * </ol>
 *
 * <p>
 * Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.fixture.FixtureFreeSpec
 * import collection.mutable.Stack
 * import java.util.NoSuchElementException
 *
 * class StackSpec extends FixtureFreeSpec {
 *
 *   // 1. define type FixtureParam
 *   type FixtureParam = Stack[Int]
 *
 *   // 2. define the withFixture method
 *   def withFixture(test: OneArgTest) {
 *     val stack = new Stack[Int]
 *     stack.push(1)
 *     stack.push(2)
 *     test(stack) // "loan" the fixture to the test
 *   }
 *
 *   "A Stack" - {
 *
 *     // 3. write tests that take a fixture parameter
 *     "should pop a value" in { stack =>
 *       val top = stack.pop()
 *       assert(top === 2)
 *       assert(stack.size === 1)
 *     }
 *
 *     "should push a value" in { stack =>
 *       stack.push(9)
 *       assert(stack.size === 3)
 *       assert(stack.head === 9)
 *     }
 *
 *     // 4. You can also write tests that don't take a fixture parameter.
 *     "should complain if popped while empty" () {
 *       intercept[NoSuchElementException] {
 *         (new Stack[Int]).pop()
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * In the previous example, <code>withFixture</code> creates and initializes a stack, then invokes the test function, passing in
 * the stack.  In addition to setting up a fixture before a test, the <code>withFixture</code> method also allows you to
 * clean it up afterwards, if necessary. If you need to do some clean up that must happen even if a test
 * fails, you should invoke the test function from inside a <code>try</code> block and do the cleanup in a
 * <code>finally</code> clause, like this:
 * </p>
 *
 * <pre class="stHighlight">
 * def withFixture(test: OneArgTest) {
 *   val resource = someResource.open() // set up the fixture
 *   try {
 *     test(resource) // if the test fails, test(...) will throw an exception
 *   }
 *   finally {
 *     // clean up the fixture no matter whether the test succeeds or fails
 *     resource.close()
 *   }
 * }
 * </pre>
 *
 * <p>
 * The reason you must perform cleanup in a <code>finally</code> clause is that <code>withFixture</code> is called by
 * <code>runTest</code>, which expects an exception to be thrown to indicate a failed test. Thus when you invoke
 * the <code>test</code> function, it may complete abruptly with an exception. The <code>finally</code> clause will
 * ensure the fixture cleanup happens as that exception propagates back up the call stack to <code>runTest</code>.
 * </p>
 *
 * <p>
 * If the fixture you want to pass into your tests consists of multiple objects, you will need to combine
 * them into one object to use this trait. One good approach to passing multiple fixture objects is
 * to encapsulate them in a case class. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.fixture.FixtureFreeSpec
 * import scala.collection.mutable.ListBuffer
 *
 * class ExampleSpec extends FixtureFreeSpec {
 *
 *   case class F(builder: StringBuilder, buffer: ListBuffer[String])
 *   type FixtureParam = F
 *
 *   def withFixture(test: OneArgTest) {
 *
 *     // Create needed mutable objects
 *     val stringBuilder = new StringBuilder("ScalaTest is ")
 *     val listBuffer = new ListBuffer[String]
 *
 *     // Invoke the test function, passing in the mutable objects
 *     test(F(stringBuilder, listBuffer))
 *   }
 *
 *   "Testing" - {
 *     "should be easy" in { f =>
 *       f.builder.append("easy!")
 *       assert(f.builder.toString === "ScalaTest is easy!")
 *       assert(f.buffer.isEmpty)
 *       f.buffer += "sweet"
 *     }
 *
 *     "should be fun" in { f =>
 *       f.builder.append("fun!")
 *       assert(f.builder.toString === "ScalaTest is fun!")
 *       assert(f.buffer.isEmpty)
 *     }
 *   }
 * }
 * </pre>
 *
 * <h2>Configuring fixtures and tests</h2>
 * 
 * <p>
 * Sometimes you may want to write tests that are configurable. For example, you may want to write
 * a suite of tests that each take an open temp file as a fixture, but whose file name is specified
 * externally so that the file name can be can be changed from run to run. To accomplish this
 * the <code>OneArgTest</code> trait has a <code>configMap</code>
 * method, which will return a <code>Map[String, Any]</code> from which configuration information may be obtained.
 * The <code>runTest</code> method of this trait will pass a <code>OneArgTest</code> to <code>withFixture</code>
 * whose <code>configMap</code> method returns the <code>configMap</code> passed to <code>runTest</code>.
 * Here's an example in which the name of a temp file is taken from the passed <code>configMap</code>:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.fixture.FixtureFreeSpec
 * import java.io.FileReader
 * import java.io.FileWriter
 * import java.io.File
 * 
 * class ExampleSpec extends FixtureFreeSpec {
 *
 *   type FixtureParam = FileReader
 *   def withFixture(test: OneArgTest) {
 *
 *     require(
 *       test.configMap.contains("TempFileName"),
 *       "This suite requires a TempFileName to be passed in the configMap"
 *     )
 *
 *     // Grab the file name from the configMap
 *     val FileName = test.configMap("TempFileName").asInstanceOf[String]
 *
 *     // Set up the temp file needed by the test
 *     val writer = new FileWriter(FileName)
 *     try {
 *       writer.write("Hello, test!")
 *     }
 *     finally {
 *       writer.close()
 *     }
 *
 *     // Create the reader needed by the test
 *     val reader = new FileReader(FileName)
 *  
 *     try {
 *       // Run the test using the temp file
 *       test(reader)
 *     }
 *     finally {
 *       // Close and delete the temp file
 *       reader.close()
 *       val file = new File(FileName)
 *       file.delete()
 *     }
 *   }
 * 
 *   "A file" - {
 *     "can be read" { reader =>
 *       var builder = new StringBuilder
 *       var c = reader.read()
 *       while (c != -1) {
 *         builder.append(c.toChar)
 *         c = reader.read()
 *       }
 *       assert(builder.toString === "Hello, test!")
 *     }
 *   }
 *
 *   "The first char of a file" - {
 *     "can be read" { reader =>
 *       assert(reader.read() === 'H')
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * If you want to pass into each test the entire <code>configMap</code> that was passed to <code>runTest</code>, you 
 * can mix in trait <code>ConfigMapFixture</code>. See the <a href="ConfigMapFixture.html">documentation
 * for <code>ConfigMapFixture</code></a> for the details, but here's a quick
 * example of how it looks:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.fixture.FixtureFreeSpec
 * import org.scalatest.fixture.ConfigMapFixture
 *
 * class ExampleSpec extends FixtureFreeSpec with ConfigMapFixture {
 *
 *   "The config map" - {
 *
 *     "must contain hello" in { configMap =>
 *       // Use the configMap passed to runTest in the test
 *       assert(configMap.contains("hello"))
 *     }
 *
 *     "must contain world" in { configMap =>
 *       assert(configMap.contains("world"))
 *     }
 *   }
 * }
 * </pre>
 *
 * <h2>Providing multiple fixtures</h2>
 *
 * <p>
 * If different tests in the same <code>FixtureFreeSpec</code> need different shared fixtures, you can use the <em>loan pattern</em> to supply to
 * each test just the fixture or fixtures it needs. First select the most commonly used fixture objects and pass them in via the
 * <code>FixtureParam</code>. Then for each remaining fixture needed by multiple tests, create a <em>with&lt;fixture name&gt;</em>
 * method that takes a function you will use to pass the fixture to the test. Lasty, use the appropriate
 * <em>with&lt;fixture name&gt;</em> method or methods in each test.
 * </p>
 *
 * <p>
 * In the following example, the <code>FixtureParam</code> is set to <code>Map[String, Any]</code> by mixing in <code>ConfigMapFixture</code>.
 * The <code>withFixture</code> method in trait <code>ConfigMapFixture</code> will pass the config map to any test that needs it.
 * In addition, some tests in the following example need a <code>Stack[Int]</code> and others a <code>Stack[String]</code>.
 * The <code>withIntStack</code> method takes
 * care of supplying the <code>Stack[Int]</code> to those tests that need it, and the <code>withStringStack</code> method takes care
 * of supplying the <code>Stack[String]</code> fixture. Here's how it looks:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.fixture.FixtureFreeSpec
 * import org.scalatest.fixture.ConfigMapFixture
 * import collection.mutable.Stack
 *
 * class StackSpec extends FixtureFreeSpec with ConfigMapFixture {
 *
 *   def withIntStack(test: Stack[Int] => Any) {
 *     val stack = new Stack[Int]
 *     stack.push(1)
 *     stack.push(2)
 *     test(stack) // "loan" the Stack[Int] fixture to the test
 *   }
 *
 *   def withStringStack(test: Stack[String] => Any) {
 *     val stack = new Stack[String]
 *     stack.push("one")
 *     stack.push("two")
 *     test(stack) // "loan" the Stack[String] fixture to the test
 *   }
 *
 *   "A Stack" - {
 *
 *     "should pop an Int value" in { () => // This test doesn't need the configMap fixture, ...
 *       withIntStack { stack =>
 *         val top = stack.pop() // But it needs the Stack[Int] fixture.
 *         assert(top === 2)
 *         assert(stack.size === 1)
 *       }
 *     }
 *
 *     "should push an Int value" in { configMap =>
 *       withIntStack { stack =>
 *         val iToPush = // This test uses the configMap fixture...
 *           configMap("IntToPush").toString.toInt
 *         stack.push(iToPush) // And also uses the Stack[Int] fixture.
 *         assert(stack.size === 3)
 *         assert(stack.head === iToPush)
 *       }
 *     }
 *
 *     "should pop a String value" in { () => // This test doesn't need the configMap fixture, ...
 *       withStringStack { stack =>
 *         val top = stack.pop() // But it needs the Stack[String] fixture.
 *         assert(top === "two")
 *         assert(stack.size === 1)
 *       }
 *     }
 *
 *     "should push a String value" in { configMap =>
 *       withStringStack { stack =>
 *         val sToPush = // This test uses the configMap fixture...
 *           configMap("StringToPush").toString
 *         stack.push(sToPush) // And also uses the Stack[Int] fixture.
 *         assert(stack.size === 3)
 *         assert(stack.head === sToPush)
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * If you run the previous class in the Scala interpreter, you'll see:
 * </p>
 *
 * <pre class="stREPL">
 * scala> import org.scalatest._
 * import org.scalatest._
 *
 * scala> run(new StackSpec, configMap = Map("IntToPush" -> 9, "StringToPush" -> "nine"))
 * <span class="stGreen">StackSpec:
 * A Stack
 * - should pop an Int value
 * - should push an Int value
 * - should pop a String value
 * - should push a String value</span>
 * </pre>
 *
 * @author Bill Venners
 */
trait FixtureFreeSpec extends FixtureSuite { thisSuite =>

  private final val engine = new FixtureEngine[FixtureParam]("concurrentFixtureFreeSpecMod", "FixtureFreeSpec")
  import engine._

  /**
   * Returns an <code>Informer</code> that during test execution will forward strings (and other objects) passed to its
   * <code>apply</code> method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * <code>FixtureFreeSpec</code> is being executed, such as from inside a test function, it will forward the information to
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
  private def registerTestToRun(specText: String, testTags: List[Tag], testFun: FixtureParam => Any) {
    // TODO: This is what was being used before but it is wrong
    registerTest(specText, testFun, "itCannotAppearInsideAnotherIt", "FixtureFreeSpec.scala", "it", testTags: _*)
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
  private def registerTestToIgnore(specText: String, testTags: List[Tag], testFun: FixtureParam => Any) {
    // TODO: This is how these were, but it needs attention. Mentions "it".
    registerIgnoredTest(specText, testFun, "ignoreCannotAppearInsideAnIt", "FreeSpec.scala", "ignore", testTags: _*)
  }
   /*
  private def registerBranch(description: String, childPrefix: Option[String], fun: () => Unit) {

    // TODO: Fix the resource name and method name
    registerNestedBranch(description, childPrefix, fun(), "describeCannotAppearInsideAnIt", "FixtureFreeSpec.scala", "describe")
  }  */

  /**
   * Class that supports the registration of tagged tests.
   *
   * <p>
   * Instances of this class are returned by the <code>taggedAs</code> method of 
   * class <code>FreeSpecStringWrapper</code>.
   * </p>
   *
   * @author Bill Venners
   */
  protected final class ResultOfTaggedAsInvocationOnString(specText: String, tags: List[Tag]) {

    /**
     * Supports tagged test registration.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" taggedAs(SlowTest) in { fixture => ... }
     *                                       ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     */
    def in(testFun: FixtureParam => Any) {
      registerTestToRun(specText, tags, testFun)
    }

    /**
     * Supports tagged test registration, for tests that don't take a fixture.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" taggedAs(SlowTest) in { () => ... }
     *                                       ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     */
    def in(testFun: () => Any) {
      registerTestToRun(specText, tags, new NoArgTestWrapper(testFun))
    }

    /**
     * Supports registration of tagged, pending tests.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" taggedAs(SlowTest) is (pending)
     *                                       ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     */
    def is(testFun: => PendingNothing) {
      registerTestToRun(specText, tags, unusedFixtureParam => testFun)
    }

    /**
     * Supports registration of tagged, ignored tests.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" taggedAs(SlowTest) ignore { fixture => ... }
     *                                       ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     */
    def ignore(testFun: FixtureParam => Any) {
      registerTestToIgnore(specText, tags, testFun)
    }

    /**
     * Supports registration of tagged, ignored tests that take no fixture parameter.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" taggedAs(SlowTest) ignore { () => ... }
     *                                       ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     */
    def ignore(testFun: () => Any) {
      registerTestToIgnore(specText, tags, new NoArgTestWrapper(testFun))
    }
  }

  /**
   * A class that via an implicit conversion (named <code>convertToFreeSpecStringWrapper</code>) enables
   * methods <code>when</code>, <code>that</code>, <code>in</code>, <code>is</code>, <code>taggedAs</code>
   * and <code>ignore</code> to be invoked on <code>String</code>s.
   *
   * <p>
   * This class provides much of the syntax for <code>FixtureFreeSpec</code>, however, it does not add
   * the verb methods (<code>should</code>, <code>must</code>, and <code>can</code>) to <code>String</code>.
   * Instead, these are added via the <code>ShouldVerb</code>, <code>MustVerb</code>, and <code>CanVerb</code>
   * traits, which <code>FixtureFreeSpec</code> mixes in, to avoid a conflict with implicit conversions provided
   * in <code>ShouldMatchers</code> and <code>MustMatchers</code>. 
   * </p>
   *
   * @author Bill Venners
   */
  protected final class FreeSpecStringWrapper(string: String) {

    // TODO: Fill in Scaladoc
    def - (fun: => Unit) {
      // registerBranch(string, None, testFun)
      // TODO: Fix the resource name and method name
      registerNestedBranch(string, None, fun, "describeCannotAppearInsideAnIt", "FixtureFreeSpec.scala", "describe")
    }

    /**
     * Supports test registration.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" in { fixture => ... }
     *                    ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     */
    def in(testFun: FixtureParam => Any) {
      registerTestToRun(string, List(), testFun)
    }

    /**
     * Supports registration of tests that take no fixture.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" in { () => ... }
     *                    ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     */
    def in(testFun: () => Any) {
      registerTestToRun(string, List(), new NoArgTestWrapper(testFun))
    }

    /**
     * Supports pending test registration.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" is (pending)
     *                    ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     */
    def is(testFun: => PendingNothing) {
      registerTestToRun(string, List(), unusedFixtureParam => testFun)
    }

    /**
     * Supports ignored test registration.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" ignore { fixture => ... }
     *                    ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     */
    def ignore(testFun: FixtureParam => Any) {
      registerTestToIgnore(string, List(), testFun)
    }

    /**
     * Supports registration of ignored tests that take no fixture.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" ignore { () => ... }
     *                    ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     */
    def ignore(testFun: () => Any) {
      registerTestToIgnore(string, List(), new NoArgTestWrapper(testFun))
    
    }

    /**
     * Supports tagged test registration.
     *
     * <p>
     * For example, this method supports syntax such as the following:
     * </p>
     *
     * <pre class="stHighlight">
     * "complain on peek" taggedAs(SlowTest) in { fixture => ... }
     *                    ^
     * </pre>
     *
     * <p>
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait <code>FreeSpec</code>.
     * </p>
     */
    def taggedAs(firstTestTag: Tag, otherTestTags: Tag*) = {
      val tagList = firstTestTag :: otherTestTags.toList
      new ResultOfTaggedAsInvocationOnString(string, tagList)
    }
  }

  /**
   * Implicitly converts <code>String</code>s to <code>FreeSpecStringWrapper</code>, which enables
   * methods <code>when</code>, <code>that</code>, <code>in</code>, <code>is</code>, <code>taggedAs</code>
   * and <code>ignore</code> to be invoked on <code>String</code>s.
   */
  protected implicit def convertToFreeSpecStringWrapper(s: String) = new FreeSpecStringWrapper(s)

  /**
   * A <code>Map</code> whose keys are <code>String</code> tag names to which tests in this <code>Spec</code> belong, and values
   * the <code>Set</code> of test names that belong to each tag. If this <code>FreeSpec</code> contains no tags, this method returns an empty <code>Map</code>.
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
      theTest.testFun match {
        case wrapper: NoArgTestWrapper[_] =>
          withFixture(new FixturelessTestFunAndConfigMap(testName, wrapper.test, configMap))
        case fun => withFixture(new TestFunAndConfigMap(testName, fun, configMap))
      }
    }

    runTestImpl(thisSuite, testName, reporter, stopper, configMap, tracker, true, invokeWithFixture)
  }

  /**
   * <p>
   * Run zero to many of this <code>Spec</code>'s tests.
   * </p>
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
   * @param testName an optional name of one test to execute. If <code>None</code>, all relevant tests should be executed.
   *                 I.e., <code>None</code> acts like a wildcard that means execute all relevant tests in this <code>Spec</code>.
   * @param reporter the <code>Reporter</code> to which results will be reported
   * @param stopper the <code>Stopper</code> that will be consulted to determine whether to stop execution early.
   * @param tagsToInclude a <code>Set</code> of <code>String</code> tag names to include in the execution of this <code>Spec</code>
   * @param tagsToExclude a <code>Set</code> of <code>String</code> tag names to exclude in the execution of this <code>Spec</code>
   * @param configMap a <code>Map</code> of key-value pairs that can be used by this <code>Spec</code>'s executing tests.
   * @throws NullPointerException if any of <code>testName</code>, <code>reporter</code>, <code>stopper</code>, <code>tagsToInclude</code>,
   *     <code>tagsToExclude</code>, or <code>configMap</code> is <code>null</code>.
   */
  protected override def runTests(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
      configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

    runTestsImpl(thisSuite, testName, reporter, stopper, filter, configMap, distributor, tracker, info, true, runTest)
  }

  /**
   * An immutable <code>Set</code> of test names. If this <code>FixtureFreeSpec</code> contains no tests, this method returns an
   * empty <code>Set</code>.
   *
   * <p>
   * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's
   * iterator will return those names in the order in which the tests were registered. Each test's name is composed
   * of the concatenation of the text of each surrounding describer, in order from outside in, and the text of the
   * example itself, with all components separated by a space.
   * </p>
   */
  override def testNames: Set[String] = {
    // I'm returning a ListSet here so that they tests will be run in registration order
    ListSet(atomic.get.testNamesList.toArray: _*)
  }

  override def run(testName: Option[String], reporter: Reporter, stopper: Stopper, filter: Filter,
      configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker) {

    runImpl(thisSuite, testName, reporter, stopper, filter, configMap, distributor, tracker, super.run)
  }

  /**
   * Supports shared test registration in <code>FixtureFreeSpec</code>s.
   *
   * <p>
   * This field enables syntax such as the following:
   * </p>
   *
   * <pre class="stHighlight">
   * behave like nonFullStack(stackWithOneItem)
   * ^
   * </pre>
   *
   * <p>
   * For more information and examples of the use of <cod>behave</code>, see the <a href="../FreeSpec.html#SharedTests">Shared tests section</a>
   * in the main documentation for trait <code>FreeSpec</code>.
   * </p>
   */
  protected val behave = new BehaveWord
}
