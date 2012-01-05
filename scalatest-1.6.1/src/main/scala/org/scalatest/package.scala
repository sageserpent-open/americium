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
package org

/**
 * ScalaTest's main traits, classes, and other members, including members supporting ScalaTest's DSL for the Scala interpreter.
 */
package object scalatest {

  private val defaultShell = ShellImpl()

  /**
   * Returns a copy of this <code>Shell</code> with <code>colorPassed</code> configuration parameter set to <code>true</code>.
   */
  lazy val color: Shell = defaultShell.color

  /**
   * Returns a copy of this <code>Shell</code> with <code>durationsPassed</code> configuration parameter set to <code>true</code>.
   */
  lazy val durations: Shell = defaultShell.durations

  /**
   * Returns a copy of this <code>Shell</code> with <code>shortStacksPassed</code> configuration parameter set to <code>true</code>.
   */
  lazy val shortstacks: Shell = defaultShell.shortstacks

  /**
   * Returns a copy of this <code>Shell</code> with <code>fullStacksPassed</code> configuration parameter set to <code>true</code>.
   */
  lazy val fullstacks: Shell = defaultShell.fullstacks

  /**
   * Returns a copy of this <code>Shell</code> with <code>statsPassed</code> configuration parameter set to <code>true</code>.
   */
  lazy val stats: Shell = defaultShell.stats

  /**
   * Returns a copy of this <code>Shell</code> with <code>colorPassed</code> configuration parameter set to <code>false</code>.
   */
  lazy val nocolor: Shell = defaultShell.nocolor

  /**
   * Returns a copy of this <code>Shell</code> with <code>durationsPassed</code> configuration parameter set to <code>false</code>.
   */
  lazy val nodurations: Shell = defaultShell.nodurations

  /**
   * Returns a copy of this <code>Shell</code> with <code>shortStacksPassed</code> configuration parameter set to <code>false</code>.
   */
  lazy val nostacks: Shell = defaultShell.nostacks

  /**
   * Returns a copy of this <code>Shell</code> with <code>statsPassed</code> configuration parameter set to <code>false</code>.
   */
  lazy val nostats: Shell = defaultShell.nostats

  /**
   * Run the passed suite, optionally passing in a test name and config map. 
   *
   * <p>
   * This method will invoke <code>execute</code> on the passed <code>suite</code>, passing in
   * the specified (or default) <code>testName</code> and <code>configMap</code> and the configuration values
   * passed to this <code>Shell</code>'s constructor (<code>colorPassed</code>, <code>durationsPassed</code>, <code>shortStacksPassed</code>,
   * <code>fullStacksPassed</code>, and <code>statsPassed</code>).
   * </p>
   */
  def run(suite: Suite, testName: String = null, configMap: Map[String, Any] = Map()) {
    defaultShell.run(suite, testName, configMap)
  }
}
