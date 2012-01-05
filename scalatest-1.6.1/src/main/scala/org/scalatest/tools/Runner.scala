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
import scala.collection.mutable.ListBuffer
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import java.net.URL
import java.net.MalformedURLException
import java.net.URLClassLoader
import java.io.File
import java.io.IOException
import javax.swing.SwingUtilities
import java.util.concurrent.ArrayBlockingQueue
import java.util.regex.Pattern
import org.scalatest.testng.TestNGWrapperSuite
import java.util.concurrent.Semaphore
import org.scalatest.events._
import org.scalatest.junit.JUnitWrapperSuite
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

/**
 * <p>
 * Application that runs a suite of tests.
 * The application accepts command line arguments that specify optional <em>config map</em> (key-value pairs), an optional 
 * <em>runpath</em>, zero to many <code>Reporter</code>s, optional lists of tags to include and/or exclude, zero to many
 * <code>Suite</code> class names, zero to many "members-only" <code>Suite</code> paths, zero to many "wildcard" <code>Suite</code> paths,
 * and zero to many TestNG XML config file paths.
 * All of these arguments are described in more detail below. Here's a summary:
 * </p>
 *
 * <pre class="stExamples">
 * scala [-classpath scalatest-&lt;version&gt;.jar:...] org.scalatest.tools.Runner 
[-D&lt;key&gt;=&lt;value&gt; [...]] [-p &lt;runpath&gt;] [reporter [...]] 
[-n &lt;includes&gt;] [-l &lt;excludes&gt;] [-c] [-s &lt;suite class name&gt; 
[...]] [-j &lt;junit class name&gt; [...]] [-m &lt;members-only suite path&gt; 
[...]] [-w &lt;wildcard suite path&gt; [...]] [-t &lt;TestNG config file 
path&gt; [...]]
 * </pre>
 *
 * <p>
 * The simplest way to start <code>Runner</code> is to specify the directory containing your compiled tests as the sole element of the runpath, for example:
 * </p>
 *
 * <pre class="stExamples">scala -classpath scalatest-&lt;version&gt;.jar org.scalatest.tools.Runner -p compiled_tests</pre>
 *
 * <p>
 * Given the previous command, <code>Runner</code> will discover and execute all <code>Suite</code>s in the <code>compiled_tests</code> directory and its subdirectories,
 * and show results in graphical user interface (GUI).
 * </p>
 *
 * <a name="configMapSection"></a>
 * <h2>Specifying the config map</h2>
 *
 * <p>
 * A <em>config map</em> contains pairs consisting of a string key and a value that may be of any type. (Keys that start with
 * &quot;org.scalatest.&quot; are reserved for ScalaTest. Configuration values that are themselves strings may be specified on the
 * <code>Runner</code> command line.
 * Each configuration pair is denoted with a "-D", followed immediately by the key string, an &quot;=&quot;, and the value string.
 * For example:
 * </p>
 *
 * <pre class="stExamples">-Ddbname=testdb -Dserver=192.168.1.188</pre>
 *
 * <h2>Specifying a runpath</h2>
 *
 * <p>
 * A runpath is the list of filenames, directory paths, and/or URLs that <code>Runner</code>
 * uses to load classes for the running test. If runpath is specified, <code>Runner</code> creates
 * a custom class loader to load classes available on the runpath.
 * The graphical user interface reloads the test classes anew for each run
 * by creating and using a new instance of the custom class loader for each run.
 * The classes that comprise the test may also be made available on
 * the classpath, in which case no runpath need be specified.
 * </p>
 *
 * <p>
 * The runpath is specified with the <b>-p</b> option. The <b>-p</b> must be followed by a space,
 * a double quote (<code>"</code>), a white-space-separated list of
 * paths and URLs, and a double quote. If specifying only one element in the runpath, you can leave off
 * the double quotes, which only serve to combine a white-space separated list of strings into one
 * command line argument. If you have path elements that themselves have a space in them, you must
 * place a backslash (\) in front of the space. Here's an example:
 * </p>
 *
 * <pre class="stExamples">-p "serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar target/class\ files"</pre>
 *
 * <h2>Specifying reporters</h2>
 *
 * <p>
 * Reporters can be specified  on the command line in any of the following
 * ways:
 * </p>
 *
 * <ul>
 * <li> <code><b>-g[configs...]</b></code> - causes display of a graphical user interface that allows
 *    tests to be run and results to be investigated</li>
 * <li> <code><b>-f[configs...] &lt;filename&gt;</b></code> - causes test results to be written to
 *     the named file</li>
 * <li> <code><b>-u &lt;directory&gt;</b></code> - causes test results to be written to
 *      xml files in the named directory</li>
 * <li> <code><b>-o[configs...]</b></code> - causes test results to be written to
 *     the standard output</li>
 * <li> <code><b>-e[configs...]</b></code> - causes test results to be written to
 *     the standard error</li>
 * <li> <code><b>-r[configs...] &lt;reporterclass&gt;</b></code> - causes test results to be reported to
 *     an instance of the specified fully qualified <code>Reporter</code> class name</li>
 * </ul>
 *
 * <p>
 * The <code><b>[configs...]</b></code> parameter, which is used to configure reporters, is described in the next section.
 * </p>
 *
 * <p>
 * The <code><b>-r</b></code> option causes the reporter specified in
 * <code><b>&lt;reporterclass&gt;</b></code> to be
 * instantiated.
 * Each reporter class specified with a <b>-r</b> option must be public, implement
 * <code>org.scalatest.Reporter</code>, and have a public no-arg constructor.
 * Reporter classes must be specified with fully qualified names. 
 * The specified reporter classes may be
 * deployed on the classpath. If a runpath is specified with the
 * <code>-p</code> option, the specified reporter classes may also be loaded from the runpath.
 * All specified reporter classes will be loaded and instantiated via their no-arg constructor.
 * </p>
 *
 * <p>
 * For example, to run a suite named <code>MySuite</code> from the <code>mydir</code> directory
 * using two reporters, the graphical reporter and a file reporter
 * writing to a file named <code>"test.out"</code>, you would type:
 * </p>
 *
 * <pre class="stExamples">java -jar scalatest.jar -p mydir <b>-g -f test.out</b> -s MySuite</pre>
 *
 * <p>
 * The <code><b>-g</b></code>, <code><b>-o</b></code>, or <code><b>-e</b></code> options can
 * appear at most once each in any single command line.
 * Multiple appearances of <code><b>-f</b></code> and <code><b>-r</b></code> result in multiple reporters
 * unless the specified <code><b>&lt;filename&gt;</b></code> or <code><b>&lt;reporterclass&gt;</b></code> is
 * repeated. If any of <code><b>-g</b></code>, <code><b>-o</b></code>, <code><b>-e</b></code>,
 * <code><b>&lt;filename&gt;</b></code> or <code><b>&lt;reporterclass&gt;</b></code> are repeated on
 * the command line, the <code>Runner</code> will print an error message and not run the tests.
 * </p>
 *
 * <p>
 * <code>Runner</code> adds the reporters specified on the command line to a <em>dispatch reporter</em>,
 * which will dispatch each method invocation to each contained reporter. <code>Runner</code> will pass
 * the dispatch reporter to executed suites. As a result, every
 * specified reporter will receive every report generated by the running suite of tests.
 * If no reporters are specified, a graphical
 * runner will be displayed that provides a graphical report of
 * executed suites.
 * </p>
 *
 * <h2>Configuring Reporters</h2>
 *
 * <p>
 * Each reporter option on the command line can include configuration characters. Configuration characters
 * are specified immediately following the <code><b>-g</b></code>, <code><b>-o</b></code>,
 * <code><b>-e</b></code>, <code><b>-f</b></code>, or <code><b>-r</b></code>. The following configuration
 * characters, which cause reports to be dropped, are valid for any reporter:
 * </p>
 *
 * <ul>
 * <li> <code><b>N</b></code> - drop <code>TestStarting</code> events</li>
 * <li> <code><b>C</b></code> - drop <code>TestSucceeded</code> events</li>
 * <li> <code><b>X</b></code> - drop <code>TestIgnored</code> events</li>
 * <li> <code><b>E</b></code> - drop <code>TestPending</code> events</li>
 * <li> <code><b>H</b></code> - drop <code>SuiteStarting</code> events</li>
 * <li> <code><b>L</b></code> - drop <code>SuiteCompleted</code> events</li>
 * <li> <code><b>O</b></code> - drop <code>InfoProvided</code> events</li>
 * </ul>
 *
 * <p>
 * A dropped event will not be delivered to the reporter at all. So the reporter will not know about it and therefore not
 * present information about the event in its report. For example, if you specify <code>-oN</code>, the standard output reporter
 * will never receive any <code>TestStarting</code> events and will therefore never report them. The purpose of these
 * configuration parameters is to allow users to selectively remove events they find add clutter to the report without
 * providing essential information.
 * </p>
 *
 * <p>
 * The following three reporter configuration parameters may additionally be used on standard output (-o), standard error (-e),
 * and file (-f) reporters: 
 * </p>
 *
 * <ul>
 * <li> <code><b>W</b></code> - without color</li>
 * <li> <code><b>D</b></code> - show all durations</li>
 * <li> <code><b>S</b></code> - show short stack traces</li>
 * <li> <code><b>F</b></code> - show full stack traces</li>
 * </ul>
 *
 * <p>
 * If you specify a W, D, S, or F for any reporter other than standard output, standard error, or file reporters, <code>Runner</code>
 * will complain with an error message and not perform the run.
 * </p>
 *
 * <p>
 * Configuring a standard output, error, or file reporter with <code>D</code> will cause that reporter to
 * print a duration for each test and suite.  When running in the default mode, a duration will only be printed for
 * the entire run.
 * </p>
 *
 * <p>
 * Configuring a standard output, error, or file reporter with <code>F</code> will cause that reporter to print full stack traces for all exceptions,
 * including <code>TestFailedExceptions</code>. Every <code>TestFailedException</code> contains a stack depth of the
 * line of test code that failed so that users won't need to search through a stack trace to find it. When running in the default,
 * mode, these reporters will only show full stack traces when other exceptions are thrown, such as an exception thrown
 * by production code. When a <code>TestFailedException</code> is thrown in default mode, only the source filename and
 * line number of the line of test code that caused the test to fail are printed along with the error message, not the full stack
 * trace. 
 * </p>
 *
 * <p>
 * By default, a standard output, error, or file reporter inserts ansi escape codes into the output printed to change and later reset
 * terminal colors. Information printed as a result of run starting, completed, and stopped events
 * is printed in cyan. Information printed as a result of ignored or pending test events is shown in yellow. Information printed
 * as a result of test failed, suite aborted, or run aborted events is printed in red. All other information is printed in green.
 * The purpose of these colors is to facilitate speedy reading of the output, especially the finding of failed tests, which can
 * get lost in a sea of passing tests. Configuring a standard output, error, or file reporter into without-color mode ('W') will
 * turn off this behavior. No ansi codes will be inserted.
 * </p>
 *
 * <p>
 * For example, to run a suite using two reporters, the graphical reporter configured to present every reported event
 * and a standard error reporter configured to present everything but test starting, test succeeded, test ignored, test
 * pending, suite starting, suite completed, and info provided events, you would type:
 * </p>
 *
 * <p>
 * <code>scala -classpath scalatest-&lt;version&gt;.jar -p mydir <strong>-g -eNDXEHLO</strong> -s MySuite</code>
 * </p>
 *
 * <p>
 * Note that no white space is allowed between the reporter option and the initial configuration
 * parameters. So <code>"-e NDXEHLO"</code> will not work,
 * <code>"-eNDXEHLO"</code> will work.
 * </p>
 *
 * <h2>Specifying tags to include and exclude</h2>
 *
 * <p>
 * You can specify tag names of tests to include or exclude from a run. To specify tags to include,
 * use <code>-n</code> followed by a white-space-separated list of tag names to include, surrounded by
 * double quotes. (The double quotes are not needed if specifying just one tag.)  Similarly, to specify tags
 * to exclude, use <code>-l</code> followed by a white-space-separated
 * list of tag names to exclude, surrounded by double quotes. (As before, the double quotes are not needed
 * if specifying just one tag.) If tags to include is not specified, then all tests
 * except those mentioned in the tags to exclude (and in the <code>org.scalatest.Ignore</code> tag), will be executed.
 * (In other words, the absence of a <code>-n</code> option is like a wildcard, indicating all tests be included.)
 * If tags to include is specified, then only those tests whose tags are mentioned in the argument following <code>-n</code>
 * and not mentioned in the tags to exclude, will be executed. For more information on test tags, see
 * the <a href="../Suite.html">documentation for <code>Suite</code></a>. Here are some examples:
 * </p>
 *
 * <p>
 * <ul>
 * <li><code>-n CheckinTests</code></li>
 * <li><code>-n FunctionalTests -l SlowTests</code></li>
 * <li><code>-n "CheckinTests FunctionalTests" -l "SlowTests NetworkTests"</code></li>
 * </ul>
 * </p>
 *
 * <h2>Executing <code>Suite</code>s in parallel</h2>
 *
 * <p>
 * With the proliferation of multi-core architectures, and the often parallelizable nature of tests, it is useful to be able to run
 * tests in parallel. If you include <code>-c</code> on the command line, <code>Runner</code> will pass a <code>Distributor</code> to 
 * the <code>Suite</code>s you specify with <code>-s</code>. <code>Runner</code> will set up a thread pool to execute any <code>Suite</code>s
 * passed to the <code>Distributor</code>'s <code>put</code> method in parallel. Trait <code>Suite</code>'s implementation of
 * <code>runNestedSuites</code> will place any nested <code>Suite</code>s into this <code>Distributor</code>. Thus, if you have a <code>Suite</code>
 * of tests that must be executed sequentially, you should override <code>runNestedSuites</code> as described in the <a href="../Distributor.html">documentation for <code>Distributor</code></a>.
 * </p>
 *
 * <p>
 * The <code>-c</code> option may optionally be appended with a number (e.g.
 * "<code>-c10</code>" -- no intervening space) to specify the number of
 * threads to be created in the thread pool.  If no number (or 0) is
 * specified, the number of threads will be decided based on the number of
 * processors available.
 * </p>
 *
 * <h2>Specifying <code>Suite</code>s</h2>
 *
 * <p>
 * Suites are specified on the command line with a <b>-s</b> followed by the fully qualified
 * name of a <code>Suite</code> subclass, as in:
 * </p>
 *
 * <pre class="stExamples">-s com.artima.serviceuitest.ServiceUITestkit</pre>
 *
 * <p>
 * Each specified suite class must be public, a subclass of
 * <code>org.scalatest.Suite</code>, and contain a public no-arg constructor.
 * <code>Suite</code> classes must be specified with fully qualified names. 
 * The specified <code>Suite</code> classes may be
 * loaded from the classpath. If a runpath is specified with the
 * <code>-p</code> option, specified <code>Suite</code> classes may also be loaded from the runpath.
 * All specified <code>Suite</code> classes will be loaded and instantiated via their no-arg constructor.
 * </p>
 *
 * <p>
 * The runner will invoke <code>execute</code> on each instantiated <code>org.scalatest.Suite</code>,
 * passing in the dispatch reporter to each <code>execute</code> method.
 * </p>
 *
 * <p>
 * <code>Runner</code> is intended to be used from the command line. It is included in <code>org.scalatest</code>
 * package as a convenience for the user. If this package is incorporated into tools, such as IDEs, which take
 * over the role of runner, object <code>org.scalatest.tools.Runner</code> may be excluded from that implementation of the package.
 * All other public types declared in package <code>org.scalatest.tools.Runner</code> should be included in any such usage, however,
 * so client software can count on them being available.
 * </p>
 *
 * <a name="membersOnlyWildcard">
 * <h2>Specifying "members-only" and "wildcard" <code>Suite</code> paths</h2>
 * </a>
 *
 * <p>
 * If you specify <code>Suite</code> path names with <code>-m</code> or <code>-w</code>, <code>Runner</code> will automatically
 * discover and execute accessible <code>Suite</code>s in the runpath that are either a member of (in the case of <code>-m</code>)
 * or enclosed by (in the case of <code>-w</code>) the specified path. As used in this context, a <em>path</em> is a portion of a fully qualified name.
 * For example, the fully qualifed name <code>com.example.webapp.MySuite</code> contains paths <code>com</code>, <code>com.example</code>, and <code>com.example.webapp</code>.
 * The fully qualifed name <code>com.example.webapp.MyObject.NestedSuite</code> contains paths <code>com</code>, <code>com.example</code>,
 * <code>com.example.webapp</code>, and <code>com.example.webapp.MyObject</code>.
 * An <em>accessible <code>Suite</code></em> is a public class that extends <code>org.scalatest.Suite</code>
 * and defines a public no-arg constructor. Note that <code>Suite</code>s defined inside classes and traits do not have no-arg constructors,
 * and therefore won't be discovered. <code>Suite</code>s defined inside singleton objects, however, do get a no-arg constructor by default, thus
 * they can be discovered.
 * </p>
 *
 * <p>
 * For example, if you specify <code>-m com.example.webapp</code>
 * on the command line, and you've placed <code>com.example.webapp.RedSuite</code> and <code>com.example.webapp.BlueSuite</code>
 * on the runpath, then <code>Runner</code> will instantiate and execute both of those <code>Suite</code>s. The difference
 * between <code>-m</code> and <code>-w</code> is that for <code>-m</code>, only <code>Suite</code>s that are direct members of the named path
 * will be discovered. For <code>-w</code>, any <code>Suite</code>s whose fully qualified
 * name begins with the specified path will be discovered. Thus, if <code>com.example.webapp.controllers.GreenSuite</code>
 * exists on the runpath, invoking <code>Runner</code> with <code>-w com.example.webapp</code> will cause <code>GreenSuite</code>
 * to be discovered, because its fully qualifed name begins with <code>"com.example.webapp"</code>. But if you invoke <code>Runner</code>
 * with <code>-m com.example.webapp</code>, <code>GreenSuite</code> will <em>not</em> be discovered because it is directly
 * a member of <code>com.example.webapp.controllers</code>, not <code>com.example.webapp</code>.
 * </p>
 *
 * <p>
 * If you specify no <code>-s</code>, <code>-m</code>, or <code>-w</code> arguments on the command line to <code>Runner</code>, it will discover and execute all accessible <code>Suite</code>s
 * in the runpath.
 * </p>
 *
 * <h2>Specifying TestNG XML config file paths</h2>
 *
 * <p>
 * If you specify one or more file paths with <code>-t</code>, <code>Runner</code> will create a <code>org.scalatest.testng.TestNGWrapperSuite</code>,
 * passing in a <code>List</code> of the specified paths. When executed, the <code>TestNGWrapperSuite</code> will create one <code>TestNG</code> instance
 * and pass each specified file path to it for running. If you include <code>-t</code> arguments, you must include TestNG's jar file on the class path or runpath.
 * The <code>-t</code> argument will enable you to run existing <code>TestNG</code> tests, including tests written in Java, as part of a ScalaTest run.
 * You need not use <code>-t</code> to run suites written in Scala that extend <code>TestNGSuite</code>. You can simply run such suites with 
 * <code>-s</code>, <code>-m</code>, or </code>-w</code> parameters.
 * </p>
 *
 * <h2>Specifying JUnit tests</h2>
 *
 * <p>
 * JUnit tests, including ones written in Java, may be run by specifying
 * <code>-j classname</code>, where the classname is a valid JUnit class
 * such as a TestCase, TestSuite, or a class implementing a static suite()
 * method returning a TestSuite. </p>
 * <p>
 * To use this option you must include a JUnit jar file on your classpath.
 * </p>
 *
 * @author Bill Venners
 * @author George Berger
 * @author Josh Cough
 */
