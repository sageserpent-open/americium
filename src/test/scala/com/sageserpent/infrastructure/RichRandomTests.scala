package com.sageserpent.infrastructure

import org.junit.Test
import scala.util.Random
import scala.math
import junit.framework.TestCase
import com.sageserpent.infrastructure._
import scala.collection.mutable.Map

class RichRandomTests extends TestCase {
  @Test
  def testCoverageOfIntegersUpToExclusiveUpperBound() {
    val random = new Random(29)

    val maximumUpperBound = 30

    for (upperBound <- 0 to maximumUpperBound) {
      val expectedRange = 0 until upperBound

      val chosenItems = random.chooseSeveralOf(expectedRange, upperBound)
      assert(chosenItems.toSet == expectedRange.toSet)

      val chosenItemsViaAnotherWay = random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(upperBound)
      assert(chosenItemsViaAnotherWay.toSet == expectedRange.toSet)
    }
  }

  @Test
  def testUniquenessOfIntegersProduced() {
    val random = new Random(678)

    val maximumUpperBound = 30

    for (upperBound <- 0 to maximumUpperBound) {
      val concreteRangeOfIntegers = 0 until upperBound

      val chosenItems = random.chooseSeveralOf(concreteRangeOfIntegers, upperBound)
      assert(upperBound == chosenItems.toSet.size)
      assert(upperBound == chosenItems.length)

      val chosenItemsViaAnotherWay = random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(upperBound)
      assert(upperBound == chosenItemsViaAnotherWay.toSet.size)
      assert(upperBound == chosenItemsViaAnotherWay.length)
    }
  }

  private def sampleDistributions(upperBound: Int, sampleSize: Int, buildRandomSequenceOfDistinctIntegersOfSize: Int => Seq[Int]) {

    val numberOfTrials = BargainBasement.numberOfCombinations(upperBound, sampleSize) * 1000

    println("Number of trials: %d, upperBound: %d, sampleSize: %d".format(numberOfTrials, upperBound, sampleSize))

    val sampleToCountMap = Map.empty[Set[Int], Int].withDefaultValue(0)

    val itemToCountAndSumOfPositionsMap = Map.empty[Int, (Int, Double)].withDefaultValue(0 -> 0.0)

    for {
      _ <- 1 to numberOfTrials
    } {
      val sample = buildRandomSequenceOfDistinctIntegersOfSize(sampleSize).toList

      val sampleAsSet = sample.toSet

      sampleToCountMap(sampleAsSet) = 1 + sampleToCountMap(sampleAsSet)

      for { (item, position) <- sample.zipWithIndex } {
        val (count, sumOfPositions) = itemToCountAndSumOfPositionsMap(item)
        itemToCountAndSumOfPositionsMap(item) = 1 + count -> (position + sumOfPositions)
      }
    }

    val numberOfDistinctSamplesObtained = sampleToCountMap.size

    assert(sampleToCountMap.values.filter({ count =>
      val expectedCount = 1.0 * numberOfTrials / numberOfDistinctSamplesObtained
      val tolerance = 1e-1
      Math.abs(count - expectedCount) <= tolerance * expectedCount
    }).size >= 9e-1 * sampleToCountMap.size)

    assert(upperBound == itemToCountAndSumOfPositionsMap.size)

    assert(itemToCountAndSumOfPositionsMap.keys.forall({
      item =>
        {
          0 <= item && upperBound > item
        }
    }))

    assert(itemToCountAndSumOfPositionsMap.values.filter({
      case (count, sumOfPositions) => {
        val meanPosition = sumOfPositions / count
        val expectedMeanPosition = (sampleSize - 1) / 2.0
        val difference = Math.abs(meanPosition - expectedMeanPosition)
        val tolerance = 1e-1
        difference <= tolerance * expectedMeanPosition
      }
    }).size >= 9e-1 * itemToCountAndSumOfPositionsMap.size)
  }

  @Test
  def testDistributionOfSuccessiveSequencesWithTheSameUpperBound() {
    val random = new Random(1)

    val maximumUpperBound = 20

    for (upperBound <- 1 to maximumUpperBound) yield {
      val concreteRangeOfIntegers = 0 until upperBound

      sampleDistributions(upperBound, 1, { _ => List(random.chooseOneOf(concreteRangeOfIntegers)) })

      val sampleSizes = Set(1, Math.min(1 + random.nextInt(upperBound), upperBound), upperBound)

      for (sampleSize <- sampleSizes) {

        sampleDistributions(upperBound, sampleSize, { random.chooseSeveralOf(concreteRangeOfIntegers, _) })

        sampleDistributions(upperBound, sampleSize, { random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(upperBound).take(_) })
      }
    }
  }

  def anotherWayOfChoosingSeveralOf(random: Random, candidates: Traversable[Int], numberToChoose: Int) = {
    val candidatesWithRandomAccess = candidates.toArray

    for (index <- random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(candidatesWithRandomAccess.size) take numberToChoose)
      yield candidatesWithRandomAccess(index)
  }

