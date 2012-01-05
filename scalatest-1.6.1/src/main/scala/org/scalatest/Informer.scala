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

/**
 * Trait to which custom information about a running suite of tests can be reported.
 * 
 * <p>
 * An <code>Informer</code> is essentially
 * used to wrap a <code>Reporter</code> and provide easy ways to send custom information
 * to that <code>Reporter</code> via an <code>InfoProvided</code> event.
 * <code>Informer</code> contains an <code>apply</code> method that takes an object.
 * The <code>Informer</code> will invoke <code>toString</code> on the passed object and
 * forward the resulting string to the <code>Reporter</code> as the <code>message</code>
 * parameter of an <code>InfoProvided</code> event.
 * </p>
 *
 * <p>
 * Here's an example of using an <code>Informer</code> in a <code>Suite</code>
 * subclass:
 * </p>
 * 
 * <pre class="stHighlight">
 * import org.scalatest._
 * 
 * class MySuite extends Suite {
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
 * scala> (new MySuite).execute()
 * <span class="stGreen">Test Starting - MySuite.testAddition(Reporter)
 * Info Provided - MySuite.testAddition(Reporter): Addition seems to work
 * Test Succeeded - MySuite.testAddition(Reporter)</span>
 * </pre>
 *
 * <p>
 * Traits <code>FunSuite</code>, <code>Spec</code>, <code>FlatSpec</code>, <code>WordSpec</code>, <code>FeatureSpec</code>, and 
 * their sister traits in <code>org.scalatest.fixture</code> package declare an implicit <code>info</code> method that returns
 * an <code>Informer</code>. This implicit <code>info</code> is used, for example, to enable the syntax offered by the
 * <a href="GivenWhenThen.html"><code>GivenWhenThen</code> trait</a>, which contains methods that take an implicit <code>Informer</code>.
 * Here's an example of a <code>FeatureSpec</code> that mixes in <code>GivenWhenThen</code>:
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
 * Were you to run this <code>FeatureSpec</code> in the interpreter, you would see the following messages
 * included in the printed report:
 * </p>
 *
 * <pre class="stREPL">
 * scala> (new ArithmeticFeatureSpec).run()
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
 * @author Bill Venners
 */
trait Informer extends (String => Unit) {

  /**
   * Provide information to the <code>Reporter</code> as the .
   *
   * @param message an object whose <code>toString</code> result will be forwarded to the wrapped <code>Reporter</code>
   *   via an <code>InfoProvided</code> event.
   *
   * @throws NullPointerException if <code>message</code> reference is <code>null</code>
   */
  def apply(message: String): Unit
}