object Runner {

  private val RUNNER_JFRAME_START_X: Int = 150
  private val RUNNER_JFRAME_START_Y: Int = 100

  //                     TO
  // We always include a PassFailReporter on runs in order to determine
  // whether or not all tests passed.
  //
  // The thread that calls Runner.run() will either start a GUI, if a graphic
  // reporter was requested, or just run the tests itself. If a GUI is started,
  // an event handler thread will get going, and it will start a RunnerThread,
  // which will actually do the running. The GUI can repeatedly start RunnerThreads
  // and RerunnerThreads, until the GUI is closed. If -c is specified, that means
  // the tests should be run concurrently, which in turn means a Distributor will
  // be passed to the execute method of the Suites, which will in turn populate
  // it with its nested suites instead of executing them directly in the same
  // thread. The Distributor works in conjunction with a pool of threads that
  // will take suites out of the distributor queue and execute them. The DispatchReporter
  // will serialize all reports via an actor, which because that actor uses receive
  // not react, will have its own thread. So the DispatchReporter actor's thread will
  // be the one that actually invokes TestFailed, RunAborted, etc., on this PassFailReporter.
  // The thread that invoked Runner.run(), will be the one that calls allTestsPassed.
  //
  // The thread that invoked Runner.run() will be the one to instantiate the PassFailReporter
  // and in its primary constructor acquire the single semaphore permit. This permit will
  // only be released by the DispatchReporter's actor thread when a runAborted, runStopped,
  // or runCompleted is invoked. allTestsPassed will block until it can reacquire the lone
  // semaphore permit. Thus, a PassFailReporter can just be used for one run, then it is
  // spent. A new PassFailReporter is therefore created each time the Runner.run() method is invoked.
  //
  private class PassFailReporter extends Reporter {

