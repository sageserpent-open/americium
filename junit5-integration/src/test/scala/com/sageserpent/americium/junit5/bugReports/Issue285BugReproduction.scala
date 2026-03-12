package com.sageserpent.americium.junit5.bugReports
import com.eed3si9n.expecty.Expecty.assert
import com.google.common.collect.ImmutableList
import com.sageserpent.americium.Trials
import com.sageserpent.americium.Trials.api
import com.sageserpent.americium.java.Trials as JavaTrials
import com.sageserpent.americium.java.Trials.api as javaApi
import com.sageserpent.americium.junit5.*
import org.junit.jupiter.api.TestFactory

import scala.collection.mutable
import scala.jdk.CollectionConverters.SeqHasAsJava

class Issue285BugReproduction {
  @TestFactory
  def combiningTrialsViaSequenceYieldsTheCartesianProductOfTheirElementsViaScalaApi()
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

  @TestFactory
  def combiningTrialsViaSequenceYieldsTheCartesianProductOfTheirElementsViaJavaApi()
      : DynamicTests = {
    // Outer trials instance - this is for the *test*.
    val elementMaximaTrials = api.integers(1, 5).several[List[Int]]

    elementMaximaTrials.withLimit(20).dynamicTests { elementMaxima =>
      def inRangeFromOneToMaximum(maximum: Int): JavaTrials[Int] =
        javaApi.choose((1 to maximum).asJava)

      val trialsSequence = elementMaxima.map(inRangeFromOneToMaximum).asJava

      // Inner trials instance - this is the *SUT* itself; we rely on
      // `Trials.supplyTo` being re-entrant.
      val cartesianProduct = javaApi.immutableLists(trialsSequence)

      val cartesianProductSize = elementMaxima.product

      val results = mutable.Set.empty[ImmutableList[Int]]

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
