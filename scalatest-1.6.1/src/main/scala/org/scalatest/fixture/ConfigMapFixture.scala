/*
 * Copyright 2001-2009 Artima, Inc.
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

/**
 * Trait that when mixed into a <code>FixtureSuite</code> ensures the
 * <code>configMap</code> passed to <code>runTest</code> is passed along
 * as a fixture into each test.
 */
trait ConfigMapFixture { this: FixtureSuite =>

  /**
   * The type of the <code>configMap</code>, which is <code>Map[String, Any]</code>.
   */
  type FixtureParam = Map[String, Any]

  /**
   * Invoke the test function, passing to the the test function the <code>configMap</code>
   * obtained by invoking <code>configMap</code> on the passed <code>OneArgTest</code>.
   *
   * @param fun the <code>OneArgTest</code> to invoke, passing in the
   *   <code>configMap</code> fixture
   */
  def withFixture(test: OneArgTest) {
    test(test.configMap)
  }
}
