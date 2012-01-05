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
package org.scalatest.fixture

import org.scalatest._
import collection.immutable.TreeSet
import fixture.FixtureSuite._
import java.lang.reflect.{InvocationTargetException, Method, Modifier}
import FixtureSuite.FixtureAndInformerInParens
import FixtureSuite.FixtureInParens
import FixtureSuite.testMethodTakesAFixtureAndInformer
import FixtureSuite.testMethodTakesAFixture
import FixtureSuite.simpleNameForTest
import FixtureSuite.argsArrayForTestName
import org.scalatest.events._
import org.scalatest.Suite._

/**
 * <code>Suite</code> that can pass a fixture object into its tests.
 *
 * <p>
 * The purpose of <code>FixtureSuite</code> and its subtraits is to facilitate writing tests in
 * a functional style. Some users may prefer writing tests in a functional style in general, but one
 * particular use case is parallel test execution (See <a href="../ParallelTestExecution.html">ParallelTestExecution</a>). To run
 * tests in parallel, your test class must
 * be thread safe, and a good way to make it thread safe is to make it functional. A good way to
 * write tests that need common fixtures in a functional style is to pass the fixture objects into the tests,
 * the style enabled by the <code>FixtureSuite</code> family of traits.
 * </p>
 *
 * <p>
 * Trait <code>FixtureSuite</code> behaves similarly to trait <code>org.scalatest.Suite</code>, except that tests may have a
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
 * Subclasses of this trait must, therefore, do three things differently from a plain old <code>org.scalatest.Suite</code>:
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
 * import org.scalatest.fixture.FixtureSuite
 * import collection.mutable.Stack
 * import java.util.NoSuchElementException
 *
 * class StackSuite extends FixtureSuite {
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
 *   // 3. write tests that take a fixture parameter
 *   def testPopAValue(stack: Stack[Int]) {
 *     val top = stack.pop()
 *     assert(top === 2)
 *     assert(stack.size === 1)
 *   }
 *
 *   def testPushAValue(stack: Stack[Int]) {
 *     stack.push(9)
 *     assert(stack.size === 3)
 *     assert(stack.head === 9)
 *   }
 *
 *   // 4. You can also write tests that don't take a fixture parameter.
 *   def testPopAnEmptyStack() {
 *     intercept[NoSuchElementException] {
 *       (new Stack[Int]).pop()
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
 * import org.scalatest.fixture.FixtureSuite
 * import scala.collection.mutable.ListBuffer
 *
 * class ExampleSuite extends FixtureSuite {
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
 *   def testEasy(f: F) {
 *     f.builder.append("easy!")
 *     assert(f.builder.toString === "ScalaTest is easy!")
 *     assert(f.buffer.isEmpty)
 *     f.buffer += "sweet"
 *   }
 *
 *   def testFun(f: F) {
 *     f.builder.append("fun!")
 *     assert(f.builder.toString === "ScalaTest is fun!")
 *     assert(f.buffer.isEmpty)
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
 * import org.scalatest.fixture.FixtureSuite
 * import java.io.FileReader
 * import java.io.FileWriter
 * import java.io.File
 * 
 * class ExampleSuite extends FixtureSuite {
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
 *   def testReadingFromTheTempFile(reader: FileReader) {
 *     var builder = new StringBuilder
 *     var c = reader.read()
 *     while (c != -1) {
 *       builder.append(c.toChar)
 *       c = reader.read()
 *     }
 *     assert(builder.toString === "Hello, test!")
 *   }
 * 
 *   def testFirstCharOfTheTempFile(reader: FileReader) {
 *     assert(reader.read() === 'H')
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
 *  import org.scalatest.fixture.FixtureSuite
 *  import org.scalatest.fixture.ConfigMapFixture
 *
 *  class ExampleSuite extends FixtureSuite with ConfigMapFixture {
 *
 *    def testHello(configMap: Map[String, Any]) {
 *      // Use the configMap passed to runTest in the test
 *      assert(configMap.contains("hello"))
 *    }
 *
 *    def testWorld(configMap: Map[String, Any]) {
 *      assert(configMap.contains("world"))
 *    }
 *  }
 * </pre>
 *
 * <h2>Providing multiple fixtures</h2>
 *
 * <p>
 * If different tests in the same <code>FixtureSuite</code> need different shared fixtures, you can use the <em>loan pattern</em> to supply to
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
 * import org.scalatest.fixture.FixtureSuite
 * import org.scalatest.fixture.ConfigMapFixture
 * import collection.mutable.Stack
 * 
 * class StackSuite extends FixtureSuite with ConfigMapFixture {
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
 *   def testPopAnIntValue() { // This test doesn't need the configMap fixture, ...
 *     withIntStack { stack =>
 *       val top = stack.pop() // But it needs the Stack[Int] fixture.
 *       assert(top === 2)
 *       assert(stack.size === 1)
 *     }
 *   }
 * 
 *   def testPushAnIntValue(configMap: Map[String, Any]) {
 *     withIntStack { stack =>
 *       val iToPush = // This test uses the configMap fixture...
 *         configMap("IntToPush").toString.toInt
 *       stack.push(iToPush) // And also uses the Stack[Int] fixture.
 *       assert(stack.size === 3)
 *       assert(stack.head === iToPush)
 *     }
 *   }
 * 
 *   def testPopAStringValue() { // This test doesn't need the configMap fixture, ...
 *     withStringStack { stack =>
 *       val top = stack.pop() // But it needs the Stack[String] fixture.
 *       assert(top === "two")
 *       assert(stack.size === 1)
 *     }
 *   }
 * 
 *   def testPushAStringValue(configMap: Map[String, Any]) {
 *     withStringStack { stack =>
 *       val sToPush = // This test uses the configMap fixture...
 *         configMap("StringToPush").toString
 *       stack.push(sToPush) // And also uses the Stack[Int] fixture.
 *       assert(stack.size === 3)
 *       assert(stack.head === sToPush)
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
 * scala> run(new StackSuite, configMap = Map("IntToPush" -> 9, "StringToPush" -> "nine"))
 * <span class="stGreen">StackSuite:
 * - testPopAStringValue
 * - testPopAnIntValue
 * - testPushAStringValue(FixtureParam)
 * - testPushAnIntValue(FixtureParam)</span>
 * </pre>
 *
 * @author Bill Venners
 */