    @volatile private var failedAbortedOrStopped = false
    private val runDoneSemaphore = new Semaphore(1)
    runDoneSemaphore.acquire()

    override def apply(event: Event) {
      event match {
        case _: TestFailed =>
          failedAbortedOrStopped = true

        case _: RunAborted =>
          failedAbortedOrStopped = true
          runDoneSemaphore.release()

        case _: SuiteAborted =>
          failedAbortedOrStopped = true

        case _: RunStopped =>
          failedAbortedOrStopped = true
          runDoneSemaphore.release() 

        case _: RunCompleted =>
          runDoneSemaphore.release()

        case _ =>
      }
    }

    def allTestsPassed = {
      runDoneSemaphore.acquire()
      !failedAbortedOrStopped
    }
  }

  // TODO: I don't think I'm enforcing that properties can't start with "org.scalatest"
  // TODO: I don't think I'm handling rejecting multiple -f/-r with the same arg. -f fred.txt -f fred.txt should
  // fail, as should -r MyReporter -r MyReporter. I'm failing on -o -o, -g -g, and -e -e, but the error messages
  // could indeed be nicer.
  /**
   * Runs a suite of tests, with optional GUI. See the main documentation for this singleton object for the details.
   */
  def main(args: Array[String]) {
    val result = runOptionallyWithPassFailReporter(args, true)

    if (result)
      exit(0)
    else
      exit(1)
  }

  /**
   * Runs a suite of tests, with optional GUI. See the main documentation for this singleton object for the details.
   * The difference between this method and <code>main</code> is simply that this method will block until the run
   * has completed, aborted, or been stopped, and return <code>true</code> if all tests executed and passed. In other
   * words, if any test fails, or if any suite aborts, or if the run aborts or is stopped, this method will
   * return <code>false</code>. This value is used, for example, by the ScalaTest ant task to determine whether
   * to continue the build if <code>haltOnFailure</code> is set to <code>true</code>.
   *
   * @return true if all tests were executed and passed.
   */
  def run(args: Array[String]): Boolean = {
    runOptionallyWithPassFailReporter(args, true)
  }

