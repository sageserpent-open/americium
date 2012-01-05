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
package org.scalatest.matchers

import org.scalatest._

class ShouldThrowSpec extends WordSpec with ShouldMatchers {

  "The evaluating { ... } should produce [ExceptionType] syntax" should {

    "fail if a different exception is thrown" in {
      val caught1 = intercept[TestFailedException] {
        evaluating { "hi".charAt(-1) } should produce [IllegalArgumentException]
      }
      assert(caught1.getMessage === "Expected exception java.lang.IllegalArgumentException to be thrown, but java.lang.StringIndexOutOfBoundsException was thrown.")
    }

    "fail if no exception is thrown" in {
      val caught2 = intercept[TestFailedException] {
        evaluating { "hi" } should produce [IllegalArgumentException]
      }
      assert(caught2.getMessage === "Expected exception java.lang.IllegalArgumentException to be thrown, but no exception was thrown.")
    }

    "succeed if the expected exception is thrown" in {
      evaluating { "hi".charAt(-1) } should produce [StringIndexOutOfBoundsException]
    }
    
    "succeed if a subtype of the expected exception is thrown, where the expected type is a class" in {
      evaluating { "hi".charAt(-1) } should produce [Exception]
    }

    "succeed if a subtype of the expected exception is thrown, where the expected type is a trait" in {
      trait Excitement
      def kaboom() { throw new Exception with Excitement }
      evaluating { kaboom() } should produce [Excitement]
    }
    
    "return the caught exception" in {
      def kaboom() { throw new Exception("howdy") }
      val thrown = evaluating { kaboom() } should produce [Exception]
      thrown.getMessage should be === "howdy"
    }
  }
}
