package com.sageserpent.infrastructure.tests

import scala.util.Random
import scala.math

import junit.framework.TestCase

import com.sageserpent.infrastructure._


class RichRandomTests extends TestCase {
  def testCoverageOfIntegersUpToExclusiveUpperBound() {
    val random = new Random(29)

    val maximumUpperBound = 30

    for (upperBound <- 0 to maximumUpperBound) {
      val concreteRangeOfIntegers = 0 until upperBound
      
      val chosenItems = random.chooseSeveralOf(concreteRangeOfIntegers, upperBound)
      val expectedRange = 0 until upperBound
      assert(chosenItems.toSet == expectedRange.toSet)
    }
  }

  def testUniquenessOfIntegersProduced() {
    val random = new Random(678)

    val maximumUpperBound = 30

    for (upperBound <- 0 to maximumUpperBound) {
      val concreteRangeOfIntegers = 0 until upperBound
      
      val chosenItems = random.chooseSeveralOf(concreteRangeOfIntegers, upperBound)
      assert(upperBound == chosenItems.toSet.size)
      assert(upperBound == chosenItems.length)
    }
  }

  def testDistributionOfSuccessiveSequencesWithTheSameUpperBound() {
    val random = new Random(1)

    val maximumUpperBound = 30

    for (upperBound <- 0 to maximumUpperBound) {
      val concreteRangeOfIntegers = 0 until upperBound
      
      val numberOfTrials = 100000

      val itemToCountAndSumOfPositionsMap = Array.fill(upperBound) { 0 -> 0.0 }

      for {
        _ <- 1 to numberOfTrials
        (item, position) <- random.chooseSeveralOf(concreteRangeOfIntegers, upperBound).zipWithIndex
      } {
        val (count, sumOfPositions) = itemToCountAndSumOfPositionsMap(item)
        itemToCountAndSumOfPositionsMap(item) = 1 + count -> (position + sumOfPositions)
      }

      val toleranceEpsilon = 1e-1

      println("Upper bound: " + upperBound)

      println("itemToCountAndSumOfPositionsMap: " + itemToCountAndSumOfPositionsMap)

      assert(((0 until itemToCountAndSumOfPositionsMap.length) zip itemToCountAndSumOfPositionsMap).forall({
        case (item, (count, sumOfPositions)) => {
          val difference = (sumOfPositions / count - (0 + upperBound - 1) / 2.0)
          println("Item: " + item + " has a count: " + count + " and sum of positions: " + sumOfPositions + " leading to a signed difference in mean position of: " + difference)
          difference < toleranceEpsilon
        }
      }))
    }
  }

  def commonTestStructureForTestingOfChoosingSeveralItems(testOnSuperSetAndItemsChosenFromIt: (scala.collection.immutable.Set[Int], Seq[Int], Int) => Unit) {
    val random = new Random(1)

    for (inclusiveLowerBound <- 58 to 98)
      for (numberOfConsecutiveItems <- 1 to 50) {
        val superSet = (inclusiveLowerBound until inclusiveLowerBound + numberOfConsecutiveItems).toSet
        for (subsetSize <- 1 to numberOfConsecutiveItems)
          for (_ <- 1 to 10) {
            val chosenItems = random.chooseSeveralOf(superSet, subsetSize)
            testOnSuperSetAndItemsChosenFromIt(superSet, chosenItems, subsetSize)
          }
      }
  }

  def testThatAllItemsChosenBelongToTheSourceSequence() {
    commonTestStructureForTestingOfChoosingSeveralItems((superSet, chosenItems, _) => assert(chosenItems.toSet.subsetOf(superSet)))
  }

  def testThatTheNumberOfItemsRequestedIsHonouredIfPossible() {
    commonTestStructureForTestingOfChoosingSeveralItems((_, chosenItems, subsetSize) => assert(chosenItems.length == subsetSize))
  }

  def testThatUniqueItemsInTheSourceSequenceAreNotDuplicated() {
    commonTestStructureForTestingOfChoosingSeveralItems((_, chosenItems, _) => assert(chosenItems.toSet.size == chosenItems.length))
  }

  def testThatChoosingItemsRepeatedlyEventuallyCoversAllPermutations() {
    val empiricallyDeterminedMultiplicationFactorToEnsureCoverage = 70900.toDouble / BargainBasement.factorial(7)    
    
    val random = new Random(1)

    for (inclusiveLowerBound <- 58 to 98)
      for (numberOfConsecutiveItems <- 1 to 7) {
        val superSet = (inclusiveLowerBound until inclusiveLowerBound + numberOfConsecutiveItems).toSet
        for (subsetSize <- 1 to numberOfConsecutiveItems) {
          val expectedNumberOfPermutations = BargainBasement.numberOfPermutations(numberOfConsecutiveItems, subsetSize)
          val oversampledOutputs = for (_ <- 1 to scala.math.ceil(empiricallyDeterminedMultiplicationFactorToEnsureCoverage * expectedNumberOfPermutations).toInt) yield { random.chooseSeveralOf(superSet.toSeq, subsetSize) toList }
          println("Super set size: " + numberOfConsecutiveItems + ", picked subset of size: " + subsetSize + ", got: " + oversampledOutputs.toSet.size + " unique permutations.")
          assert(oversampledOutputs.toSet.size == expectedNumberOfPermutations)
        }
      }
  }
}