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

/**
 * A suite of property-based tests.
 *
 * <p>
 * This trait facilitates a style of testing in which each test is composed
 * of one property check. Tests are registered via a "<code>property</code>" method, and given a name and a body.
 * (A <code>PropSpec</code> behaves just like a <code>FunSuite</code>, except <code>test</code> is replaced with
 * <code>property</code>.) You can do anything in the body of the test, but the intention is that you'd check
 * one property in each test. To write properties in the ScalaCheck style, mix <code>Checkers</code> into
 * your <code>PropSpec</code>. To write them in the ScalaTest style, mix in <code>PropertyChecks</code>.
 * </p>
 *
 * <p>
 * For example, given this <code>Fraction</code> class:
 * </p>
 *
 * <pre class="stHighlight">
 * class Fraction(n: Int, d: Int) {
 *   require(d != 0)
 *   require(d != Integer.MIN_VALUE)
 *   require(n != Integer.MIN_VALUE)
 *
 *   val numer = if (d < 0) -1 * n else n
 *   val denom = d.abs
 *
 *   override def toString = numer + " / " + denom
 * }
 * </pre>
 *
 * <p>
 * You could write a <code>PropSpec</code> in the ScalaTest property style that specifies the <code>Fraction</code> behavior like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.PropSpec
 * import org.scalatest.prop.PropertyChecks
 * import org.scalatest.matchers.ShouldMatchers
 *
 * class FractionSpec extends PropSpec with PropertyChecks with ShouldMatchers {
 *
 *   property("Fraction constructor normalizes numerator and denominator") {
 *
 *     forAll { (n: Int, d: Int) =>
 *       whenever (d != 0 && d != Integer.MIN_VALUE
 *           && n != Integer.MIN_VALUE) {
 *
 *         val f = new Fraction(n, d)
 *
 *         if (n < 0 && d < 0 || n > 0 && d > 0)
 *           f.numer should be > 0
 *         else if (n != 0)
 *           f.numer should be < 0
 *         else
 *           f.numer should be === 0
 *
 *         f.denom should be > 0
 *       }
 *     }
 *   }
 *
 *   property("Fraction constructor throws IAE on bad data.") {
 *
 *     val invalidCombos =
 *       Table(
 *         ("n",               "d"),
 *         (Integer.MIN_VALUE, Integer.MIN_VALUE),
 *         (1,                 Integer.MIN_VALUE),
 *         (Integer.MIN_VALUE, 1),
 *         (Integer.MIN_VALUE, 0),
 *         (1,                 0)
 *       )
 *
 *     forAll (invalidCombos) { (n: Int, d: Int) =>
 *       evaluating {
 *         new Fraction(n, d)
 *       } should produce [IllegalArgumentException]
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * &#8220;<code>property</code>&#8221; is a method, defined in <code>PropSpec</code>, which will be invoked
 * by the primary constructor of <code>MathSpec</code>. You specify the name of the property as
 * a string between the parentheses, and the test code containing the property check between curly braces.
 * The test code is a function passed as a by-name parameter to <code>property</code>, which registers
 * it for later execution.
 * </p>
 *
 * <p>
 * A <code>PropSpec</code>'s lifecycle has two phases: the <em>registration</em> phase and the
 * <em>ready</em> phase. It starts in registration phase and enters ready phase the first time
 * <code>run</code> is called on it. It then remains in ready phase for the remainder of its lifetime.
 * </p>
 *
 * <p>
 * Properties can only be registered with the <code>property</code> method while the <code>PropSpec</code> is
 * in its registration phase. Any attempt to register a property after the <code>PropSpec</code> has
 * entered its ready phase, <em>i.e.</em>, after <code>run</code> has been invoked on the <code>PropSpec</code>,
 * will be met with a thrown <code>TestRegistrationClosedException</code>. The recommended style
 * of using <code>PropSpec</code> is to register properties during object construction as is done in all
 * the examples shown here. If you keep to the recommended style, you should never see a
 * <code>TestRegistrationClosedException</code>.
 * </p>
 *
 * <p>
 * <em>Note: Trait <code>PropSpec</code> is in part inspired by class <code>org.scalacheck.Properties</code>, designed by
 * Rickard Nilsson for the <a href="http://code.google.com/p/scalacheck/">ScalaCheck test framework</a>.</em>
 * </p>
 *
 * <h2>Ignored tests</h2>
 *
 * <p>
 * To support the common use case of &#8220;temporarily&#8221; disabling a test, with the
 * good intention of resurrecting the test at a later time, <code>PropSpec</code> provides registration
 * methods that start with <code>ignore</code> instead of <code>property</code>. For example, to temporarily
 * disable the test named <code>addition</code>, just change &#8220;<code>property</code>&#8221; into &#8220;<code>ignore</code>,&#8221; like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.PropSpec
 * import org.scalatest.prop.PropertyChecks
 * import org.scalatest.matchers.ShouldMatchers
 *
 * class MathSpec extends PropSpec with PropertyChecks with ShouldMatchers {
 *
 *   ignore("addition", SlowTest) {
 *     forAll { (i: Int) => i + i should equal (2 * i) }
 *   }
 *
 *   property("subtraction", SlowTest, DbTest) {
 *     forAll { (i: Int) => i - i should equal (0) }
 *   }
 * }
 * </pre>
 *
 * <p>
 * If you run this version of <code>MathSpec</code> with:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new MathSpec).execute()
 * </pre>
 *
 * <p>
 * It will run only <code>subtraction</code> and report that <code>addition</code> was ignored:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">MathSpec:</span>
 * <span class="stYellow">- addition !!! IGNORED !!!</span>
 * <span class="stGreen">- subtraction</span>
 * </pre>
 *
 * <h2>Informers</h2>
 *
 * <p>
 * One of the parameters to the <code>run</code> method is a <code>Reporter</code>, which
 * will collect and report information about the running suite of tests.
 * Information about suites and tests that were run, whether tests succeeded or failed, 
 * and tests that were ignored will be passed to the <code>Reporter</code> as the suite runs.
 * Most often the reporting done by default by <code>PropSpec</code>'s methods will be sufficient, but
 * occasionally you may wish to provide custom information to the <code>Reporter</code> from a test.
 * For this purpose, an <code>Informer</code> that will forward information to the current <code>Reporter</code>
 * is provided via the <code>info</code> parameterless method.
 * You can pass the extra information to the <code>Informer</code> via one of its <code>apply</code> methods.
 * The <code>Informer</code> will then pass the information to the <code>Reporter</code> via an <code>InfoProvided</code> event.
 * Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.PropSpec
 * import org.scalatest.prop.PropertyChecks
 * import org.scalatest.matchers.ShouldMatchers
 *
 * class MathSpec extends PropSpec with PropertyChecks with ShouldMatchers {
 *
 *   property("addition", SlowTest) {
 *     forAll { (i: Int) => i + i should equal (2 * i) }
 *     info("Addition seems to work")
 *   }
 *
 *   property("subtraction", SlowTest, DbTest) {
 *     forAll { (i: Int) => i - i should equal (0) }
 *   }
 * }
 * </pre>
 *
 * If you run this <code>PropSpec</code> from the interpreter, you will see the following message
 * included in the printed report:
 *
 * <pre class="stREPL">
 * <span class="stGreen">MathSpec:
 * - addition
 *   + Addition seems to work</span> 
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
 * Although pending tests may be used more often in specification-style suites, such as
 * <code>org.scalatest.Spec</code>, you can also use it in <code>PropSpec</code>, like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.PropSpec
 * import org.scalatest.prop.PropertyChecks
 * import org.scalatest.matchers.ShouldMatchers
 *
 * class MathSpec extends PropSpec with PropertyChecks with ShouldMatchers {
 *
 *   ignore("addition") {
 *     forAll { (i: Int) => i + i should equal (2 * i) }
 *   }
 *
 *   property("subtraction") (pending)
 * }
 * </pre>
 *
 * <p>
 * (Note: "<code>(pending)</code>" is the body of the test. Thus the test contains just one statement, an invocation
 * of the <code>pending</code> method, which throws <code>TestPendingException</code>.)
 * If you run this version of <code>MathSpec</code> with:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new MathSpec).execute()
 * </pre>
 *
 * <p>
 * It will run both tests, but report that <code>subtraction</code> is pending. You'll see:
 * </p>
 *
 * <pre class="stREPL">
 * <span class="stGreen">MathSpec:
 * - addition</span>
 * <span class="stYellow">- subtraction (pending)</span>
 * </pre>
 *
 * <h2>Tagging tests</h2>
 *
 * <p>
 * A <code>PropSpec</code>'s tests may be classified into groups by <em>tagging</em> them with string names.
 * As with any suite, when executing a <code>PropSpec</code>, groups of tests can
 * optionally be included and/or excluded. To tag a <code>PropSpec</code>'s tests,
 * you pass objects that extend abstract class <code>org.scalatest.Tag</code> to methods
 * that register tests, <code>test</code> and <code>ignore</code>. Class <code>Tag</code> takes one parameter, a string name.  If you have
 * created Java annotation interfaces for use as group names in direct subclasses of <code>org.scalatest.Suite</code>,
 * then you will probably want to use group names on your <code>PropSpec</code>s that match. To do so, simply 
 * pass the fully qualified names of the Java interfaces to the <code>Tag</code> constructor. For example, if you've
 * defined Java annotation interfaces with fully qualified names, <code>com.mycompany.tags.SlowTest</code> and
 * <code>com.mycompany.tags.DbTest</code>, then you could
 * create matching groups for <code>PropSpec</code>s like this:
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
 * Given these definitions, you could tag a <code>PropSpec</code>'s tests like this:
 * </p>
 *
 * <pre class="stHighlight">
 * import org.scalatest.PropSpec
 * import org.scalatest.prop.PropertyChecks
 * import org.scalatest.matchers.ShouldMatchers
 *
 * class MathSpec extends PropSpec with PropertyChecks with ShouldMatchers {
 *
 *   property("addition", SlowTest) {
 *     forAll { (i: Int) => i + i should equal (2 * i) }
 *   }
 *
 *   property("subtraction", SlowTest, DbTest) {
 *     forAll { (i: Int) => i - i should equal (0) }
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
 * connections, <em>etc.</em>) used by tests to do their work. You can use fixtures in
 * <code>PropSpec</code>s with the same approaches suggested for <code>FunSuite</code> in
 * its documentation. For more information, see the <a href="FunSuite.html#SharedFixtures">Shared fixtures</a> section of <code>FunSuite</code>'s
 * documentation (and substitute <code>property</code> for <code>test</code>).
 * </p>
 *
 * <a name="SharedTests"></a><h2>Shared tests</h2>
 *
 * <p>
 * Sometimes you may want to run the same test code on different fixture objects. In other words, you may want to write tests that are "shared"
 * by different fixture objects.
 * You accomplish this in a <code>PropSpec</code> in the same way you would do it in a <code>FunSuite</code>, exception instead of <code>test</code>
 * you say <code>property</code>, and instead of <code>testsFor</code> you say <code>propertiesFor</code>. 
 * For more information, see the <a href="FunSuite.html#SharedTests">Shared tests</a> section of <code>FunSuite</code>'s
 * documentation.
 * </p>
 *
 * @author Bill Venners
 */
