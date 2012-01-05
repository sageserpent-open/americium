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
 * Trait whose instances facilitate parallel execution of <code>Suite</code>s.
 * An optional <code>Distributor</code> is passed to the <code>run</code> method of <code>Suite</code>. If a
 * <code>Distributor</code> is indeed passed, trait <code>Suite</code>'s implementation of <code>run</code> will
 * populate that <code>Distributor</code> with its nested <code>Suite</code>s (by passing them to the <code>Distributor</code>'s
 * <code>apply</code> method) rather than executing the nested <code>Suite</code>s directly. It is then up to another thread or process
 * to execute those <code>Suite</code>s.
 *
 * <p>
 * If you have a set of nested <code>Suite</code>s that must be executed sequentially, you can mix in trait
 * <code>SequentialNestedSuiteExecution</code>, which overrides <code>runNestedSuites</code> and
 * calls <code>super</code>'s <code>runNestedSuites</code> implementation, passing in <code>None</code> for the
 * <code>Distributor</code>.
 * </p>
 * 
 * <p>
 * Implementations of this trait must be thread safe.
 * </p>
 *
 * @author Bill Venners
 */
trait Distributor /* extends ((Suite, Tracker) => Unit) */ {

  /**
   * Puts a <code>Suite</code> into the <code>Distributor</code>.
   *
   * @param suite the <code>Suite</code> to put into the <code>Distributor</code>.
   * @param tracker a <code>Tracker</code> to pass to the <code>Suite</code>'s <code>run</code> method.
   *
   * @throws NullPointerException if either <code>suite</code> or <code>tracker</code> is <code>null</code>.
   */
  def apply(suite: Suite, tracker: Tracker)
}

/**
 * Companion object to Distributor that holds a deprecated implicit conversion.
 */
object Distributor {

  /**
   * Converts a <code>Distributor</code> to a function type that prior to the ScalaTest 1.5 release the
   * <code>Distributor</code> extended.
   *
   * <p>
   * Prior to ScalaTest 1.5, <code>Distributor</code> extended function type <code>(Suite, Tracker) => Unit</code>.
   * This inheritance relationship was severed in 1.5 to make it possible to implement <code>Distributor</code>s in Java, a request by an IDE
   * vendor to isolate their ScalaTest integration from binary incompatibility between different Scala/ScalaTest releases.
   * To make a trait easily implementable in Java, it needs to have no concrete methods. <code>Distributor</code> itself does not declare
   * any concrete methods, but <code>(Suite, Tracker) => Unit</code> does.
   * </p>
   *
   * <p>
   * This implicit conversion was added in ScalaTest 1.5 to avoid breaking any source code that was actually using
   * <code>Distributor</code> as an <code>(Suite, Tracker) => Unit</code> function. It is unlikely anyone was actually doing that, but if you were
   * and now get the deprecation warning, please email scalatest-users@googlegroups.com if you believe this implicit conversion should
   * be retained. If no one steps forward with a compelling justification, it will be removed in a future version of ScalaTest.
   * </p>
   */
  @deprecated("See the documentation for Distributor.convertDistributorToFunction for information")
  implicit def convertDistributorToFunction(distributor: Distributor): (Suite, Tracker) => Unit =
    (suite: Suite, tracker: Tracker) => distributor(suite, tracker)
}
