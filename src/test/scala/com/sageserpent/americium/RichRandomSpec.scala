package com.sageserpent.americium

import randomEnrichment._
import org.scalacheck.{Arbitrary, Prop, Gen}
import org.scalatest.FlatSpec
import org.scalatest.prop.Checkers

import scala.util.Random

import Prop.BooleanOperators


class RichRandomSpec extends FlatSpec with Checkers {
  val seedGenerator = Arbitrary.arbitrary[Long]

  val itemGenerator = Arbitrary.arbitrary[Char]

  val itemsGenerator = Gen.nonEmptyListOf(itemGenerator)

  val numberOfRepeatsGenerator = Gen.choose(1, 4)

  "Splitting into non empty pieces" should "yield no pieces at all when there are no items" in {
    check(Prop.forAll(seedGenerator, numberOfRepeatsGenerator)(
      (seed, numberOfRepeats) => {
        val random = new Random(seed)

        Prop.all(
          (for (_ <- 1 to numberOfRepeats)
            yield random.splitIntoNonEmptyPieces(Traversable.empty[Int])) map (
              pieces => pieces.isEmpty :| s"${pieces} should be empty"): _*)

      }))
  }

  it should "yield non empty pieces when there is at least one item" in {
    check(Prop.forAll(seedGenerator, numberOfRepeatsGenerator, itemsGenerator) {
      case (seed, numberOfRepeats, items) =>
        val random = new Random(seed)
        Prop.all((for (_ <- 1 to numberOfRepeats)
          yield random.splitIntoNonEmptyPieces(items)) map (pieces =>
          pieces.forall(_.nonEmpty) :| s"${pieces} should be composed of non-empty pieces"): _*)
    })
  }

  it should "preserve all items and not introduce any others" in {
    check(Prop.forAll(seedGenerator, numberOfRepeatsGenerator, itemsGenerator) {
      case (seed, numberOfRepeats, items) =>
        val random = new Random(seed)
        Prop.all((for {
          _ <- 1 to numberOfRepeats
          expectedItemsAndTheirFrequencies = items groupBy identity mapValues (_.length)
          pieces                           = random.splitIntoNonEmptyPieces(items)
          actualItemsAndTheirFrequences    = pieces flatMap identity groupBy identity mapValues (_.length)
        } yield
          (expectedItemsAndTheirFrequencies, actualItemsAndTheirFrequences))
          .map {
            case (expectedItemsAndTheirFrequencies,
                  actualItemsAndTheirFrequences) => {
              (expectedItemsAndTheirFrequencies === actualItemsAndTheirFrequences) :| s"${expectedItemsAndTheirFrequencies} === ${actualItemsAndTheirFrequences}"
            }
          }: _*)
    })
  }

  it should "preserve the order of items" in {
    check(Prop.forAll(seedGenerator, numberOfRepeatsGenerator, itemsGenerator) {
      case (seed, numberOfRepeats, items) =>
        val random = new Random(seed)
        Prop.all((for {
          _ <- 1 to numberOfRepeats
          pieces        = random.splitIntoNonEmptyPieces(items)
          rejoinedItems = pieces flatten
        } yield rejoinedItems).map(rejoinedItems =>
          (items === rejoinedItems) :| s"${items} === ${rejoinedItems}"): _*)
    })
  }

  it should "sometimes give more than one piece back when there is more than one item" in {
    check(Prop.forAll(seedGenerator, itemsGenerator filter (1 < _.length)) {
      case (seed, items) =>
        val random = new Random(seed)
        Prop.atLeastOne(
          (for (_ <- 0 to 10)
            yield random.splitIntoNonEmptyPieces(items)).map(pieces =>
            (1 < pieces.length) :| s"1 < ${pieces}.length}"): _*)
    })
  }

  it should "eventually yield all possible splits" in {
    check(Prop.forAll(seedGenerator, Gen.choose(1, 5)) {
      case (seed, numberOfItems) =>
        // Do not include the case of zero items in this test, it is tested elsewhere;
        // it also doesn't play well with the logic below of making a set of repeated split outcomes.
        (0 < numberOfItems) ==> {
          val items = 1 to numberOfItems toSet
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
          (expectedNumberOfPossibleSplits === setOfSets.size) :| s"${expectedNumberOfPossibleSplits} === ${setOfSets}.size"
        }
    })
  }

  it should "yield splits whose lengths only depend on the number of items" in {
    check(Prop.forAll(seedGenerator, itemsGenerator) {
      case (seed, originalItems) =>
        val transformedItems = originalItems.map(item => (1 + item).toString)

        val splitLengthsFromOriginalItems = {
          val random         = new Random(seed)
          random.splitIntoNonEmptyPieces(originalItems).map(_.size)
        }
        val splitLengthsFromTransformedItems = {
          val random         = new Random(seed)
          random.splitIntoNonEmptyPieces(transformedItems).map(_.size)
        }

        (splitLengthsFromOriginalItems === splitLengthsFromTransformedItems) :| s"$splitLengthsFromOriginalItems === $splitLengthsFromTransformedItems"
    })
  }
}