  private def runOptionallyWithPassFailReporter(args: Array[String], runWithPassFailReporter: Boolean): Boolean = {

    checkArgsForValidity(args) match {
      case Some(s) => {
        println(s)
        exit(1)
      }
      case None =>
    }

    val (
      runpathArgsList,
      reporterArgsList,
      suiteArgsList,
      junitArgsList,
      propertiesArgsList,
      includesArgsList,
      excludesArgsList,
      concurrentList,
      membersOnlyArgsList,
      wildcardArgsList,
      testNGArgsList
    ) = parseArgs(args)

    val fullReporterConfigurations: ReporterConfigurations =
      if (reporterArgsList.isEmpty)
        // If no reporters specified, just give them a graphic reporter
        new ReporterConfigurations(Some(GraphicReporterConfiguration(Set())), Nil, Nil, None, None, Nil, Nil)
      else
        parseReporterArgsIntoConfigurations(reporterArgsList)

    val suitesList: List[String] = parseSuiteArgsIntoNameStrings(suiteArgsList, "-s")
    val junitsList: List[String] = parseSuiteArgsIntoNameStrings(junitArgsList, "-j")
    val runpathList: List[String] = parseRunpathArgIntoList(runpathArgsList)
    val propertiesMap: Map[String, String] = parsePropertiesArgsIntoMap(propertiesArgsList)
    val tagsToInclude: Set[String] = parseCompoundArgIntoSet(includesArgsList, "-n")
    val tagsToExclude: Set[String] = parseCompoundArgIntoSet(excludesArgsList, "-l")
    val concurrent: Boolean = !concurrentList.isEmpty
    val numThreads: Int = parseConcurrentNumArg(concurrentList)
    val membersOnlyList: List[String] = parseSuiteArgsIntoNameStrings(membersOnlyArgsList, "-m")
    val wildcardList: List[String] = parseSuiteArgsIntoNameStrings(wildcardArgsList, "-w")
    val testNGList: List[String] = parseSuiteArgsIntoNameStrings(testNGArgsList, "-t")

    val filter = Filter(if (tagsToInclude.isEmpty) None else Some(tagsToInclude), tagsToExclude)

    // If there's a graphic reporter, we need to leave it out of
    // reporterSpecs, because we want to pass all reporterSpecs except
    // the graphic reporter's to the RunnerJFrame (because RunnerJFrame *is*
    // the graphic reporter).
    val reporterConfigs: ReporterConfigurations =
      fullReporterConfigurations.graphicReporterConfiguration match {
        case None => fullReporterConfigurations
        case Some(grs) => {
          new ReporterConfigurations(
            None,
            fullReporterConfigurations.fileReporterConfigurationList,
            fullReporterConfigurations.xmlReporterConfigurationList,
            fullReporterConfigurations.standardOutReporterConfiguration,
            fullReporterConfigurations.standardErrReporterConfiguration,
            fullReporterConfigurations.htmlReporterConfigurationList,
            fullReporterConfigurations.customReporterConfigurationList
          )
        }
      }

    val passFailReporter = if (runWithPassFailReporter) Some(new PassFailReporter) else None

    fullReporterConfigurations.graphicReporterConfiguration match {
      case Some(GraphicReporterConfiguration(configSet)) => {
        val graphicEventsToPresent: Set[EventToPresent] = EventToPresent.allEventsToPresent filter
          (if (configSet.contains(FilterTestStarting)) {_ != PresentTestStarting} else etp => true) filter
          (if (configSet.contains(FilterTestSucceeded)) {_ != PresentTestSucceeded} else etp => true) filter
          (if (configSet.contains(FilterTestIgnored)) {_ != PresentTestIgnored} else etp => true) filter
          (if (configSet.contains(FilterTestPending)) {_ != PresentTestPending} else etp => true) filter
          (if (configSet.contains(FilterSuiteStarting)) {_ != PresentSuiteStarting} else etp => true) filter
          (if (configSet.contains(FilterSuiteCompleted)) {_ != PresentSuiteCompleted} else etp => true) filter
          (if (configSet.contains(FilterInfoProvided)) {_ != PresentInfoProvided} else etp => true) 

        val abq = new ArrayBlockingQueue[RunnerJFrame](1)
        usingEventDispatchThread {
          val rjf = new RunnerJFrame(graphicEventsToPresent, reporterConfigs, suitesList, junitsList, runpathList,
            filter, propertiesMap, concurrent, membersOnlyList, wildcardList, testNGList, passFailReporter, numThreads)
          rjf.setLocation(RUNNER_JFRAME_START_X, RUNNER_JFRAME_START_Y)
          rjf.setVisible(true)
          rjf.prepUIForRunning()
          rjf.runFromGUI()
          abq.put(rjf)
        }
        // To get the Ant task to work, the main thread needs to block until
        // The GUI window exits.
        val rjf = abq.take()
        rjf.blockUntilWindowClosed()
      }
      case None => { // Run the test without a GUI
        withClassLoaderAndDispatchReporter(runpathList, reporterConfigs, None, passFailReporter) {
          (loader, dispatchReporter) => {
            doRunRunRunDaDoRunRun(dispatchReporter, suitesList, junitsList, new Stopper {}, filter,
                propertiesMap, concurrent, membersOnlyList, wildcardList, testNGList, runpathList, loader, new RunDoneListener {}, 1, numThreads) 
          }
        }
      }
    }
    
    passFailReporter match {
      case Some(pfr) => pfr.allTestsPassed
      case None => false
    }
  }

  // Returns an Option[String]. Some is an error message. None means no error.
  private[scalatest] def checkArgsForValidity(args: Array[String]) = {

    val lb = new ListBuffer[String]
    val it = args.iterator
    while (it.hasNext) {
      val s = it.next
      // Style advice
      // If it is multiple else ifs, then make it symetrical. If one needs an open curly brace, put it on all
      // If an if just has another if, a compound statement, go ahead and put the open curly brace's around the outer one
      if (s.startsWith("-p") || s.startsWith("-f") || s.startsWith("-u") || s.startsWith("-h") || s.startsWith("-r") || s.startsWith("-n") || s.startsWith("-x") || s.startsWith("-l") || s.startsWith("-s") || s.startsWith("-j") || s.startsWith("-m") || s.startsWith("-w") || s.startsWith("-t")) {
        if (it.hasNext)
          it.next
      }
      else if (!s.startsWith("-D") && !s.startsWith("-g") && !s.startsWith("-o") && !s.startsWith("-e") && !s.startsWith("-c")) {
        lb += s
      }
    }
    val argsList = lb.toList
    if (argsList.length != 0)
      Some("Unrecognized argument" + (if (argsList.isEmpty) ": " else "s: ") + argsList.mkString("", ", ", "."))
    else
      None
  }

  //
  // Examines concurrent option arg to see if it contains an optional numeric
  // value representing the number of threads to use, e.g. -c10 for 10 threads.
  //
  // It's possible for user to specify the -c option multiple times on the
  // command line, although it isn't particularly useful.  This method scans
  // through multiples until it finds one with a number appended and uses
  // that.  If none have a number it just returns 0.
  //
  private[scalatest] def parseConcurrentNumArg(concurrentList: List[String]):
  Int = {
    val opt = concurrentList.find(_.matches("-c\\d+"))

    opt match {
      case Some(arg) => arg.replace("-c", "").toInt
      case None      => 0
    }
  }

  private[scalatest] def parseArgs(args: Array[String]) = {

    val runpath = new ListBuffer[String]()
    val reporters = new ListBuffer[String]()
    val suites = new ListBuffer[String]()
    val junits = new ListBuffer[String]()
    val props = new ListBuffer[String]()
    val includes = new ListBuffer[String]()
    val excludes = new ListBuffer[String]()
    val concurrent = new ListBuffer[String]()
    val membersOnly = new ListBuffer[String]()
    val wildcard = new ListBuffer[String]()
    val testNGXMLFiles = new ListBuffer[String]()

    val it = args.iterator
    while (it.hasNext) {

      val s = it.next

      if (s.startsWith("-D")) {
         props += s
      }
      else if (s.startsWith("-p")) {
        runpath += s
        if (it.hasNext)
          runpath += it.next
      }
      else if (s.startsWith("-g")) {
        reporters += s
      }
      else if (s.startsWith("-o")) {
        reporters += s
      }
      else if (s.startsWith("-e")) {
        reporters += s
      }
      else if (s.startsWith("-f")) {
        reporters += s
        if (it.hasNext)
          reporters += it.next
      }
      else if (s.startsWith("-u")) {
        reporters += s
        if (it.hasNext)
          reporters += it.next
      }
      else if (s.startsWith("-h")) {
        reporters += s
        if (it.hasNext)
          reporters += it.next
      }
      else if (s.startsWith("-n")) {
        includes += s
        if (it.hasNext)
          includes += it.next
      }
      else if (s.startsWith("-x")) {
        System.err.println(Resources("dashXDeprecated"))
        excludes += s.replace("-x", "-l")
        if (it.hasNext)
          excludes += it.next
      }
      else if (s.startsWith("-l")) {
        excludes += s
        if (it.hasNext)
          excludes += it.next
      }
      else if (s.startsWith("-r")) {

        reporters += s
        if (it.hasNext)
          reporters += it.next
      }
      else if (s.startsWith("-s")) {

        suites += s
        if (it.hasNext)
          suites += it.next
      }
      else if (s.startsWith("-j")) {

        junits += s
        if (it.hasNext)
          junits += it.next
      }
      else if (s.startsWith("-m")) {

        membersOnly += s
        if (it.hasNext)
          membersOnly += it.next
      }
      else if (s.startsWith("-w")) {

        wildcard += s
        if (it.hasNext)
          wildcard += it.next
      }
      else if (s.startsWith("-c")) {

        concurrent += s
      }
      else if (s.startsWith("-t")) {

        testNGXMLFiles += s
        if (it.hasNext)
          testNGXMLFiles += it.next
      }
      else {
        throw new IllegalArgumentException("Unrecognized argument: " + s)
      }
    }

    (
      runpath.toList,
      reporters.toList,
      suites.toList,
      junits.toList,
      props.toList,
      includes.toList,
      excludes.toList,
      concurrent.toList,
      membersOnly.toList,
      wildcard.toList,
      testNGXMLFiles.toList
    )
  }

