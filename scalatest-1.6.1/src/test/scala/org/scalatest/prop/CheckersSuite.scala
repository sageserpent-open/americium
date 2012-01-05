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
package org.scalatest.prop

import org.scalatest._
import org.scalacheck._
import Arbitrary._
import Prop._
import org.scalatest.matchers.ShouldMatchers._

class CheckersSuite extends Suite with Checkers {

  def testCheckProp() {

    // Ensure a success does not fail in an exception
    val propConcatLists = forAll((a: List[Int], b: List[Int]) => a.size + b.size == (a ::: b).size)
    check(propConcatLists)

    // Ensure a failed property does throw an assertion error
    val propConcatListsBadly = forAll((a: List[Int], b: List[Int]) => a.size + b.size == (a ::: b).size + 1)
    intercept[TestFailedException] {
      check(propConcatListsBadly)
    }

    // Ensure a property that throws an exception causes an assertion error
    val propConcatListsExceptionally = forAll((a: List[Int], b: List[Int]) => throw new StringIndexOutOfBoundsException)
    intercept[TestFailedException] {
      check(propConcatListsExceptionally)
    }

    // Ensure a property that doesn't generate enough test cases throws an assertion error
    val propTrivial = forAll( (n: Int) => (n == 0) ==> (n == 0) )
    intercept[TestFailedException] {
      check(propTrivial)
    }

    // Make sure a Generator that doesn't throw an exception works OK
    val smallInteger = Gen.choose(0, 100)
    val propSmallInteger = Prop.forAll(smallInteger)(n => n >= 0 && n <= 100)
    check(propSmallInteger)

    // Make sure a Generator that doesn't throw an exception works OK
    val smallEvenInteger = Gen.choose(0, 200) suchThat (_ % 2 == 0)
    val propEvenInteger = Prop.forAll(smallEvenInteger)(n => n >= 0 && n <= 200 && n % 2 == 0)
    check(propEvenInteger)

    // Make sure a Generator t throws an exception results in an TestFailedException
    // val smallEvenIntegerWithBug = Gen.choose(0, 200) suchThat (throw new ArrayIndexOutOfBoundsException)
    val myArrayException = new ArrayIndexOutOfBoundsException
    val smallEvenIntegerWithBug = Gen.choose(0, 200) suchThat (n => throw myArrayException )
    val propEvenIntegerWithBuggyGen = Prop.forAll(smallEvenIntegerWithBug)(n => n >= 0 && n <= 200 && n % 2 == 0)
    val caught1 = intercept[TestFailedException] {
      check(propEvenIntegerWithBuggyGen)
    }
    assert(caught1.getCause === myArrayException)

    // Make sure that I get a thrown exception back as the TFE's cause
    val myIAE = new IllegalArgumentException
    val caught2 = intercept[TestFailedException] {
      check((s: String, t: String, u: String) => { throw myIAE })
    }
    assert(caught2.getCause === myIAE)

    val complexProp = forAll { (m: Int, n: Int) =>
      val res = n * m
      (res >= m)    :| "result > #1" &&
      (res >= n)    :| "result > #2" &&
      (res < m + n) :| "result not sum"
    }

    intercept[GeneratorDrivenPropertyCheckFailedException] {
      check(complexProp)
    }

    // This code shows up in the front page for ScalaTest
    import scala.collection.mutable.Stack
    check {
      (list: List[Int]) => {
        val stack = new Stack[Int]
        for (element <- list) stack.push(element)
        stack.iterator.toList == list.reverse
      }
    }
  }

  def testCheckPropWithSuccessOf() {

    // Ensure a success does not fail in an exception
    val propConcatLists = forAll { (a: List[Int], b: List[Int]) =>
      a.size + b.size should equal ((a ::: b).size)
      true
    }
    check(propConcatLists)

    // Ensure a failed property does throw an assertion error
    val propConcatListsBadly = forAll { (a: List[Int], b: List[Int]) =>
      a.size + b.size should equal ((a ::: b).size + 1)
      true
    }
    intercept[TestFailedException] {
      check(propConcatListsBadly)
    }

    // Ensure a property that throws an exception causes an assertion error
    val propConcatListsExceptionally = forAll { (a: List[Int], b: List[Int]) =>
      throw new StringIndexOutOfBoundsException
      true
    }
    intercept[TestFailedException] {
      check(propConcatListsExceptionally)
    }

    // Ensure a property that doesn't generate enough test cases throws an assertion error
    val propTrivial = forAll { (n: Int) =>
      (n == 0) ==> {
        n should equal (0)
        true
      }
    }
    intercept[TestFailedException] {
      check(propTrivial)
    }

    // Make sure a Generator that doesn't throw an exception works OK
    val smallIntegers = Gen.choose(0, 100)
    val propSmallInteger = Prop.forAll(smallIntegers) { n =>
      n should (be >= 0 and be <= 100)
      true
    }
    check(propSmallInteger)

    // Make sure a Generator that doesn't throw an exception works OK
    val smallEvenIntegers = Gen.choose(0, 200) suchThat (_ % 2 == 0)
    val propEvenInteger = Prop.forAll(smallEvenIntegers) { n =>
      n should (be >= 0 and be <= 200)
      n % 2 should equal (0)
      true
    }
    check(propEvenInteger)

    // Make sure a Generator t throws an exception results in an TestFailedException
    // val smallEvenIntegerWithBug = Gen.choose(0, 200) suchThat (throw new ArrayIndexOutOfBoundsException)
    val myArrayException = new ArrayIndexOutOfBoundsException
    val smallEvenIntegerWithBug = Gen.choose(0, 200) suchThat (n => throw myArrayException )
    val propEvenIntegerWithBuggyGen = Prop.forAll(smallEvenIntegerWithBug) { n =>
      n should (be >= 0 and be <= 200)
      n % 2 should equal (0)
      true
    }
    val caught1 = intercept[TestFailedException] {
      check(propEvenIntegerWithBuggyGen)
    }
    assert(caught1.getCause === myArrayException)

    // Make sure that I get a thrown exception back as the TFE's cause
    val myIAE = new IllegalArgumentException
    val caught2 = intercept[TestFailedException] {
      check((s: String, t: String, u: String) => throw myIAE)
    }
    assert(caught2.getCause === myIAE)

    val complexProp = forAll { (m: Int, n: Int) =>
      val res = n * m
      res should be >= m
      res should be >= n
      res should be < (m + n)
      true
    }

    intercept[GeneratorDrivenPropertyCheckFailedException] {
      check(complexProp)
    }

    // This code shows up in the front page for ScalaTest
    import scala.collection.mutable.Stack
    check {
      (list: List[Int]) => {
        val stack = new Stack[Int]
        for (element <- list) stack.push(element)
        stack.iterator.toList == list.reverse
      }
    }
  }
}