trait PropSpec extends Suite { thisSuite =>

  private final val engine = new Engine("concurrentPropSpecMod", "PropSpec")
  import engine._

  /**
   * Returns an <code>Informer</code> that during test execution will forward strings (and other objects) passed to its
   * <code>apply</code> method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * <code>PropSpec</code> is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * throw an exception. This method can be called safely by any thread.
   */
  implicit protected def info: Informer = atomicInformer.get

  /**
   * Register a property-based test with the specified name, optional tags, and function value that takes no arguments.
   * This method will register the test for later execution via an invocation of one of the <code>run</code>
   * methods. The passed test name must not have been registered previously on
   * this <code>PropSpec</code> instance.
   *
   * @param testName the name of the property
   * @param testTags the optional list of tags for this property
   * @param testFun the property function
   * @throws TestRegistrationClosedException if invoked after <code>run</code> has been invoked on this suite
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws NotAllowedException if <code>testName</code> had been registered previously
   * @throws NullPointerException if <code>testName</code> or any passed test tag is <code>null</code>
   */
  protected def property(testName: String, testTags: Tag*)(testFun: => Unit) {
    registerTest(testName, testFun _, "propertyCannotAppearInsideAnotherProperty", "PropSpec.scala", "property", testTags: _*)
  }

  /**
   * Register a property-based test to ignore, which has the specified name, optional tags, and function value that takes no arguments.
   * This method will register the test for later ignoring via an invocation of one of the <code>run</code>
   * methods. This method exists to make it easy to ignore an existing test by changing the call to <code>test</code>
   * to <code>ignore</code> without deleting or commenting out the actual test code. The test will not be run, but a
   * report will be sent that indicates the test was ignored. The passed test name must not have been registered previously on
   * this <code>PropSpec</code> instance.
   *
   * @param testName the name of the test
   * @param testTags the optional list of tags for this test
   * @param testFun the test function
   * @throws TestRegistrationClosedException if invoked after <code>run</code> has been invoked on this suite
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws NotAllowedException if <code>testName</code> had been registered previously
   */
  protected def ignore(testName: String, testTags: Tag*)(testFun: => Unit) {
    registerIgnoredTest(testName, testFun _, "ignoreCannotAppearInsideAProperty", "PropSpec.scala", "ignore", testTags: _*)
  }