  /**
   * Returns a possibly empty ConfigSet containing configuration
   * objects specified in the passed reporterArg. Configuration
   * options are specified immediately following
   * the reporter option, as in:
   *
   * -oFA
   *
   * If no configuration options are specified, this method returns an
   * empty ConfigSet. This method never returns null.
   */
  private def parseConfigSet(reporterArg: String): Set[ReporterConfigParam] = {

    if (reporterArg == null)
      throw new NullPointerException("reporterArg was null")

    if (reporterArg.length < 2)
      throw new IllegalArgumentException("reporterArg < 2")

    // The reporterArg passed includes the initial -, as in "-oFI",
    // so the first config param will be at index 2
    val configString = reporterArg.substring(2)
    val it = configString.iterator
    var set = Set[ReporterConfigParam]()
    while (it.hasNext) 
      it.next match {
        case 'Y' =>  throw new IllegalArgumentException("Use of Y was deprecated in ScalaTest 1.0 and removed in 1.5. Please check the Scaladoc documentation of org.scalatest.Runner for information on valid Reporter config parameters.")
        case 'Z' => throw new IllegalArgumentException("Use of Z was deprecated in ScalaTest 1.0 and removed in 1.5. Please check the Scaladoc documentation of org.scalatest.Runner for information on valid Reporter config parameters.")
        case 'T' => // Use for Dots
        case 'U' => throw new IllegalArgumentException("Use of U was deprecated in ScalaTest 1.0 and removed in 1.5. Please check the Scaladoc documentation of org.scalatest.Runner for information on valid Reporter config parameters.")
        case 'P' =>throw new IllegalArgumentException("Use of P was deprecated in ScalaTest 1.0 and removed in 1.5. Please check the Scaladoc documentation of org.scalatest.Runner for information on valid Reporter config parameters.")
        case 'B' =>throw new IllegalArgumentException("Use of B was deprecated in ScalaTest 1.0 and removed in 1.5. Please check the Scaladoc documentation of org.scalatest.Runner for information on valid Reporter config parameters.")
        case 'I' =>throw new IllegalArgumentException("Use of I was deprecated in ScalaTest 1.0 and removed in 1.5. Please check the Scaladoc documentation of org.scalatest.Runner for information on valid Reporter config parameters.")
        // case 'S' => // Use for Short Stack Traces
        case 'A' =>throw new IllegalArgumentException("Use of A was deprecated in ScalaTest 1.0 and removed in 1.5. Please check the Scaladoc documentation of org.scalatest.Runner for information on valid Reporter config parameters.")
        case 'R' =>throw new IllegalArgumentException("Use of R was deprecated in ScalaTest 1.0 and removed in 1.5. Please check the Scaladoc documentation of org.scalatest.Runner for information on valid Reporter config parameters.")
        case 'G' =>throw new IllegalArgumentException("Use of G was deprecated in ScalaTest 1.0 and removed in 1.5. Please check the Scaladoc documentation of org.scalatest.Runner for information on valid Reporter config parameters.")
        case 'N' => set += FilterTestStarting
        case 'C' => set += FilterTestSucceeded
        case 'X' => set += FilterTestIgnored
        case 'E' => set += FilterTestPending
        case 'H' => set += FilterSuiteStarting
        case 'L' => set += FilterSuiteCompleted
        case 'O' => set += FilterInfoProvided
        case 'W' => set += PresentWithoutColor
        case 'F' => set += PresentFullStackTraces
        case 'S' => set += PresentShortStackTraces
        case 'D' => set += PresentAllDurations
        case c: Char => { 

          // this should be moved to the checker, and just throw an exception here with a debug message. Or allow a MatchError.
          val msg1 = Resources("invalidConfigOption", String.valueOf(c)) + '\n'
          val msg2 =  Resources("probarg", reporterArg) + '\n'

          throw new IllegalArgumentException(msg1 + msg2)
        }
      }
    set
  }

  private[scalatest] def parseReporterArgsIntoConfigurations(args: List[String]) = {
    //
    // Checks to see if any args are smaller than two characters in length.
    // Allows a one-character arg if it's a directory-name parameter, to
    // permit use of "." for example.
    //
    def argTooShort(args: List[String]): Boolean = {
      args match {
        case Nil => false

        case "-u" :: directory :: list => argTooShort(list)

        case x :: list =>
          if (x.length < 2) true
          else              argTooShort(list)
      }
    }

    if (args == null)
      throw new NullPointerException("args was null")

    if (args.exists(_ == null))
      throw new NullPointerException("an arg String was null")

    if (argTooShort(args)) // TODO: check and print out a user friendly message for this
      throw new IllegalArgumentException("an arg String was less than 2 in length: " + args)

    for (dashX <- List("-g", "-o", "-e")) {
      if (args.toList.count(_.startsWith(dashX)) > 1) // TODO: also check and print a user friendly message for this
        throw new IllegalArgumentException("Only one " + dashX + " allowed")
    }

    // TODO: also check and print a user friendly message for this
    // again here, i had to skip some things, so I had to use an iterator.
    val it = args.iterator
    while (it.hasNext)
      it.next.take(2).toString match {
        case "-g" =>
        case "-o" =>
        case "-e" =>
        case "-f" =>
          if (it.hasNext)
            it.next // scroll past the filename
          else
            throw new IllegalArgumentException("-f needs to be followed by a file name arg: ")
        case "-u" =>
          if (it.hasNext) {
            val directory = it.next
            if (!(new File(directory).isDirectory))
              throw new IllegalArgumentException(
                "arg for -u option is not a directory [" + directory + "]")
            else {}
          }
          else {
            throw new IllegalArgumentException("-u needs to be followed by a directory name arg: ")
          }
        case "-h" =>
          if (it.hasNext)
            it.next // scroll past the filename
          else
            throw new IllegalArgumentException("-h needs to be followed by a file name arg: ")
        case "-r" =>
          if (it.hasNext)
            it.next // scroll past the reporter class
          else
            throw new IllegalArgumentException("-r needs to be followed by a reporter class name arg: ")
        case arg: String =>
          throw new IllegalArgumentException("An arg started with an invalid character string: " + arg)
      }

    val graphicReporterConfigurationOption =
      args.find(arg => arg.startsWith("-g")) match {
        case Some(dashGString) =>
          val configSet = parseConfigSet(dashGString)
          if (configSet.contains(PresentShortStackTraces))
            throw new IllegalArgumentException("Cannot specify an S (present short stack traces) configuration parameter for the graphic reporter (because it shows them anyway): " + dashGString)
          if (configSet.contains(PresentFullStackTraces))
            throw new IllegalArgumentException("Cannot specify an F (present full stack traces) configuration parameter for the graphic reporter (because it shows them anyway): " + dashGString)
          if (configSet.contains(PresentWithoutColor))
            throw new IllegalArgumentException("Cannot specify a W (present without color) configuration parameter for the graphic reporter: " + dashGString)
          if (configSet.contains(PresentAllDurations))
            throw new IllegalArgumentException("Cannot specify a D (present all durations) configuration parameter for the graphic reporter (because it shows them all anyway): " + dashGString)
          Some(new GraphicReporterConfiguration(configSet))
        case None => None
      }

    def buildFileReporterConfigurationList(args: List[String]) = {
      val it = args.iterator
      val lb = new ListBuffer[FileReporterConfiguration]
      while (it.hasNext) {
        val arg = it.next
        if (arg.startsWith("-f"))
          lb += new FileReporterConfiguration(parseConfigSet(arg), it.next)
      }
      lb.toList
    }
    val fileReporterConfigurationList = buildFileReporterConfigurationList(args)

    def buildXmlReporterConfigurationList(args: List[String]) = {
      val it = args.iterator
      val lb = new ListBuffer[XmlReporterConfiguration]
      while (it.hasNext) {
        val arg = it.next
        if (arg.startsWith("-u"))
          lb += new XmlReporterConfiguration(Set[ReporterConfigParam](),
                                             it.next)
      }
      lb.toList
    }
    val xmlReporterConfigurationList = buildXmlReporterConfigurationList(args)

    def buildHtmlReporterConfigurationList(args: List[String]) = {
      val it = args.iterator
      val lb = new ListBuffer[HtmlReporterConfiguration]
      while (it.hasNext) {
        val arg = it.next
        if (arg.startsWith("-h"))
          lb += new HtmlReporterConfiguration(parseConfigSet(arg), it.next)
      }
      lb.toList
    }
    val htmlReporterConfigurationList = buildHtmlReporterConfigurationList(args)

    val standardOutReporterConfigurationOption =
      args.find(arg => arg.startsWith("-o")) match {
        case Some(dashOString) => Some(new StandardOutReporterConfiguration(parseConfigSet(dashOString)))
        case None => None
      }

    val standardErrReporterConfigurationOption =
      args.find(arg => arg.startsWith("-e")) match {
        case Some(dashEString) => Some(new StandardErrReporterConfiguration(parseConfigSet(dashEString)))
        case None => None
      }

    def buildCustomReporterConfigurationList(args: List[String]) = {
      val it = args.iterator
      val lb = new ListBuffer[CustomReporterConfiguration]
      while (it.hasNext) {
        val arg = it.next
        if (arg.startsWith("-r")) {
          val dashRString = arg
          val customReporterClassName = it.next
          val configSet = parseConfigSet(dashRString)
          if (configSet.contains(PresentShortStackTraces))
            throw new IllegalArgumentException("Cannot specify an S (present short stack traces) configuration parameter for a custom reporter: " + dashRString + " " + customReporterClassName)
          if (configSet.contains(PresentFullStackTraces))
            throw new IllegalArgumentException("Cannot specify an F (present full stack traces) configuration parameter for a custom reporter: " + dashRString + " " + customReporterClassName)
          if (configSet.contains(PresentWithoutColor))
            throw new IllegalArgumentException("Cannot specify a W (without color) configuration parameter for a custom reporter: " + dashRString + " " + customReporterClassName)
          if (configSet.contains(PresentAllDurations))
            throw new IllegalArgumentException("Cannot specify a D (present all durations) configuration parameter for a custom reporter: " + dashRString + " " + customReporterClassName)
          lb += new CustomReporterConfiguration(configSet, customReporterClassName)
        }
      }
      lb.toList
    }
    val customReporterConfigurationList = buildCustomReporterConfigurationList(args)

    // Here instead of one loop, i go through the loop several times.
    new ReporterConfigurations(
      graphicReporterConfigurationOption,
      fileReporterConfigurationList,
      xmlReporterConfigurationList,
      standardOutReporterConfigurationOption,
      standardErrReporterConfigurationOption,
      htmlReporterConfigurationList,
      customReporterConfigurationList
    )
  }

