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

import events.InfoProvided
import org.scalatest.matchers.ShouldMatchers

class EngineSpec extends FlatSpec with SharedHelpers with ShouldMatchers {

  "EngineSpec.getTestNamePrefix" should "return empty string for Trunk" in {
    val engine = new Engine("concurrentFunSuiteBundleMod", "FunSuite")
    import engine._
    getTestNamePrefix(Trunk) should be ("")
  }

  it should "return empty string for direct children of Trunk" in {
    val engine = new Engine("concurrentFunSuiteBundleMod", "FunSuite")
    import engine._
    val child = DescriptionBranch(Trunk, "Catherine", Some("child prefix"))
    Trunk.subNodes ::= child
    getTestNamePrefix(child) should be ("Catherine child prefix")
  }

  it should "return the parent's description name for DescriptionBranch grandchildren of trunk" in {
    val engine = new Engine("concurrentFunSuiteBundleMod", "FunSuite")
    import engine._
    val child = DescriptionBranch(Trunk, "child", Some("child prefix"))
    Trunk.subNodes ::= child
    val grandchild = DescriptionBranch(child, "grandchild", None)
    child.subNodes ::= grandchild
    getTestNamePrefix(grandchild) should be ("child child prefix grandchild")
  }

  "EngineSpec.getTestName" should "return the prefix, a space, and the testText" in {
    val engine = new Engine("concurrentFunSuiteBundleMod", "FunSuite")
    import engine._
    val child = DescriptionBranch(Trunk, "child", Some("child prefix"))
    Trunk.subNodes ::= child
    val grandchild = DescriptionBranch(child, "grandchild", None)
    child.subNodes ::= grandchild
    getTestName("howdy there", grandchild) should be ("child child prefix grandchild howdy there")
  }
  "EngineSpec.getIndentationLevelForNode" should "return the indentation level for a test" in {
    val engine = new Engine("concurrentFunSuiteBundleMod", "FunSuite")
    import engine._
    val child = DescriptionBranch(Trunk, "child", Some("child prefix"))
    Trunk.subNodes ::= child
    val childTest = TestLeaf(Trunk, "child test", "child test", () => ())
    Trunk.subNodes ::= childTest
    val grandchild = DescriptionBranch(child, "grandchild", None)
    child.subNodes ::= grandchild
    val grandchildTest = TestLeaf(child, "grandchild test", "grandchild test", () => ())
    child.subNodes ::= grandchildTest
    val greatGrandchildTest = TestLeaf(grandchild, "great-grandchild test", "great-grandchild test", () => ())
    grandchild.subNodes ::= greatGrandchildTest
    Trunk.indentationLevel should be (0)
    child.indentationLevel should be (0)
    childTest.indentationLevel should be (0)
    grandchild.indentationLevel should be (1)
    grandchildTest.indentationLevel should be (1)
    greatGrandchildTest.indentationLevel should be (2)
  }
}