  /**
  * An immutable <code>Set</code> of test names. If this <code>PropSpec</code> contains no tests, this method returns an empty <code>Set</code>.
  *
  * <p>
  * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's iterator will
  * return those names in the order in which the tests were registered.
  * </p>
  */
  override def testNames: Set[String] = {
    // I'm returning a ListSet here so that they tests will be run in registration order
    ListSet(atomic.get.testNamesList.toArray: _*)
  }

  /**
   * Run a test. This trait's implementation runs the test registered with the name specified by <code>testName</code>.
   *
   * @param testName the name of one test to run.
   * @param reporter the <code>Reporter</code> to which results will be reported
   * @param stopper the <code>Stopper</code> that will be consulted to determine whether to stop execution early.
   * @param configMap a <code>Map</code> of properties that can be used by the executing <code>Suite</code> of tests.
   * @throws IllegalArgumentException if <code>testName</code> is defined but a test with that name does not exist on this <code>PropSpec</code>
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
   * A <code>Map</code> whose keys are <code>String</code> tag names to which tests in this <code>PropSpec</code> belong, and values
   * the <code>Set</code> of test names that belong to each tag. If this <code>PropSpec</code> contains no tags, this method returns an empty <code>Map</code>.
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
   * Registers shared tests.
   *
   * <p>
   * This method enables the following syntax for shared tests in a <code>PropSpec</code>:
   * </p>
   *
   * <pre class="stHighlight">
   * propertiesFor(nonEmptyStack(lastValuePushed))
   * </pre>
   *
   * <p>
   * This method just provides syntax sugar intended to make the intent of the code clearer.
   * Because the parameter passed to it is
   * type <code>Unit</code>, the expression will be evaluated before being passed, which
   * is sufficient to register the shared tests. For examples of shared tests, see the
   * <a href="#SharedTests">Shared tests section</a> in the main documentation for this trait.
   * </p>
   */
  protected def propertiesFor(unit: Unit) {}
}
