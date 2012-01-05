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
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import scala.io.Source
import scala.io.Codec // for 2.8

object Helper {

  implicit val codec = Codec.default // for 2.8
  
  def translateShouldToMust(shouldLine: String): String = {
    val temp1 = shouldLine.replaceAll("<code>must</code>", "<code>I_WAS_must_ORIGINALLY</code>")
    val temp2 = temp1.replaceAll("<!-- PRESERVE -->should", " I_MUST_STAY_SHOULD")
    val temp3 = temp2.replaceAll(
      "<a href=\"MustMatchers.html\"><code>MustMatchers</code></a>",
      "<a href=\"I_WAS_Must_ORIGINALLYMatchers.html\"><code>I_WAS_Must_ORIGINALLYMatchers</code></a>"
    )
    val temp4 = temp3.replaceAll("should", "must")
    val temp5 = temp4.replaceAll("Should", "Must")
    val temp6 = temp5.replaceAll("I_WAS_must_ORIGINALLY", "should")
    val temp7 = temp6.replaceAll("I_MUST_STAY_SHOULD", "should")
    temp7.replaceAll("I_WAS_Must_ORIGINALLY", "Should")
  }
  def generateFile(srcFileName: String, targetFileName: String) {
    val matchersDir = new File("target/generated/src/main/scala/org/scalatest/matchers")
    matchersDir.mkdirs()
    val junitDir = new File("target/generated/src/main/scala/org/scalatest/junit")
    junitDir.mkdirs()
    val writer = new BufferedWriter(new FileWriter("target/generated/src/main/scala/org/scalatest/" + targetFileName))
    try {
      val shouldLines = Source.fromFile("src/main/scala/org/scalatest/" + srcFileName).getLines().toList // for 2.8
      for (shouldLine <- shouldLines) {
        val mustLine = translateShouldToMust(shouldLine)
        writer.write(mustLine)
        writer.newLine() // add for 2.8
      }
    }
    finally {
      writer.close()
    }
  }
}

import Helper._

object GenMustMatchers extends Application {
  generateFile("matchers/ShouldMatchers.scala", "matchers/MustMatchers.scala")
  generateFile("junit/ShouldMatchersForJUnit.scala", "junit/MustMatchersForJUnit.scala")
}

object GenMustMatchersTests extends Application {

  val matchersDir = new File("target/generated/src/test/scala/org/scalatest/matchers")
  matchersDir.mkdirs()
  val shouldFileNames = 
    List(
      "ShouldBehaveLikeSpec.scala",
      "ShouldContainElementSpec.scala",
      "ShouldContainKeySpec.scala",
      "ShouldContainValueSpec.scala",
      "ShouldEqualSpec.scala",
      "ShouldHavePropertiesSpec.scala",
      "ShouldLengthSpec.scala",
      "ShouldOrderedSpec.scala",
      "ShouldSizeSpec.scala",
      // "ShouldStackSpec.scala", now in examples
      // "ShouldStackFlatSpec.scala",
      "ShouldBeASymbolSpec.scala",
      "ShouldBeAnSymbolSpec.scala",
      "ShouldBeMatcherSpec.scala",
      "ShouldBePropertyMatcherSpec.scala",
      "ShouldBeSymbolSpec.scala",
      "ShouldEndWithRegexSpec.scala",
      "ShouldEndWithSubstringSpec.scala",
      "ShouldFullyMatchSpec.scala",
      "ShouldIncludeRegexSpec.scala",
      "ShouldIncludeSubstringSpec.scala",
      "ShouldLogicalMatcherExprSpec.scala",
      "ShouldMatcherSpec.scala",
      "ShouldPlusOrMinusSpec.scala",
      "ShouldSameInstanceAsSpec.scala",
      "ShouldStartWithRegexSpec.scala",
      "ShouldStartWithSubstringSpec.scala",
      "ShouldBeNullSpec.scala"
    )

  for (shouldFileName <- shouldFileNames) {

    val mustFileName = shouldFileName.replace("Should", "Must")
    val writer = new BufferedWriter(new FileWriter("target/generated/src/test/scala/org/scalatest/matchers/" + mustFileName))
    try {
      val shouldLines = Source.fromFile("src/test/scala/org/scalatest/matchers/" + shouldFileName).getLines().toList // for 2.8
      for (shouldLine <- shouldLines) {
        val mustLine = translateShouldToMust(shouldLine)
        writer.write(mustLine.toString)
        writer.newLine() // add for 2.8
      }
    }
    finally {
      writer.close()
    }
  }

  val junitDir = new File("target/generated/src/test/scala/org/scalatest/junit")
  junitDir.mkdirs()
  val writer = new BufferedWriter(new FileWriter("target/generated/src/test/scala/org/scalatest/junit/" + "MustMatchersForJUnitWordSpec.scala"))
  try {
    val shouldLines = Source.fromFile("src/test/scala/org/scalatest/junit/" + "ShouldMatchersForJUnitWordSpec.scala").getLines().toList // for 2.8
    for (shouldLine <- shouldLines) {
      val mustLine = translateShouldToMust(shouldLine)
      writer.write(mustLine.toString)
      writer.newLine() // add for 2.8
    }
  }
  finally {
    writer.close()
  }
}

