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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitTestFailedError

class ClueSpec extends FlatSpec with ShouldMatchers {

// TOTEST: clue object with toString. clue object with null toString. all-whitespace clue string
  "The modifyMessage method on TFE" should "return the an exception with an equal message option if passed a function that returns the same option passed to it" in {
    val tfe = new TestFailedException("before", 3)
    tfe.modifyMessage(opt => opt) should equal (tfe)
  }

  it should "return the new exception with the clue string prepended, separated by a space char if passed a function that does that" in {
    val tfe = new TestFailedException("message", 3)
    val clue = "clue"
    val fun: (Option[String] => Option[String]) =
      opt => opt match {
        case Some(msg) => Some(clue + " " + msg)
        case None => Some(clue)
      }
    tfe.modifyMessage(fun).message.get should be ("clue message")
  }

  "The modifyMessage method on JUTFE" should "return the an exception with an equal message option if passed a function that returns the same option passed to it" in {
    val jutfe = new JUnitTestFailedError("before", 3)
    jutfe.modifyMessage(opt => opt) should equal (jutfe)
  }

  it should "return the new exception with the clue string prepended, separated by a space char if passed a function that does that" in {
    val jutfe = new JUnitTestFailedError("message", 3)
    val clue = "clue"
    val fun: (Option[String] => Option[String]) =
      opt => opt match {
        case Some(msg) => Some(clue + " " + msg)
        case None => Some(clue)
      }
    jutfe.modifyMessage(fun).message.get should be ("clue message")
  }

  // ******* withClue tests *******

  "The withClue construct" should "allow any non-ModifiableMessage exception to pass through" in {
    val iae = new IllegalArgumentException
    val caught = intercept[IllegalArgumentException] {
      withClue("howdy") {
        throw iae 
      }
    }
    caught should be theSameInstanceAs (iae)
  }

  it should "given an empty clue string, rethrow the same TFE exception" in {
    val tfe = new TestFailedException("before", 3)
    val caught = intercept[TestFailedException] {
      withClue("") {
        throw tfe 
      }
    }
    caught should be theSameInstanceAs (tfe)
  }

  it should "given an all-whitespace clue string, should throw a new TFE with the white space prepended to the old message" in {
    val tfe = new TestFailedException("message", 3)
    val white = "    "
    val caught = intercept[TestFailedException] {
      withClue(white) {
        throw tfe 
      }
    }
    caught should not be theSameInstanceAs (tfe)
    caught.message should be ('defined)
    caught.message.get should equal (white + "message")
  }

  it should "given a non-empty clue string with no trailing white space, throw a new instance of the caught TFE exception that has all fields the same except a prepended clue string followed by an extra space" in {
    val tfe = new TestFailedException("message", 3)
    val caught = intercept[TestFailedException] {
      withClue("clue") {
        throw tfe 
      }
    }
    caught should not be theSameInstanceAs (tfe)
    caught.message should be ('defined)
    caught.message.get should equal ("clue message")
  }

  it should "given a non-empty clue string with a trailing space, throw a new instance of the caught TFE exception that has all fields the same except a prepended clue string (followed by no extra space)" in {
    val tfe = new TestFailedException("message", 3)
    val caught = intercept[TestFailedException] {
      withClue("clue ") { // has a trailing space
        throw tfe 
      }
    }
    caught should not be theSameInstanceAs (tfe)
    caught.message should be ('defined)
    caught.message.get should equal ("clue message")
  }

  it should "given a non-empty clue string with a end of line, throw a new instance of the caught TFE exception that has all fields the same except a prepended clue string (followed by no extra space)" in {
    val tfe = new TestFailedException("message", 3)
    val caught = intercept[TestFailedException] {
      withClue("clue\n") { // has a end of line character
        throw tfe 
      }
    }
    caught should not be theSameInstanceAs (tfe)
    caught.message should be ('defined)
    caught.message.get should equal ("clue\nmessage")
  }

  it should "given an empty clue string, rethrow the same JUTFE exception" in {
    val jutfe = new JUnitTestFailedError("before", 3)
    val caught = intercept[JUnitTestFailedError] {
      withClue("") {
        throw jutfe 
      }
    }
    caught should be theSameInstanceAs (jutfe)
  }

  it should "given an all-whitespace clue string, should throw a new JUTFE with the white space prepended to the old message" in {
    val jutfe = new JUnitTestFailedError("message", 3)
    val white = "    "
    val caught = intercept[JUnitTestFailedError] {
      withClue(white) {
        throw jutfe 
      }
    }
    caught should not be theSameInstanceAs (jutfe)
    caught.message should be ('defined)
    caught.message.get should equal (white + "message")
  }

  it should "given a non-empty clue string with no trailing white space, throw a new instance of the caught JUTFE exception that has all fields the same except a prepended clue string followed by an extra space" in {
    val jutfe = new JUnitTestFailedError("message", 3)
    val caught = intercept[JUnitTestFailedError] {
      withClue("clue") {
        throw jutfe 
      }
    }
    caught should not be theSameInstanceAs (jutfe)
    caught.message should be ('defined)
    caught.message.get should equal ("clue message")
  }

  it should "given a non-empty clue string with a trailing space, throw a new instance of the caught JUTFE exception that has all fields the same except a prepended clue string (followed by no extra space)" in {
    val jutfe = new JUnitTestFailedError("message", 3)
    val caught = intercept[JUnitTestFailedError] {
      withClue("clue ") { // has a trailing space
        throw jutfe 
      }
    }
    caught should not be theSameInstanceAs (jutfe)
    caught.message should be ('defined)
    caught.message.get should equal ("clue message")
  }

  it should "given a non-empty clue string with a end of line, throw a new instance of the caught JUTFE exception that has all fields the same except a prepended clue string (followed by no extra space)" in {
    val jutfe = new JUnitTestFailedError("message", 3)
    val caught = intercept[JUnitTestFailedError] {
      withClue("clue\n") { // has a end of line character
        throw jutfe 
      }
    }
    caught should not be theSameInstanceAs (jutfe)
    caught.message should be ('defined)
    caught.message.get should equal ("clue\nmessage")
  }

  // ***** tests with objects other than String *****

  it should "given an object with a non-empty clue string with no trailing white space, throw a new instance of the caught TFE exception that has all fields the same except a prepended clue string followed by an extra space" in {
    val tfe = new TestFailedException("message", 3)
    val list = List(1, 2, 3)
    val caught = intercept[TestFailedException] {
      withClue(list) {
        throw tfe 
      }
    }
    caught should not be theSameInstanceAs (tfe)
    caught.message should be ('defined)
    caught.message.get should equal ("List(1, 2, 3) message")
  }

/*
  // ***** withClueFunction *****
  "the withClueFunction method" should "allow any non-ModifiableMessage exception to pass through" in {
    val iae = new IllegalArgumentException
    val caught = intercept[IllegalArgumentException] {
      withClueFunction(opt => Some("howdy")) {
        throw iae 
      }
    }
    caught should be theSameInstanceAs (iae)
  }
*/
}