  // Used to parse -s, -j, -m, and -w args, one of which will be passed as a String as dashArg
  private[scalatest] def parseSuiteArgsIntoNameStrings(args: List[String], dashArg: String) = {

    if (args == null)
      throw new NullPointerException("args was null")

    if (args.exists(_ == null))
      throw new NullPointerException("an arg String was null")

    if (dashArg != "-j" && dashArg != "-s" && dashArg != "-w" && dashArg != "-m" && dashArg != "-t")
      throw new NullPointerException("dashArg invalid: " + dashArg)

    val lb = new ListBuffer[String]
    val it = args.iterator
    while (it.hasNext) {
      val dashS = it.next
      if (dashS != dashArg)
        throw new IllegalArgumentException("Every other element, starting with the first, must be -s")
      if (it.hasNext) {
        val suiteName = it.next
        if (!suiteName.startsWith("-"))
          lb += suiteName
        else
          throw new IllegalArgumentException("Expecting a Suite class name to follow -s, but got: " + suiteName)
      }
      else
        throw new IllegalArgumentException("Last element must be a Suite class name, not a -s.")
    }
    lb.toList
  }

  private[scalatest] def parseCompoundArgIntoSet(args: List[String], expectedDashArg: String): Set[String] = 
      Set() ++ parseCompoundArgIntoList(args, expectedDashArg)

  private[scalatest] def parseRunpathArgIntoList(args: List[String]): List[String] = parseCompoundArgIntoList(args, "-p")

  private[scalatest] def parseCompoundArgIntoList(args: List[String], expectedDashArg: String): List[String] = {

    if (args == null)
      throw new NullPointerException("args was null")

    if (args.exists(_ == null))
      throw new NullPointerException("an arg String was null")

    if (args.length == 0) {
      List()
    }
    else if (args.length == 2) {
      val dashArg = args(0)
      val runpathArg = args(1)

      if (dashArg != expectedDashArg)
        throw new IllegalArgumentException("First arg must be " + expectedDashArg + ", but was: " + dashArg)

      if (runpathArg.trim.isEmpty)
        throw new IllegalArgumentException("The runpath string must actually include some non-whitespace characters.")

      splitPath(runpathArg)
    }
    else {
      throw new IllegalArgumentException("Runpath must be either zero or two args: " + args)
    }
  }

  //
  // Splits a space-delimited path into its component parts.
  //
  // Spaces within path elements may be escaped with backslashes, e.g.
  // "c:\Documents\ And\ Settings c:\Program\ Files"
  //
  // See comments for isCompleteToken() below for exceptions.
  //
  private val START_TOKEN_PATTERN = Pattern.compile("""^\s*(.*?)(\s|$)""")
  private val FULL_TOKEN_PATTERN  = Pattern.compile("""^\s*(.+?)(((?<=[^\\])\s)|$)""")
  private def splitPath(pathArg: String): List[String] = {
    val path = pathArg.trim

    if (path.isEmpty) Nil
    else {
      val startMatcher = START_TOKEN_PATTERN.matcher(path)

      if (!startMatcher.find())
        throw new RuntimeException("unexpected startMatcher path [" +
                                   path + "]")
      val token = startMatcher.group(1)

      if (isCompleteToken(token)) {
        token :: splitPath(path.substring(startMatcher.end))
      }
      else {
        val fullMatcher = FULL_TOKEN_PATTERN.matcher(path)

        if (!fullMatcher.find())
          throw new RuntimeException("unexpected fullMatcher path [" +
                                     path + "]")
        val fullToken = fullMatcher.group(1).replaceAll("""\\(\s)""", "$1")

        fullToken :: splitPath(path.substring(fullMatcher.end))
      }
    }
  }

  //
  // Determines whether specified token is complete or partial.
  //
  // Tokens are considered partial if they end with a backslash, since
  // backslash is used to escape spaces that would otherwise be
  // treated as delimiters within the path string.
  //
  // Exceptions are cases where the token ends in a backslash
  // but is still considered a complete token because it constitutes
  // a valid representation of a root directory on a windows system,
  // e.g. "c:\" or just "\".
  //
  private val ROOT_DIR_PATTERN = Pattern.compile("""(?i)\\|[a-z]:\\""")
  private def isCompleteToken(token: String): Boolean = {
    val matcher = ROOT_DIR_PATTERN.matcher(token)

    matcher.matches() || (token(token.length - 1) != '\\')
  }

  private[scalatest] def parsePropertiesArgsIntoMap(args: List[String]) = {

    if (args == null)
      throw new NullPointerException("args was null")

    if (args.exists(_ == null))
      throw new NullPointerException("an arg String was null")

    if (args.exists(_.indexOf('=') == -1))
      throw new IllegalArgumentException("A -D arg does not contain an equals sign.")

    if (args.exists(!_.startsWith("-D")))
      throw new IllegalArgumentException("A spice arg does not start with -D.")

    if (args.exists(_.indexOf('=') == 2))
      throw new IllegalArgumentException("A spice arg does not have a key to the left of the equals sign.")

    if (args.exists(arg => arg.indexOf('=') == arg.length - 1))
      throw new IllegalArgumentException("A spice arg does not have a value to the right of the equals sign.")

    val tuples = for (arg <- args) yield {
      val keyValue = arg.substring(2) // Cut off the -D at the beginning
      val equalsPos = keyValue.indexOf('=')
      val key = keyValue.substring(0, equalsPos)
      val value = keyValue.substring(equalsPos + 1)
      (key, value)
    }

    scala.collection.immutable.Map() ++ tuples
  }

