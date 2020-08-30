package com.sageserpent.americium

import com.sageserpent.americium.randomEnrichment._
import hedgehog.core.{DiscardCount, PropertyConfig, ShrinkLimit, SuccessCount}
import hedgehog.{Gen, Range}
import org.scalatest.{FlatSpec, Inspectors, Matchers}

import scala.util.Random

class RichRandomSplittingSpec
    extends FlatSpec
    with Matchers
    with Inspectors
    with HedgehogScalatestIntegration {
  implicit val configuration: PropertyConfig =
    PropertyConfig(SuccessCount(5000), DiscardCount(1000), ShrinkLimit(100))

  private val seedGenerator = Gen.long(Range.linear(0L, 100000L))

  private val itemGenerator = Gen.alphaNum

  private val itemsGenerator = Gen.list(itemGenerator, Range.linear(1, 100))

  private val numberOfRepeatsGenerator = Gen.int(Range.linear(1, 4))

  "Splitting into non empty pieces" should "yield no pieces at all when there are no items" in
    check(seedGenerator, numberOfRepeatsGenerator)((seed, numberOfRepeats) => {
      val random = new Random(seed)

      for (_ <- 1 to numberOfRepeats) {
        random.splitIntoNonEmptyPieces(Traversable.empty[Int]) shouldBe empty
      }
    })

  it should "yield non empty pieces when there is at least one item" in {
    check(seedGenerator, numberOfRepeatsGenerator, itemsGenerator) {
      case (seed, numberOfRepeats, items) =>
        val random = new Random(seed)
        for (_ <- 1 to numberOfRepeats) {
          random.splitIntoNonEmptyPieces(items).foreach(_ should not be empty)
        }
    }
  }

  it should "preserve all items and not introduce any others" in {
    check(seedGenerator, numberOfRepeatsGenerator, itemsGenerator) {
      case (seed, numberOfRepeats, items) =>
        val random = new Random(seed)
        for {
          _ <- 1 to numberOfRepeats
          expectedItemsAndTheirFrequencies = items groupBy identity mapValues (_.length)
          pieces                           = random.splitIntoNonEmptyPieces(items)
          actualItemsAndTheirFrequences    = pieces.flatten groupBy identity mapValues (_.length)
        } {
          actualItemsAndTheirFrequences should contain theSameElementsAs expectedItemsAndTheirFrequencies
        }
    }
  }

  it should "preserve the order of items" in {
    check(seedGenerator, numberOfRepeatsGenerator, itemsGenerator) {
      case (seed, numberOfRepeats, items) =>
        val random = new Random(seed)
        for {
          _ <- 1 to numberOfRepeats
          pieces        = random.splitIntoNonEmptyPieces(items)
          rejoinedItems = pieces flatten
        } {
          items should contain theSameElementsInOrderAs rejoinedItems
        }
    }
  }

  it should "sometimes give more than one piece back when there is more than one item" in {
    check(seedGenerator, itemsGenerator filter (1 < _.length)) {
      case (seed, items) =>
        val random = new Random(seed)

        forAtLeast(1,
                   for (_ <- 0 to 10)
                     yield random.splitIntoNonEmptyPieces(items))(pieces =>
          withClue(s"For the number of pieces in: $pieces")(
            pieces.length should be > 1))
    }
  }

  it should "eventually yield all possible splits" in {
    check(seedGenerator, Gen.int(Range.linear(1, 5))) {
      case (seed, numberOfItems) =>
        // Do not include the case of zero items in this test, it is tested elsewhere;
        // it also doesn't play well with the logic below of making a set of repeated split outcomes.
        whenever(0 < numberOfItems) {
          val items  = 1 to numberOfItems toSet
          val random = new Random(seed)
          // This is subtle - the best way to understand this is to visualise a bit string of length 'numberOfItems - 1'
          // - the bit string aligns off by one with all but the first item, eg:-
          // I1, I2, I3, I4
          //      1,  0,  1
          // Each one-bit corresponds to the decision to start a new piece, so the above example yields:-
          // [I1], [I2, I3], [I4]
          // The first item *has* to be included in a piece, so it has no corresponding bit.
          // Likewise, we don't need an off-by-one bit - the last item is either joined on
          // to the end of a bigger piece (0) or is in a piece by itself (1) - so only 'numberOfItems - 1'
          // bits are required. Now you see it.
          val expectedNumberOfPossibleSplits = (1 << numberOfItems) / 2 // Avoid passing -1 to the right hand of the left shift invocation.
          val setOfSets = (for (_ <- 0 to 20 * expectedNumberOfPossibleSplits)
            yield random.splitIntoNonEmptyPieces(items) toList).toSet
          setOfSets should have size expectedNumberOfPossibleSplits
        }
    }
  }

  it should "yield splits whose lengths only depend on the number of items" in {
    check(seedGenerator, itemsGenerator) {
      case (seed, originalItems) =>
        val transformedItems = originalItems.map(item => (1 + item).toString)

        val splitLengthsFromOriginalItems = {
          val random = new Random(seed)
          random.splitIntoNonEmptyPieces(originalItems).map(_.size)
        }
        val splitLengthsFromTransformedItems = {
          val random = new Random(seed)
          random.splitIntoNonEmptyPieces(transformedItems).map(_.size)
        }

        splitLengthsFromTransformedItems should contain theSameElementsInOrderAs splitLengthsFromOriginalItems
    }
  }
}