trait FixtureSuite extends org.scalatest.Suite { thisSuite =>

  /**
   * The type of the fixture parameter that can be passed into tests in this suite.
   */
  protected type FixtureParam

  /**
   * Trait whose instances encapsulate a test function that takes a fixture and config map.
   *
   * <p>
   * The <code>FixtureSuite</code> trait's implementation of <code>runTest</code> passes instances of this trait
   * to <code>FixtureSuite</code>'s <code>withFixture</code> method, such as:
   * </p>
   *
   * <pre class="stHighlight">
   * def testSomething(fixture: Fixture) {
   *   // ...
   * }
   * def testSomethingElse(fixture: Fixture, info: Informer) {
   *   // ...
   * }
   * </pre>
   *
   * <p>
   * For more detail and examples, see the
   * <a href="FixtureSuite.html">documentation for trait <code>FixtureSuite</code></a>.
   * </p>
   */
  protected trait OneArgTest extends (FixtureParam => Unit) { thisOneArgTest =>

    /**
     * The name of this test.
     */
    def name: String

    /**
     * Run the test, using the passed <code>FixtureParam</code>.
     */
    def apply(fixture: FixtureParam)

    /**
     * Return a <code>Map[String, Any]</code> containing objects that can be used
     * to configure the fixture and test.
     */
    def configMap: Map[String, Any]

