package com.sageserpent.infrastructure.tests

import scala.util.Random

import org.junit.runner.RunWith
import org.scalatest.Suite

import com.sageserpent.infrastructure._

@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RichRandomTests extends Suite {
  def anUpperBoundOfZeroYieldsAnEmptyCollection() {
    val random = new Random(34)

    for (_ <- 1 to 10) assert(List.empty == random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(0))
  }

  def testCoverageOfIntegersUpToExclusiveUpperBound() {
    val random = new Random(29)

    val maximumUpperBound = 30

    for (upperBound <- 0 until maximumUpperBound) {
      val chosenItems = random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(upperBound)
      val expectedRange = 0 until upperBound
      assert(chosenItems.toSet == expectedRange.toSet)
    }
  }

  def testUniquenessOfIntegersProduced() {
    val random = new Random(678)

    val maximumUpperBound = 30

    for (upperBound <- 0 until maximumUpperBound) {
      val chosenItems = random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(upperBound)
      assert(upperBound == chosenItems.toSet.size)
    }
  }

  def testDistributionOfSuccessiveSequencesWithTheSameUpperBound() {

    val random = new Random(1)

    val maximumUpperBound = 30

    for (upperBound <- 0 until maximumUpperBound) {
      val numberOfTrials = 100000

      val emptyItemToCountAndSumOfPositionsMap: Map[Int, (Int, Double)] = Map.empty withDefaultValue (0 -> 0.0)

      val positionsAndItemsTakenOverAllTrials = for {
        _ <- 1 to numberOfTrials
        (position, item) <- 0 until upperBound zip random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(upperBound)
      } yield (position, item)

      val itemToCountAndSumOfPositionsMap = (emptyItemToCountAndSumOfPositionsMap /: positionsAndItemsTakenOverAllTrials) {
        case (itemToCountAndSumOfPositionsMap, (position, item)) => {
          val (count, sumOfPositions) = itemToCountAndSumOfPositionsMap(item)
          itemToCountAndSumOfPositionsMap + (item -> (1 + count -> (position + sumOfPositions)))
        }
      }

      val toleranceEpsilon = 1e-1

      println("Upper bound: " + upperBound)

      println("itemToCountAndSumOfPositionsMap: " + itemToCountAndSumOfPositionsMap)

      assert(itemToCountAndSumOfPositionsMap.forall({
        case (_, (count, sumOfPositions)) => {
          val difference = (sumOfPositions / count - (0 + upperBound - 1) / 2.0).abs
          println((count, sumOfPositions) + " leads to a difference of: " + difference)
          difference < toleranceEpsilon
        }
      }))
    }
  }
}