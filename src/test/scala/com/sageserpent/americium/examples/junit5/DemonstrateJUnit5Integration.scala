package com.sageserpent.americium.examples.junit5

import com.eed3si9n.expecty.Expecty.assert
import com.google.common.collect.Iterators
import com.sageserpent.americium.Trials.api
import com.sageserpent.americium.junit5.*
import org.junit.jupiter.api.{Disabled, DynamicTest, TestFactory}

import scala.jdk.CollectionConverters.IteratorHasAsJava

class DemonstrateJUnit5Integration {
  @Disabled
  @TestFactory
  def dynamicTestsExample: DynamicTests = {
    val expectedNumberOfTestCases = 15
    val supplier =
      api.integers.withLimit(expectedNumberOfTestCases)
    var trialsCount: Int = 0

    val parameterisedDynamicTests =
      supplier.dynamicTests(testCase => {
        trialsCount += 1

        assert(0 != testCase % 3)

        println(s"Test case #$trialsCount is $testCase")

      })

    // The final check is only run when the test is modified to not artificially
    // fail. Should this be considered a bug or a feature? It is currently
    // consider to be a feature - we expect Americium to finally throw a
    // `TrialsException` after it completes shrinkage, and that will cause an
    // overall failure *prior* to the final check. In effect, the final check is
    // conditional on all the previous dynamic tests having passed.
    val finalCheck = DynamicTest.dynamicTest(
      "Final Check",
      () => {
        assert(trialsCount == expectedNumberOfTestCases)
      }
    )

    Iterators.concat(parameterisedDynamicTests, Iterator(finalCheck).asJava)
  }

  @Disabled
  @TestFactory
  def dynamicTestsExampleUsingAGangOfTwo(): DynamicTests = {
    val expectedNumberOfTestCases = 15
    val supplier =
      (api.integers and api.strings).withLimit(expectedNumberOfTestCases)
    var trialsCount: Int = 0

    val parameterisedDynamicTests =
      supplier.dynamicTests((partOne, partTwo) => {
        trialsCount += 1

        assert(0 != partOne % 3)

        println(s"Test case #$trialsCount is $partOne, $partTwo")

      })

    // The final check is only run when the test is modified to not artificially
    // fail. Should this be considered a bug or a feature? It is currently
    // consider to be a feature - we expect Americium to finally throw a
    // `TrialsException` after it completes shrinkage, and that will cause an
    // overall failure *prior* to the final check. In effect, the final check is
    // conditional on all the previous dynamic tests having passed.
    val finalCheck = DynamicTest.dynamicTest(
      "Final Check",
      () => {
        assert(trialsCount == expectedNumberOfTestCases)
      }
    )

    Iterators.concat(parameterisedDynamicTests, Iterator(finalCheck).asJava)
  }
}
