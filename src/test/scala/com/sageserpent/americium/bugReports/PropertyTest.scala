package com.sageserpent.americium.bugReports

import com.sageserpent.americium.Trials
import com.sageserpent.americium.bugReports.PropertyTest.positiveIntegers
import com.sageserpent.americium.junit5.*
import org.junit.jupiter.api.TestFactory

object PropertyTest {
  private val api = Trials.api

  /** Simple generator that produces positive integers.
    */
  private val positiveIntegers = api.integers(1, 100).withLimit(50)
}

/** Port of [[com.sageserpent.americium.java.bugReports.PropertyTest]] to Scala
  * to see whether the bug is Java-specific (see original class for description
  * of bug. It fails here, too.
  */

class PropertyTest {
  @TestFactory
  def positiveIntegersShouldBeGreaterThanZero(): DynamicTests =
    positiveIntegers.dynamicTests(value =>
      if (value <= 0)
        throw new AssertionError(
          "Expected positive integer but got: " + value
        )
    )

  @TestFactory
  def positiveIntegersShouldBeLessThan100(): DynamicTests =
    positiveIntegers.dynamicTests(value =>
      if (value > 100)
        throw new AssertionError("Expected value <= 100 but got: " + value)
    )
}
