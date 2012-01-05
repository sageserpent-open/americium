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

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers

class TableDrivenPropertyCheckFailedExceptionSpec extends Spec with ShouldMatchers with TableDrivenPropertyChecks {

  val baseLineNumber = 22

  describe("The TableDrivenPropertyCheckFailedException") {

    it("should give the proper line on a table-driven property check") {
      val examples =
        Table(
          ("a", "b"),
          (1, 2),
          (3, 4),
          (6, 5),
          (7, 8)
        )
      try {
        forAll (examples) { (a, b) => a should be < b }
      }
      catch {
        case e: TableDrivenPropertyCheckFailedException =>
          e.failedCodeFileNameAndLineNumberString match {
            case Some(s) => s should equal ("TableDrivenPropertyCheckFailedExceptionSpec.scala:" + (baseLineNumber + 15))
            case None => fail("A table-driven property check didn't produce a file name and line number string", e)
          }
        case e =>
          fail("forAll (examples) { (a, b) => a should be < b } didn't produce a TableDrivenPropertyCheckFailedException", e)
      }
    }

    describe("even when it is nested in another describe") {
      it("should give the proper line on a table-driven property check") {
        val examples =
          Table(
            ("a", "b"),
            (1, 2),
            (3, 4),
            (6, 5),
            (7, 8)
          )
        try {
          forAll (examples) { (a, b) => a should be < b }
        }
        catch {
          case e: TableDrivenPropertyCheckFailedException =>
            e.failedCodeFileNameAndLineNumberString match {
              case Some(s) => s should equal ("TableDrivenPropertyCheckFailedExceptionSpec.scala:" + (baseLineNumber + 39))
              case None => fail("A table-driven property check didn't produce a file name and line number string", e)
            }
          case e =>
            fail("forAll (examples) { (a, b) => a should be < b } didn't produce a TableDrivenPropertyCheckFailedException", e)
        }
      }
    }

    it("should return the cause in both cause and getCause") {
      val theCause = new IllegalArgumentException("howdy")
      val tfe = new TableDrivenPropertyCheckFailedException(sde => "doody", Some(theCause), sde => 3, "howdy", List(1, 2, 3), List("a", "b", "c"), 7)
      assert(tfe.cause.isDefined)
      assert(tfe.cause.get === theCause)
      assert(tfe.getCause == theCause)
    }

    it("should return None in cause and null in getCause if no cause") {
      val tfe = new TableDrivenPropertyCheckFailedException(sde => "doody", None, sde => 3, "howdy", List(1, 2, 3), List("a", "b", "c"), 7)
      assert(tfe.cause.isEmpty)
      assert(tfe.getCause == null)
    }

    it("should be equal to itself") {
      val tfe = new TableDrivenPropertyCheckFailedException(sde => "doody", None, sde => 3, "howdy", List(1, 2, 3), List("a", "b", "c"), 7)
      assert(tfe equals tfe)
    }
  }
}
 
