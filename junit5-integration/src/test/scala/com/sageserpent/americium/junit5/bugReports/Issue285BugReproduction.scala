package com.sageserpent.americium.junit5.bugReports
import com.eed3si9n.expecty.Expecty.assert
import com.sageserpent.americium.Trials
import com.sageserpent.americium.Trials.api
import com.sageserpent.americium.junit5.*
import org.junit.jupiter.api.TestFactory

import scala.collection.mutable

class Issue285BugReproduction {
  @TestFactory
  def combiningTrialsViaSequenceYieldsTheCartesianProductOfTheirElements()
      : DynamicTests = {
    // Outer trials instance - this is for the *test*.
    val elementMaximaTrials = api.integers(1, 5).several[List[Int]]

    elementMaximaTrials.withLimit(20).dynamicTests { elementMaxima =>
      def inRangeFromOneToMaximum(maximum: Int): Trials[Int] =
        api.choose(1 to maximum)

      val trialsSequence = elementMaxima.map(inRangeFromOneToMaximum)

      // Inner trials instance - this is the *SUT* itself; we rely on
      // `Trials.supplyTo` being re-entrant.
      val cartesianProduct = api.sequences(trialsSequence)

      val cartesianProductSize = elementMaxima.product

      val results = mutable.Set.empty[List[Int]]

      println("---------------------------------")

      cartesianProduct
        .withLimit(cartesianProductSize)
        .supplyTo { productMember =>
          println(productMember)
          results += productMember
        }

      assert(cartesianProductSize == results.size)
    }
  }
}
