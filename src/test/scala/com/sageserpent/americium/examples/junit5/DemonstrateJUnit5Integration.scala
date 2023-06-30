package com.sageserpent.americium.examples.junit5
import com.google.common.collect.Iterators
import com.sageserpent.americium.Trials
import com.sageserpent.americium.junit5.*
import org.junit.jupiter.api.{DynamicTest, TestFactory}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit5.AssertionsForJUnit

import _root_.java.util.Iterator as JavaIterator
import scala.jdk.CollectionConverters.IteratorHasAsJava

class DemonstrateJUnit5Integration extends AssertionsForJUnit with Matchers {
  @TestFactory
  def dynamicTestsExample: JavaIterator[DynamicTest] = {
    val expectedNumberOfTestCases = 10
    val supplier =
      Trials.api.integers.withLimit(expectedNumberOfTestCases)
    var trialsCount: Int = 0

    val parameterisedDynamicTests =
      supplier.dynamicTests(testCase => {
        println(s"Test case #${trialsCount += 1} is $testCase")

      })
    val finalCheck = DynamicTest.dynamicTest(
      "Final Check",
      () => {
        trialsCount should be(expectedNumberOfTestCases)
      }
    )

    Iterators.concat(parameterisedDynamicTests, Iterator(finalCheck).asJava)
  }
}