  // For debugging.
/*
  private[scalatest] def printOpts(opt: EventToPresent.Set32) {
    if (opt.contains(EventToPresent.PresentRunStarting))
      println("PresentRunStarting")
    if (opt.contains(EventToPresent.PresentTestStarting))
      println("PresentTestStarting")
    if (opt.contains(EventToPresent.PresentTestSucceeded))
      println("PresentTestSucceeded")
    if (opt.contains(EventToPresent.PresentTestFailed))
      println("PresentTestFailed")
    if (opt.contains(EventToPresent.PresentTestIgnored))
      println("PresentTestIgnored")
    if (opt.contains(EventToPresent.PresentSuiteStarting))
      println("PresentSuiteStarting")
    if (opt.contains(EventToPresent.PresentSuiteCompleted))
      println("PresentSuiteCompleted")
    if (opt.contains(EventToPresent.PresentSuiteAborted))
      println("PresentSuiteAborted")
    if (opt.contains(EventToPresent.PresentInfoProvided))
      println("PresentInfoProvided")
    if (opt.contains(EventToPresent.PresentRunStopped))
      println("PresentRunStopped")
    if (opt.contains(EventToPresent.PresentRunCompleted))
      println("PresentRunCompleted")
    if (opt.contains(EventToPresent.PresentRunAborted))
      println("PresentRunAborted")
  }
*/

  private def configSetMinusNonFilterParams(configSet: Set[ReporterConfigParam]) =
    (((configSet - PresentShortStackTraces) - PresentFullStackTraces) - PresentWithoutColor) - PresentAllDurations

  private[scalatest] def getDispatchReporter(reporterSpecs: ReporterConfigurations, graphicReporter: Option[Reporter], passFailReporter: Option[Reporter], loader: ClassLoader) = {

    def getReporterFromConfiguration(configuration: ReporterConfiguration): Reporter =

      configuration match {
        case StandardOutReporterConfiguration(configSet) =>
          if (configSetMinusNonFilterParams(configSet).isEmpty)
            new StandardOutReporter(
              configSet.contains(PresentAllDurations),
              !configSet.contains(PresentWithoutColor),
              configSet.contains(PresentShortStackTraces) || configSet.contains(PresentFullStackTraces),
              configSet.contains(PresentFullStackTraces) // If they say both S and F, F overrules
            )
          else
            new FilterReporter(
              new StandardOutReporter(
                configSet.contains(PresentAllDurations),
                !configSet.contains(PresentWithoutColor),
                configSet.contains(PresentShortStackTraces) || configSet.contains(PresentFullStackTraces),
                configSet.contains(PresentFullStackTraces) // If they say both S and F, F overrules
              ),
              configSet
            )

      case StandardErrReporterConfiguration(configSet) =>
        if (configSetMinusNonFilterParams(configSet).isEmpty)
          new StandardErrReporter(
            configSet.contains(PresentAllDurations),
            !configSet.contains(PresentWithoutColor),
            configSet.contains(PresentShortStackTraces) || configSet.contains(PresentFullStackTraces),
            configSet.contains(PresentFullStackTraces) // If they say both S and F, F overrules
          )
        else
          new FilterReporter(
            new StandardErrReporter(
              configSet.contains(PresentAllDurations),
              !configSet.contains(PresentWithoutColor),
              configSet.contains(PresentShortStackTraces) || configSet.contains(PresentFullStackTraces),
              configSet.contains(PresentFullStackTraces) // If they say both S and F, F overrules
            ),
            configSet
          )

      case FileReporterConfiguration(configSet, fileName) =>
        if (configSetMinusNonFilterParams(configSet).isEmpty)
          new FileReporter(
            fileName,
            configSet.contains(PresentAllDurations),
            !configSet.contains(PresentWithoutColor),
            configSet.contains(PresentShortStackTraces) || configSet.contains(PresentFullStackTraces),
            configSet.contains(PresentFullStackTraces) // If they say both S and F, F overrules
          )
        else
          new FilterReporter(
            new FileReporter(
              fileName,
              configSet.contains(PresentAllDurations),
              !configSet.contains(PresentWithoutColor),
              configSet.contains(PresentShortStackTraces) || configSet.contains(PresentFullStackTraces),
              configSet.contains(PresentFullStackTraces) // If they say both S and F, F overrules
            ),
            configSet
          )

      case XmlReporterConfiguration(configSet, directory) =>
        new XmlReporter(directory)

        case HtmlReporterConfiguration(configSet, fileName) =>
          if (configSetMinusNonFilterParams(configSet).isEmpty)
            new HtmlReporter(
              fileName,
              configSet.contains(PresentAllDurations),
              !configSet.contains(PresentWithoutColor),
              configSet.contains(PresentShortStackTraces) || configSet.contains(PresentFullStackTraces),
              configSet.contains(PresentFullStackTraces) // If they say both S and F, F overrules
            )
          else
            new FilterReporter(
              new HtmlReporter(
                fileName,
                configSet.contains(PresentAllDurations),
                !configSet.contains(PresentWithoutColor),
                configSet.contains(PresentShortStackTraces) || configSet.contains(PresentFullStackTraces),
                configSet.contains(PresentFullStackTraces) // If they say both S and F, F overrules
              ),
              configSet
            )

      case CustomReporterConfiguration(configSet, reporterClassName) => {
        val customReporter = getCustomReporter(reporterClassName, loader, "-r... " + reporterClassName)
        if (configSet.isEmpty)
          customReporter
        else
          new FilterReporter(customReporter, configSet)
      }
      case GraphicReporterConfiguration(configSet) => throw new RuntimeException("Should never happen.")
    }

    val reporterSeq =
      (for (spec <- reporterSpecs)
        yield getReporterFromConfiguration(spec))

    val almostFullReporterList: List[Reporter] =
      graphicReporter match {
        case None => reporterSeq.toList
        case Some(gRep) => gRep :: reporterSeq.toList
      }
      
    val fullReporterList: List[Reporter] =
      passFailReporter match {
        case Some(pfr) => pfr :: almostFullReporterList
        case None => almostFullReporterList
      }

    new DispatchReporter(fullReporterList)
  }

  private def getCustomReporter(reporterClassName: String, loader: ClassLoader, argString: String): Reporter = {
    try {
      val reporterClass: java.lang.Class[_] = loader.loadClass(reporterClassName) 
      reporterClass.newInstance.asInstanceOf[Reporter]
    }    // Could probably catch ClassCastException too
    catch {
      case e: ClassNotFoundException => {

        val msg1 = Resources("cantLoadReporterClass", reporterClassName)
        val msg2 = Resources("probarg", argString)
        val msg = msg1 + "\n" + msg2
    
        val iae = new IllegalArgumentException(msg)
        iae.initCause(e)
        throw iae
      }
      case e: InstantiationException => {

        val msg1 = Resources("cantInstantiateReporter", reporterClassName)
        val msg2 = Resources("probarg", argString)
        val msg = msg1 + "\n" + msg2
    
        val iae = new IllegalArgumentException(msg)
        iae.initCause(e)
        throw iae
      }
      case e: IllegalAccessException => {

        val msg1 = Resources("cantInstantiateReporter", reporterClassName)
        val msg2 = Resources("probarg", argString)
        val msg = msg1 + "\n" + msg2
    
        val iae = new IllegalArgumentException(msg)
        iae.initCause(e)
        throw iae
      }
    }
  }