    /**
     * Convert this <code>OneArgTest</code> to a <code>NoArgTest</code> whose
     * <code>name</code> and <code>configMap</code> methods return the same values
     * as this <code>OneArgTest</code>, and whose <code>apply</code> method invokes
     * this <code>OneArgTest</code>'s apply method,
     * passing in the given <code>fixture</code>.
     *
     * <p>
     * This method makes it easier to invoke the <code>withFixture</code> method
     * that takes a <code>NoArgTest</code>. For example, if a <code>FixtureSuite</code> 
     * mixes in <code>SeveredStackTraces</code>, it will inherit an implementation
     * of <code>withFixture(NoArgTest)</code> provided by
     * <code>SeveredStackTraces</code> that implements the stack trace severing
     * behavior. If the <code>FixtureSuite</code> does not delegate to that
     * <code>withFixture(NoArgTest)</code> method, the stack trace severing behavior
     * will not happen. Here's how that might look in a <code>FixtureSuite</code>
     * whose <code>FixtureParam</code> is <code>StringBuilder</code>:
     * </p>
     *
     * <pre class="stHighlight">
     * def withFixture(test: OneArgTest) {
     *   withFixture(test.toNoArgTest(new StringBuilder))
     * }
     * </pre>
     */
    def toNoArgTest(fixture: FixtureParam) =
      new NoArgTest {
        val name = thisOneArgTest.name
        def configMap = thisOneArgTest.configMap
        def apply() { thisOneArgTest(fixture) }
      }
  }

  /**
   *  Run the passed test function with a fixture created by this method.
   *
   * <p>
   * This method should create the fixture object needed by the tests of the
   * current suite, invoke the test function (passing in the fixture object),
   * and if needed, perform any clean up needed after the test completes.
   * For more detail and examples, see the <a href="FixtureSuite.html">main documentation for this trait</a>.
   * </p>
   *
   * @param fun the <code>OneArgTest</code> to invoke, passing in a fixture
   */
  protected def withFixture(test: OneArgTest)

  private[fixture] class TestFunAndConfigMap(val name: String, test: FixtureParam => Any, val configMap: Map[String, Any])
    extends OneArgTest {
    
    def apply(fixture: FixtureParam) {
      test(fixture)
    }
  }

  private[fixture] class FixturelessTestFunAndConfigMap(override val name: String, test: () => Any, override val configMap: Map[String, Any])
    extends NoArgTest {

    def apply() { test() }
  }

  override def testNames: Set[String] = {

    def takesTwoParamsOfTypesAnyAndInformer(m: Method) = {
      val paramTypes = m.getParameterTypes
      val hasTwoParams = paramTypes.length == 2
      hasTwoParams && classOf[Informer].isAssignableFrom(paramTypes(1))
    }

    def takesOneParamOfAnyType(m: Method) = m.getParameterTypes.length == 1

    def isTestMethod(m: Method) = {

      // Factored out to share code with Suite.testNames
      val (isInstanceMethod, simpleName, firstFour, paramTypes, hasNoParams, isTestNames) = isTestMethodGoodies(m)

      // Also, will discover both
      // testNames(Object) and testNames(Object, Informer). Reason is if I didn't discover these
      // it would likely just be silently ignored, and that might waste users' time
      isInstanceMethod && (firstFour == "test") && ((hasNoParams && !isTestNames) ||
          takesInformer(m) || takesOneParamOfAnyType(m) || takesTwoParamsOfTypesAnyAndInformer(m))
    }

    val testNameArray =
      for (m <- getClass.getMethods; if isTestMethod(m)) yield
        if (takesInformer(m))
          m.getName + InformerInParens
        else if (takesOneParamOfAnyType(m))
          m.getName + FixtureInParens
        else if (takesTwoParamsOfTypesAnyAndInformer(m))
          m.getName + FixtureAndInformerInParens
        else m.getName

    TreeSet[String]() ++ testNameArray
  }

