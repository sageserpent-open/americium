package com.sageserpent.americium.examples.junit5
/* import com.sageserpent.americium.Trials import
 * com.sageserpent.americium.junit5.* import org.junit.jupiter.api.{DynamicTest,
 * TestFactory} import org.scalatest.matchers.should.Matchers import
 * org.scalatestplus.junit5.AssertionsForJUnit
 *
 * class DemonstrateJUnit5Integration extends AssertionsForJUnit with Matchers {
 * @TestFactory def dynamicTestsExample: Array[DynamicTest] = { val
 * expectedNumberOfTestCases = 10 val supplier =
 * Trials.api.integers.withLimit(expectedNumberOfTestCases) var trialsCount: Int
 * = 0
 *
 * val parameterisedDynamicTests =
 * supplier.dynamicTests(testCase => { trialsCount += 1
 *
 * println(s"Test case #$trialsCount is $testCase")
 *
 * }) val finalCheck = DynamicTest.dynamicTest( "Final Check", () => {
 * trialsCount should be(expectedNumberOfTestCases) } )
 *
 * parameterisedDynamicTests :+ finalCheck } } */