  def commonTestStructureForTestingOfChoosingSeveralItems(testOnSuperSetAndItemsChosenFromIt: (scala.collection.immutable.Set[Int], Seq[Int], Int) => Unit) {
    val random = new Random(1)

    for (inclusiveLowerBound <- 58 to 98)
      for (numberOfConsecutiveItems <- 1 to 50) {
        val superSet = (inclusiveLowerBound until inclusiveLowerBound + numberOfConsecutiveItems).toSet
        val chosenItem = random.chooseOneOf(superSet)
        testOnSuperSetAndItemsChosenFromIt(superSet, List(chosenItem), 1)
        for (subsetSize <- 1 to numberOfConsecutiveItems)
          for (_ <- 1 to 10) {
            val chosenItems = random.chooseSeveralOf(superSet, subsetSize)
            testOnSuperSetAndItemsChosenFromIt(superSet, chosenItems, subsetSize)

            val anotherBunchOfChosenItems = anotherWayOfChoosingSeveralOf(random, superSet, subsetSize)
            testOnSuperSetAndItemsChosenFromIt(superSet, chosenItems, subsetSize)
          }
      }
  }

  @Test
  def testThatAllItemsChosenBelongToTheSourceSequence() {
    commonTestStructureForTestingOfChoosingSeveralItems((superSet, chosenItems, _) => assert(chosenItems.toSet.subsetOf(superSet)))
  }

  @Test
  def testThatTheNumberOfItemsRequestedIsHonouredIfPossible() {
    commonTestStructureForTestingOfChoosingSeveralItems((_, chosenItems, subsetSize) => assert(chosenItems.length == subsetSize))
  }

  @Test
  def testThatUniqueItemsInTheSourceSequenceAreNotDuplicated() {
    commonTestStructureForTestingOfChoosingSeveralItems((_, chosenItems, _) => assert(chosenItems.toSet.size == chosenItems.length))
  }

  @Test
  def testThatChoosingItemsRepeatedlyEventuallyCoversAllPermutations() {
    val empiricallyDeterminedMultiplicationFactorToEnsureCoverage = 79200.toDouble / BargainBasement.factorial(7)

    val random = new Random(1)

    for (inclusiveLowerBound <- 58 to 98)
      for (numberOfConsecutiveItems <- 1 to 7) {
        val superSet = (inclusiveLowerBound until inclusiveLowerBound + numberOfConsecutiveItems).toSet

        val expectedNumberOfPermutations = BargainBasement.numberOfPermutations(numberOfConsecutiveItems, 1)

        val oversampledOutputs = for (_ <- 1 to scala.math.ceil(empiricallyDeterminedMultiplicationFactorToEnsureCoverage * expectedNumberOfPermutations).toInt) yield { random.chooseOneOf(superSet.toSeq) }
        assert(oversampledOutputs.toSet.size == expectedNumberOfPermutations)

        for (subsetSize <- 1 to numberOfConsecutiveItems) {
          val expectedNumberOfPermutations = BargainBasement.numberOfPermutations(numberOfConsecutiveItems, subsetSize)

          val oversampledOutputs = for (_ <- 1 to scala.math.ceil(empiricallyDeterminedMultiplicationFactorToEnsureCoverage * expectedNumberOfPermutations).toInt) yield { random.chooseSeveralOf(superSet.toSeq, subsetSize) toList }
          assert(oversampledOutputs.toSet.size == expectedNumberOfPermutations)

          val oversampledOutputsViaAnotherWay = for (_ <- 1 to scala.math.ceil(empiricallyDeterminedMultiplicationFactorToEnsureCoverage * expectedNumberOfPermutations).toInt) yield { anotherWayOfChoosingSeveralOf(random, superSet.toSeq, subsetSize) toList }
          assert(oversampledOutputsViaAnotherWay.toSet.size == expectedNumberOfPermutations)
        }
      }
  }

  @Test
  def testPig0GetInTheTrough() {
    pig(64000)
  }

  @Test
  def testPig1() {
    pig(1000)
  }

  @Test
  def testPig2() {
    pig(2000)
  }

  @Test
  def testPig3() {
    pig(4000)
  }

  @Test
  def testPig4() {
    pig(8000)
  }

  @Test
  def testPig5() {
    pig(16000)
  }

  @Test
  def testPig6() {
    pig(32000)
  }

  @Test
  def testPig7() {
    pig(64000)
  }

  @Test
  def testPig8() {
    pig(50000)
  }

  @Test
  def testPig9() {
    pig(100000)
  }

  @Test
  def testPig10() {
    pig(200000)
  }

  @Test
  def testPig11() {
    pig(500000)
  }

  @Test
  def testPig12() {
    pig(1000000)
  }

  private def pig(maximumUpperBound: Int) {
    val random = new Random(678)
    val concreteRangeOfIntegers = 0 until maximumUpperBound

    for (_ <- 1 to 10) {
      val chosenItems = random.chooseSeveralOf(concreteRangeOfIntegers, maximumUpperBound)
      for (chosenItem <- chosenItems) {}
    }
  }

  @Test
  def testJustAFew() {
    val random = new Random(678)
    val maximumUpperBound = 1000000
    val concreteRangeOfIntegers = 0 until maximumUpperBound

    val chosenItem = random.chooseOneOf(concreteRangeOfIntegers)

    for (_ <- 1 to 10) {
      val sampleSize = maximumUpperBound / 2
      val chosenItems = random.chooseSeveralOf(concreteRangeOfIntegers, sampleSize)
      for (chosenItem <- chosenItems) {}
    }
  }
}