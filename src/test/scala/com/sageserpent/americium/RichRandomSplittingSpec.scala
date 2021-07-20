package com.sageserpent.americium

import com.sageserpent.americium.randomEnrichment._
import org.scalatest.Inspectors
import org.scalatest.enablers.Aggregating._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.whenever

import scala.util.Random

class RichRandomSplittingSpec
    extends AnyFlatSpec
    with Matchers
    with Inspectors {
  val api = Trials.api

  val seedTrials: Trials[Long] = api.longs

  val itemTrials: Trials[Char] = api.characters

  val itemsTrials: Trials[List[Char]] = itemTrials.lists

  val numberOfRepeatsTrials: Trials[Int] = api.choose(1, 4)

  "Splitting into non empty pieces" should "yield no pieces at all when there are no items" in
    (seedTrials, numberOfRepeatsTrials)
      .withLimit(100)
      .supplyTo((seed, numberOfRepeats) => {
        val random = new Random(seed)

        for (_ <- 1 to numberOfRepeats) {
          random.splitIntoNonEmptyPieces(Iterable.empty[Int]) shouldBe empty
        }
      })

  it should "yield non empty pieces when there is at least one item" in {
    (seedTrials, numberOfRepeatsTrials, itemsTrials)
      .withLimit(100)
      .supplyTo { (seed, numberOfRepeats, items) =>
        {
          val random = new Random(seed)
          for (_ <- 1 to numberOfRepeats) {
            random.splitIntoNonEmptyPieces(items).foreach(_ should not be empty)
          }
        }
      }
  }

  it should "preserve all items and not introduce any others" in {
    (seedTrials, numberOfRepeatsTrials, itemsTrials)
      .withLimit(100)
      .supplyTo { (seed, numberOfRepeats, items) =>
        val random = new Random(seed)
        for {
          _ <- 1 to numberOfRepeats
          expectedItemsAndTheirFrequencies = (items groupBy identity).view
            .mapValues(_.length)
          pieces = random.splitIntoNonEmptyPieces(items)
          actualItemsAndTheirFrequences = (pieces.flatten groupBy identity).view
            .mapValues(_.length)
        } {
          actualItemsAndTheirFrequences.toSeq should contain theSameElementsAs expectedItemsAndTheirFrequencies.toSeq
        }
      }
  }

  it should "preserve the order of items" in {
    (seedTrials, numberOfRepeatsTrials, itemsTrials)
      .withLimit(100)
      .supplyTo { (seed, numberOfRepeats, items) =>
        val random = new Random(seed)
        for {
          _ <- 1 to numberOfRepeats
          pieces        = random.splitIntoNonEmptyPieces(items)
          rejoinedItems = pieces.flatten
        } {
          items should contain theSameElementsInOrderAs rejoinedItems
        }
      }
  }

  it should "sometimes give more than one piece back when there is more than one item" in {
    (seedTrials, itemsTrials filter (1 < _.length))
      .withLimit(100)
      .supplyTo { (seed, items) =>
        val random = new Random(seed)

        forAtLeast(
          1,
          for (_ <- 0 to 10)
            yield random.splitIntoNonEmptyPieces(items)
        )(pieces =>
          withClue(s"For the number of pieces in: $pieces")(
            pieces.length should be > 1
          )
        )
      }
  }

  it should "eventually yield all possible splits" in {
    (seedTrials, api.choose(1, 5))
      .withLimit(100)
      .supplyTo { (seed, numberOfItems) =>
        // Do not include the case of zero items in this test, it is tested elsewhere;
        // it also doesn't play well with the logic below of making a set of repeated split outcomes.
        whenever(0 < numberOfItems) {
          val items  = (1 to numberOfItems).toSet
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
          val expectedNumberOfPossibleSplits =
            (1 << numberOfItems) / 2 // Avoid passing -1 to the right hand of the left shift invocation.
          val setOfSets = (for (_ <- 0 to 20 * expectedNumberOfPossibleSplits)
            yield random.splitIntoNonEmptyPieces(items).toList).toSet
          setOfSets should have size expectedNumberOfPossibleSplits
        }
      }
  }

  it should "yield splits whose lengths only depend on the number of items" in {
    (seedTrials, itemsTrials)
      .withLimit(100)
      .supplyTo { (seed, originalItems) =>
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
