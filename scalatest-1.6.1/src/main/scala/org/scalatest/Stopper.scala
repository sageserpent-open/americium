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
 * Trait whose instances can indicate whether a stop has been requested. This is passed in
 * to the <code>run</code> method of <code>Suite</code>, so that running suites of tests can be
 * requested to stop early.
 *
 * @author Bill Venners
 */
trait Stopper /* extends (() => Boolean) */ {

  /**
   * Indicates whether a stop has been requested.  Call this method
   * to determine whether a running test should stop. The <code>run</code> method of any <code>Suite</code>, or
   * code invoked by <code>run</code>, should periodically check the
   * stop requested function. If <code>true</code>,
   * the <code>run</code> method should interrupt its work and simply return.
   */
  def apply() = false
}

/**
 * Companion object to Stopper that holds a deprecated implicit conversion.
 */
object Stopper {

  /**
   * Converts a <code>Stopper</code> to a function type that prior to the ScalaTest 1.5 release the
   * <code>Stopper</code> extended.
   *
   * <p>
   * Prior to ScalaTest 1.5, <code>Stopper</code> extended function type <code>() => Boolean</code>.
   * This inheritance relationship was severed in 1.5 to make it possible to implement <code>Stopper</code>s in Java, a request by an IDE
   * vendor to isolate their ScalaTest integration from binary incompatibility between different Scala/ScalaTest releases.
   * To make a trait easily implementable in Java, it needs to have no concrete methods. <code>Stopper</code> itself does not declare
   * any concrete methods, but <code>() => Boolean</code> does.
   * </p>
   *
   * <p>
   * This implicit conversion was added in ScalaTest 1.5 to avoid breaking any source code that was actually using
   * <code>Stopper</code> as an <code>() => Boolean</code> function. It is unlikely anyone was actually doing that, but if you were
   * and now get the deprecation warning, please email scalatest-users@googlegroups.com if you believe this implicit conversion should
   * be retained. If no one steps forward with a compelling justification, it will be removed in a future version of ScalaTest.
   * </p>
   */
  @deprecated("See the documentation for Stopper.convertStopperToFunction for information")
  implicit def convertStopperToFunction(stopper: Stopper): () => Boolean =
    () => stopper()
}
