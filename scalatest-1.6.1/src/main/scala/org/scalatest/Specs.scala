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
 * A <code>Suite</code> class that takes zero to many (likely specification-style) <code>Suite</code>s,
 *  which will be returned from its <code>nestedSuites</code> method.
 *
 * <p>
 * For example, you can define a suite that always executes a list of
 * nested, specification-style suites like this:
 * </p>
 *
 * <pre class="stHighlight">
 * class StepsSpec extends Specs {
 *   new Step1Spec,
 *   new Step2Spec,
 *   new Step3Spec,
 *   new Step4Spec,
 *   new Step5Spec
 * }
 * </pre>
 *
 * <p>
 * When <code>StepsSpec</code> is executed, it will execute its
 * nested suites in the passed order: <code>Step1Spec</code>, <code>Step2Spec</code>,
 * <code>Step3Spec</code>, <code>Step4Spec</code>, and <code>Step5Spec</code>.
 * </p>
 *
 * @param suitesToNest a sequence of <code>Suite</code>s to nest.
 *
 * @throws NullPointerException if <code>suitesToNest</code>, or any suite
 * it contains, is <code>null</code>.
 *
 * @author Bill Venners
 */
class Specs(specsToNest: Suite*) extends Suite {

  for (s <- specsToNest) {
    if (s == null)
      throw new NullPointerException("A passed suite was null")
  }

  /**
   * Returns a list containing the suites passed to the constructor in
   * the order they were passed.
   */
  override val nestedSuites = specsToNest.toList
}

/**
 * Companion object to class <code>Specs</code> that offers an <code>apply</code> factory method
 * for creating a <code>Specs</code> instance.
 *
 * <p>
 * One use case for this object is to run multiple specification-style suites in the Scala interpreter, like this:
 * </p>
 *
 * <pre class="stREPL">
 * scala> Specs(new MyFirstSpec, new MyNextSpec).execute()
 * </pre>
 */
object Specs {

  /**
   * Factory method for creating a <code>Suites</code> instance.
   */
  def apply(specsToNest: Suite*): Specs = new Specs(specsToNest: _*)
}

