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

class SuitesSpec extends Spec {

  val a = new Suite {}
  val b = new FunSuite {}
  val c = new Spec {}
  val d = new WordSpec {}
  val e = new FeatureSpec {}

  describe("Suites") {
    it("should return the passed suites from nestedSuites") {
      val f = new Suites(a, b, c, d, e)
      assert(f.nestedSuites == List(a, b, c, d, e))
      val g = new Suites(Array(a, b, c, d, e): _*)
      assert(g.nestedSuites == List(a, b, c, d, e))
      intercept[NullPointerException] {
        new Suites(a, b, null, d, e)
      }
      intercept[NullPointerException] {
        val aNull: Array[Suite] = null
        new Suites(aNull: _*)
      }
    }
  }
  describe("Specs") {
    it("should return the passed suites from nestedSuites") {
      val f = new Specs(a, b, c, d, e)
      assert(f.nestedSuites == List(a, b, c, d, e))
      val g = new Specs(Array(a, b, c, d, e): _*)
      assert(g.nestedSuites == List(a, b, c, d, e))
      intercept[NullPointerException] {
        new Specs(a, b, null, d, e)
      }
      intercept[NullPointerException] {
        val aNull: Array[Suite] = null
        new Specs(aNull: _*)
      }
    }
  }
  describe("SuperSuite") {
    it("should still work after being deprecated and extended from Suites") {
      val f = new SuperSuite(List(a, b, c, d, e))
      assert(f.nestedSuites == List(a, b, c, d, e))
    }
  }
}

