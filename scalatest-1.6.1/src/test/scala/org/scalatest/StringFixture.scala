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

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.fixture._

trait StringFixture { this: FixtureSuite =>
  type FixtureParam = String
  def withFixture(test: OneArgTest) {
    test("hi")
  }
}

trait StringFixtureSpec extends FixtureSpec with StringFixture
trait StringFixtureFunSuite extends FixtureFunSuite with StringFixture
