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
package org.scalatest.events

import org.scalatest._

/**
 * Class that holds information about names for an <code>InfoProvided</code> event.
 *
 * <p>
 * An <code>InfoProvided</code> event may be fired from anywhere. In this respect <code>InfoProvided</code> is different
 * from the other events, for which it is defined whether they are fired in the context of a suite or test.
 * If fired in the context of a test, the <code>InfoProvided</code> event should include a <code>NameInfo</code> in which
 * <code>testName</code> is defined. If fired in the context of a suite, but not a test, the <code>InfoProvided</code> event
 * should include a <code>NameInfo</code> in which <code>testName</code> is <em>not</em> defined. If fired within the context
 * of neither a suite nor a test, the <code>nameInfo</code> of the <code>InfoProvided</code> event (an <code>Option[NameInfo]</code>) should be <code>None</code>.
 * </p>
 *
 * <p>
 * If either <code>suiteClassName</code> or <code>testName</code> is defined, then <code>suiteName</code> must be defined.
 * The suite class name parameter is optional even if a suite name is provided by passing a <code>Some</code> as <code>suiteName</code>,
 * because suites in ScalaTest are an abstraction that
 * need not necessarily correspond to one class. Nevertheless, it most cases each suite will correspond
 * to a class, and when it does, the fully qualified name of that class should be reported by passing a
 * <code>Some</code> for <code>suiteClassName</code>. One use for this bit of information is JUnit integration,
 * because the "name" provided to a JUnit <code>org.junit.runner.Description</code> appears to usually include
 * a fully qualified class name by convention.
 * </p>
 *
 * @param suiteName an optional name of the suite about which the information was provided
 * @param suiteClassName an optional fully qualifed <code>Suite</code> class name about which the information was provided
 * @param testName an optional name of the test about which the information was provided
 *
 * @author Bill Venners
 */
final case class NameInfo(suiteName: String, suiteClassName: Option[String], testName: Option[String])

/**
 * Companion object for case class <a href="NameInfo.html"><code>NameInfo</code></a>.
 */
object NameInfo