  private[scalatest] def doRunRunRunDaDoRunRun(
    dispatch: DispatchReporter,
    suitesList: List[String],
    junitsList: List[String],
    stopRequested: Stopper,
    filter: Filter,
    configMap: Map[String, String],
    concurrent: Boolean,
    membersOnlyList: List[String],
    wildcardList: List[String],
    testNGList: List[String],
    runpath: List[String],
    loader: ClassLoader,
    doneListener: RunDoneListener,
    runStamp: Int,
    numThreads: Int
  ) = {

    // TODO: add more, and to RunnerThread too
    if (dispatch == null)
      throw new NullPointerException
    if (suitesList == null)
      throw new NullPointerException
    if (junitsList == null)
      throw new NullPointerException
    if (stopRequested == null)
      throw new NullPointerException
    if (filter == null)
      throw new NullPointerException
    if (configMap == null)
      throw new NullPointerException
    if (membersOnlyList == null)
      throw new NullPointerException
    if (wildcardList == null)
      throw new NullPointerException
    if (runpath == null)
      throw new NullPointerException
    if (loader == null)
      throw new NullPointerException
    if (doneListener == null)
      throw new NullPointerException

    val tagsToInclude =
      filter.tagsToInclude match {
        case None => Set[String]()
        case Some(tti) => tti
      }
    val tagsToExclude = filter.tagsToExclude

    var tracker = new Tracker(new Ordinal(runStamp))

    val runStartTime = System.currentTimeMillis

    try {
      val loadProblemsExist =
        try {
          val unassignableList = suitesList.filter(className => !classOf[Suite].isAssignableFrom(loader.loadClass(className)))
          if (!unassignableList.isEmpty) {
            val names = for (className <- unassignableList) yield " " + className
            dispatch(RunAborted(tracker.nextOrdinal(), Resources("nonSuite") + names, None))
            true
          }
          else {
            false
          }
        }
        catch {
          case e: ClassNotFoundException => {
            dispatch(RunAborted(tracker.nextOrdinal(), Resources("cannotLoadSuite", e.getMessage), Some(e)))
            true
          }
        }
  
      if (!loadProblemsExist) {
        try {
          val namedSuiteInstances: List[Suite] =
            for (suiteClassName <- suitesList)
              yield {
                val clazz = loader.loadClass(suiteClassName)
                clazz.newInstance.asInstanceOf[Suite]
              }

          val junitSuiteInstances: List[Suite] =
            for (junitClassName <- junitsList)
              yield new JUnitWrapperSuite(junitClassName, loader)

          val testNGWrapperSuiteList: List[TestNGWrapperSuite] =
            if (!testNGList.isEmpty)
              List(new TestNGWrapperSuite(testNGList))
            else
              Nil

          val (membersOnlySuiteInstances, wildcardSuiteInstances) = {

            val membersOnlyAndBeginsWithListsAreEmpty = membersOnlyList.isEmpty && wildcardList.isEmpty // They didn't specify any -m's or -w's on the command line


            // TODO: rename the 'BeginsWith' variables to 'Wildcard' to match the terminology that
            // we ended up with on the outside
            // TODO: Should SuiteDiscoverHelper be a singleton object?
            if (membersOnlyAndBeginsWithListsAreEmpty && (!suitesList.isEmpty || !junitsList.isEmpty)) {
              (Nil, Nil) // No DiscoverySuites in this case. Just run Suites named with -s or -j
            }
            else {
              val accessibleSuites = (new SuiteDiscoveryHelper).discoverSuiteNames(runpath, loader)

              if (membersOnlyAndBeginsWithListsAreEmpty && suitesList.isEmpty && junitsList.isEmpty) {
                // In this case, they didn't specify any -w, -m, -s, or -j on the command line, so the default
                // is to run any accessible Suites discovered on the runpath
                (Nil, List(new DiscoverySuite("", accessibleSuites, true, loader)))
              }
              else {
                val membersOnlyInstances =
                  for (membersOnlyName <- membersOnlyList)
                    yield new DiscoverySuite(membersOnlyName, accessibleSuites, false, loader)

                val wildcardInstances =
                  for (wildcardName <- wildcardList)
                    yield new DiscoverySuite(wildcardName, accessibleSuites, true, loader)

                (membersOnlyInstances, wildcardInstances)
              }
            }
          }

          val suiteInstances: List[Suite] = namedSuiteInstances ::: junitSuiteInstances ::: membersOnlySuiteInstances ::: wildcardSuiteInstances ::: testNGWrapperSuiteList

          val testCountList =
            for (suite <- suiteInstances)
              yield suite.expectedTestCount(filter)
  
          def sumInts(list: List[Int]): Int =
            list match {
              case Nil => 0
              case x :: xs => x + sumInts(xs)
            }

          val expectedTestCount = sumInts(testCountList)

          dispatch(RunStarting(tracker.nextOrdinal(), expectedTestCount, configMap))

          if (concurrent) {

            // Because some tests may do IO, will create a pool of 2 times the number of processors reported
            // by the Runtime's availableProcessors method.
            val poolSize =
              if (numThreads > 0) numThreads
              else Runtime.getRuntime.availableProcessors * 2

            val execSvc: ExecutorService = Executors.newFixedThreadPool(poolSize)
            try {

            if (System.getProperty("org.scalatest.tools.Runner.forever", "false") == "true") {
              val distributor = new ConcurrentDistributor(dispatch, stopRequested, filter, configMap, execSvc)
              while (true) {
                for (suite <- suiteInstances) {
                  distributor.apply(suite, tracker.nextTracker())
                }
                distributor.waitUntilDone()
              }
            }
            else {
              val distributor = new ConcurrentDistributor(dispatch, stopRequested, filter, configMap, execSvc)
              for (suite <- suiteInstances) {
                distributor.apply(suite, tracker.nextTracker())
              }
              distributor.waitUntilDone()
            }
            }
            finally {
              execSvc.shutdown()
            }
          }
          else {
            for (suite <- suiteInstances) {
              val suiteRunner = new SuiteRunner(suite, dispatch, stopRequested, filter,
                  configMap, None, tracker)
              suiteRunner.run()
            }
          }

          val duration = System.currentTimeMillis - runStartTime
          if (stopRequested()) {
            dispatch(RunStopped(tracker.nextOrdinal(), Some(duration)))
          }
          else {
            dispatch(RunCompleted(tracker.nextOrdinal(), Some(duration)))
          }
        }
        catch {
          case e: InstantiationException =>
            dispatch(RunAborted(tracker.nextOrdinal(), Resources("cannotInstantiateSuite", e.getMessage), Some(e), Some(System.currentTimeMillis - runStartTime)))
          case e: IllegalAccessException =>
            dispatch(RunAborted(tracker.nextOrdinal(), Resources("cannotInstantiateSuite", e.getMessage), Some(e), Some(System.currentTimeMillis - runStartTime)))
          case e: NoClassDefFoundError =>
            dispatch(RunAborted(tracker.nextOrdinal(), Resources("cannotLoadClass", e.getMessage), Some(e), Some(System.currentTimeMillis - runStartTime)))
          case e: Throwable =>
            dispatch(RunAborted(tracker.nextOrdinal(), Resources.bigProblems(e), Some(e), Some(System.currentTimeMillis - runStartTime)))
        }
      }
    }
    finally {
      dispatch.dispatchDisposeAndWaitUntilDone()
      doneListener.done()
    }
  }

  private[scalatest] def excludesWithIgnore(excludes: Set[String]) = excludes + "org.scalatest.Ignore"

  private[scalatest] def withClassLoaderAndDispatchReporter(runpathList: List[String], reporterSpecs: ReporterConfigurations,
      graphicReporter: Option[Reporter], passFailReporter: Option[Reporter])(f: (ClassLoader, DispatchReporter) => Unit): Unit = {

    val loader: ClassLoader = getRunpathClassLoader(runpathList)
    try {
      Thread.currentThread.setContextClassLoader(loader)
      try {
        val dispatchReporter = getDispatchReporter(reporterSpecs, graphicReporter, passFailReporter, loader)
        try {
          f(loader, dispatchReporter)
        }
        finally {
          dispatchReporter.dispatchDisposeAndWaitUntilDone()
        }
      }
      catch {
        // getDispatchReporter may complete abruptly with an exception, if there is an problem trying to load
        // or instantiate a custom reporter class.
        case ex: Throwable => {
          System.err.println(Resources("bigProblemsMaybeCustomReporter"))
          ex.printStackTrace(System.err)
        }
      }
    }
    finally {
      // eventually call close on the RunpathClassLoader
    }
  }

  private[scalatest] def getRunpathClassLoader(runpathList: List[String]): ClassLoader = {

    if (runpathList == null)
      throw new NullPointerException
    if (runpathList.isEmpty) {
      classOf[Suite].getClassLoader // Could this be null technically?
    }
    else {
      val urlsList: List[URL] =
        for (raw <- runpathList) yield {
          try {
            new URL(raw)
          }
          catch {
            case murle: MalformedURLException => {
  
              // Assume they tried to just pass in a file name
              val file: File = new File(raw)
  
              // file.toURL may throw MalformedURLException too, but for now
              // just let that propagate up.
              file.toURI.toURL // If a dir, comes back terminated by a slash
            }
          }
        }
  
      // Here is where the Jini preferred class loader stuff went.

      // Tell the URLConnections to not use caching, so that repeated runs and reruns actually work
      // on the latest binaries.
      for (url <- urlsList) {
        try {
          url.openConnection.setDefaultUseCaches(false)
        }
        catch {
          case e: IOException => // just ignore these
        }
      }

      new URLClassLoader(urlsList.toArray, classOf[Suite].getClassLoader)
    }
  }

  private[scalatest] def usingEventDispatchThread(f: => Unit): Unit = {
    SwingUtilities.invokeLater(
      new Runnable() {
        def run() {
          f
        }
      }
    )
  }
}
