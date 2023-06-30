package com.sageserpent.americium.examples.junit5

import com.google.common.collect.Iterators
import com.sageserpent.americium.Trials
import com.sageserpent.americium.junit5.*
import org.junit.jupiter.api.{Disabled, DynamicTest, TestFactory}
import utest.*

import java.util.Iterator as JavaIterator
import scala.jdk.CollectionConverters.IteratorHasAsJava

class DemonstrateJUnit5Integration {
  @Disabled
  @TestFactory
  def dynamicTestsExample: JavaIterator[DynamicTest] = {
    val expectedNumberOfTestCases = 15
    val supplier =
      Trials.api.integers.withLimit(expectedNumberOfTestCases)
    var trialsCount: Int = 0

    val parameterisedDynamicTests =
      supplier.dynamicTests(testCase => {
        trialsCount += 1

        assert(0 != testCase % 3)

        println(s"Test case #$trialsCount is $testCase")

      })

    // TODO: the final check is only run when the test is modified to not
    // artificially fail. Should this be considered a bug or a feature?
    val finalCheck = DynamicTest.dynamicTest(
      "Final Check",
      () => {
        assert(trialsCount == expectedNumberOfTestCases)
      }
    )

    Iterators.concat(parameterisedDynamicTests, Iterator(finalCheck).asJava)
  }
}
