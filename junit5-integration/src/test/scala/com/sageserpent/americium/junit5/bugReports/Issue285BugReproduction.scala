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
    def nElements(n: Int): Trials[Int] = api.integers(1, n)

    val elementRangesTrials = api.integers(1, 5).several[List[Int]]

    elementRangesTrials.withLimit(20).dynamicTests { elementRanges =>
      val trialsSequence = elementRanges.map(nElements)

      val cartesianProduct = api.sequences(trialsSequence)

      val cartesianProductSize = elementRanges.product

      val results = mutable.Set.empty[List[Int]]

      println("---------------------------------")

      cartesianProduct.withLimit(cartesianProductSize).supplyTo {
        productMember =>
          println(productMember)
          results += productMember
      }

      assert(cartesianProductSize == results.size)
    }
  }
}