  protected override def runTest(testName: String, reporter: Reporter, stopper: Stopper, configMap: Map[String, Any], tracker: Tracker) {

    checkRunTestParamsForNull(testName, reporter, stopper, configMap, tracker)

    val (stopRequested, report, method, hasPublicNoArgConstructor, rerunnable, testStartTime) =
      getSuiteRunTestGoodies(stopper, reporter, testName)

    reportTestStarting(thisSuite, report, tracker, testName, rerunnable)

    val formatter = getIndentedText(testName, 1, true)

    try {
      if (testMethodTakesAFixtureAndInformer(testName) || testMethodTakesAFixture(testName)) {
        val testFun: FixtureParam => Unit = {
          (fixture: FixtureParam) => {
            val anyRefFixture: AnyRef = fixture.asInstanceOf[AnyRef] // TODO zap this cast
            val args: Array[Object] =
              if (testMethodTakesAFixtureAndInformer(testName)) {
                val informer =
                  new Informer {
                    def apply(message: String) {
                      if (message == null)
                        throw new NullPointerException
                      reportInfoProvided(thisSuite, report, tracker, Some(testName), message, 2, true)
                    }
                  }
                Array(anyRefFixture, informer)
              }
              else
                Array(anyRefFixture)

            method.invoke(thisSuite, args: _*)
          }
        }
        withFixture(new TestFunAndConfigMap(testName, testFun, configMap))
      }
      else { // Test method does not take a fixture
        val testFun: () => Unit = {
          () => {
            val args: Array[Object] =
              if (testMethodTakesAnInformer(testName)) {
                val informer =
                  new Informer {
                    def apply(message: String) {
                      if (message == null)
                        throw new NullPointerException
                      reportInfoProvided(thisSuite, report, tracker, Some(testName), message, 2, true)
                    }
                  }
                Array(informer)
              }
              else
                Array()

            method.invoke(this, args: _*)
          }
        }
        withFixture(new FixturelessTestFunAndConfigMap(testName, testFun, configMap))
      }

      val duration = System.currentTimeMillis - testStartTime
      reportTestSucceeded(thisSuite, report, tracker, testName, duration, formatter, rerunnable)
    }
    catch { 
      case ite: InvocationTargetException =>
        val t = ite.getTargetException
        t match {
          case _: TestPendingException =>
            reportTestPending(thisSuite, report, tracker, testName, formatter)
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
  }

  // Overriding this in FixtureSuite to reduce duplication of tags method
  private[scalatest] override def getMethodForTestName(testName: String) = {
    val candidateMethods = getClass.getMethods.filter(_.getName == simpleNameForTest(testName))
    val found =
      if (testMethodTakesAFixtureAndInformer(testName))
        candidateMethods.find(
          candidateMethod => {
            val paramTypes = candidateMethod.getParameterTypes
            paramTypes.length == 2 && paramTypes(1) == classOf[Informer]
          }
        )
      else if (testMethodTakesAnInformer(testName))
        candidateMethods.find(
          candidateMethod => {
            val paramTypes = candidateMethod.getParameterTypes
            paramTypes.length == 1 && paramTypes(0) == classOf[Informer]
          }
        )
      else if (testMethodTakesAFixture(testName))
        candidateMethods.find(
          candidateMethod => {
            val paramTypes = candidateMethod.getParameterTypes
            paramTypes.length == 1
          }
        )
      else
        candidateMethods.find(_.getParameterTypes.length == 0)

     found match {
       case Some(method) => method
       case None =>
         throw new IllegalArgumentException(Resources("testNotFound", testName))
     }
  }
}

private object FixtureSuite {

  val FixtureAndInformerInParens = "(FixtureParam, Informer)"
  val FixtureInParens = "(FixtureParam)"

  private def testMethodTakesAFixtureAndInformer(testName: String) = testName.endsWith(FixtureAndInformerInParens)
  private def testMethodTakesAFixture(testName: String) = testName.endsWith(FixtureInParens)

  private def simpleNameForTest(testName: String) =
    if (testName.endsWith(FixtureAndInformerInParens))
      testName.substring(0, testName.length - FixtureAndInformerInParens.length)
    else if (testName.endsWith(FixtureInParens))
      testName.substring(0, testName.length - FixtureInParens.length)
    else if (testName.endsWith(InformerInParens))
      testName.substring(0, testName.length - InformerInParens.length)
    else
      testName

  private def argsArrayForTestName(testName: String): Array[Class[_]] =
    if (testMethodTakesAFixtureAndInformer(testName))
      Array(classOf[Object], classOf[Informer])
    else
      Array(classOf[Informer])
}
