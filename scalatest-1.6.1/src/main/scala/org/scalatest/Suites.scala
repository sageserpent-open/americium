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

/**
 * A <code>Suite</code> class that takes zero to many <code>Suite</code>s,
 *  which will be returned from its <code>nestedSuites</code> method.
 *
 * <p>
 * For example, you can define a suite that always executes a list of
 * nested suites like this:
 * </p>
 *
 * <pre class="stHighlight">
 * class StepsSuite extends Suites {
 *   new Step1Suite,
 *   new Step2Suite,
 *   new Step3Suite,
 *   new Step4Suite,
 *   new Step5Suite
 * }
 * </pre>
 *
 * <p>
 * When <code>StepsSuite</code> is executed, it will execute its
 * nested suites in the passed order: <code>Step1Suite</code>, <code>Step2Suite</code>,
 * <code>Step3Suite</code>, <code>Step4Suite</code>, and <code>Step5Suite</code>.
 * </p>
 *
 * @param suitesToNest a sequence of <code>Suite</code>s to nest.
 *
 * @throws NullPointerException if <code>suitesToNest</code>, or any suite
 * it contains, is <code>null</code>.
 *
 * @author Bill Venners
 */
class Suites(suitesToNest: Suite*) extends Suite {

  for (s <- suitesToNest) {
    if (s == null)
      throw new NullPointerException("A passed suite was null")
  }

  /**
   * Returns a list containing the suites passed to the constructor in
   * the order they were passed.
   */
  override val nestedSuites = suitesToNest.toList
}

/**
 * Companion object to class <code>Suites</code> that offers an <code>apply</code> factory method
 * for creating a <code>Suites</code> instance.
 *
 * <p>
 * One use case for this object is to run multiple specification-style suites in the Scala interpreter, like this:
 * </p>
 *
 * <pre class="stREPL">
 * scala> Suites(new MyFirstSuite, new MyNextSuite).execute()
 * </pre>
 */
object Suites {

  /**
   * Factory method for creating a <code>Suites</code> instance.
   */
  def apply(suitesToNest: Suite*): Suites = new Suites(suitesToNest: _*)
}

/**
 * <strong>SuperSuite has been deprecated and will be removed in a future
 * release of ScalaTest. Please change any uses of <code>SuperSuite</code>
 * to a corresponding use of <a href="Suites.html"><code>Suites</code></a> or <a href="Specs.html"><code>Specs</code></a> instead.</strong>
 */
@deprecated("Please use org.scalatest.Suites or org.scalatest.Specs instead.")
class SuperSuite(suitesToNest: List[Suite]) extends Suites(suitesToNest: _*)
// deprecated in 1.5, so remove in 1.7 or later

