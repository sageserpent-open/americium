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

/**
 * A basic Stopper used by the RunnerJFrame to request tests to stop when
 * its Stop button is pressed.
 *
 * @author Bill Venners
 */
private[scalatest] class SimpleStopper extends Stopper {

  // TODO: Change this to AtomicBoolean
  @volatile
  private var stopWasRequested = false

  /**
   * Gets the <code>boolean</code> "stop requested" property. Call this method
   * to determine whether a running test should stop. The <code>run</code> method of any <code>Suite</code>, or
   * code invoked by <code>run</code>, should periodically check the
   * stop requested property. If <code>true</code>,
   * the <code>run</code> method should interrupt its work and simply return.
   */
  override def apply() = stopWasRequested

  /**
   * Sets the stop requested property to the specified <code>Boolean</code> value. Call this method
   * to request that a running test stop. The <code>run</code> method of any <code>Suite</code>, or
   * code invoked by <code>run</code>, should periodically check the
   * stop requested property. If <code>true</code>,
   * the <code>run</code> method should interrupt its work and simply return.
   */
  def requestStop() {
    stopWasRequested = true
  }

  def reset() {
    stopWasRequested = false
  }
}
