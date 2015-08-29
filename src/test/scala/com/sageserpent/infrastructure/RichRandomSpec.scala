package com.sageserpent.infrastructure

import org.scalacheck.{Arbitrary, Prop, Gen}
import org.scalatest.FlatSpec
import org.scalatest.prop.Checkers

import scala.util.Random

/**
 * Created by Gerard on 29/08/2015.
 */
class RichRandomSpec extends FlatSpec with Checkers {
  val seedGenerator = Arbitrary.arbitrary[Long]

  val itemGenerator = Arbitrary.arbitrary[Char]

  val itemsGenerator = Gen.listOf(itemGenerator) filter (!_.isEmpty)

  val numberOfRepeatsGenerator = Gen.choose(1, 4)

  "Splitting into non empty pieces" should "yield no pieces at all" in {
    check(Prop.forAll(seedGenerator, numberOfRepeatsGenerator)((seed, numberOfRepeats) => {
      val random = new Random(seed)

      (for (_ <- 1 to numberOfRepeats) yield random.splitIntoNonEmptyPieces(Traversable.empty[Int]).isEmpty).forall(identity)

    }
    ))
  }

  it should "yield non empty pieces when there is more than one item" in {
    check(Prop.forAll(seedGenerator, numberOfRepeatsGenerator, itemsGenerator) { case (seed, numberOfRepeats, items) => {
      val random = new Random(seed)
      val checks = for (_ <- 1 to numberOfRepeats) yield random.splitIntoNonEmptyPieces(items)
      checks.forall(_.forall(!_.isEmpty))
    }
    })
  }

  it should "preserve all items and not introduce any others" in {
    check(Prop.forAll(seedGenerator, numberOfRepeatsGenerator, itemsGenerator) { case (seed, numberOfRepeats, items) => {
      val random = new Random(seed)
      val checks = for {_ <- 1 to numberOfRepeats
                        expectedItemsAndTheirFrequencies = items groupBy identity mapValues (_.length)
                        pieces = random.splitIntoNonEmptyPieces(items)
                        actualItemsAndTheirFrequences = pieces flatMap identity groupBy identity mapValues (_.length)
      } yield (expectedItemsAndTheirFrequencies, actualItemsAndTheirFrequences)
      checks.forall { case (expectedItemsAndTheirFrequencies, actualItemsAndTheirFrequences) => expectedItemsAndTheirFrequencies === actualItemsAndTheirFrequences }
    }
    })
  }

  it should "preserve the order of items" in {
    check(Prop.forAll(seedGenerator, numberOfRepeatsGenerator, itemsGenerator) { case (seed, numberOfRepeats, items) => {
      val random = new Random(seed)
      val checks = for {_ <- 1 to numberOfRepeats
                        pieces = random.splitIntoNonEmptyPieces(items)
                        rejoinedItems = pieces flatMap identity
      } yield rejoinedItems
      checks.forall(rejoinedItems => items === rejoinedItems)
    }
    })
  }

  it should "sometimes give more than one piece back when there is more than one item" in {
    check(Prop.forAll(seedGenerator, itemsGenerator filter (1 < _.length)) { case (seed, items) => {
      val random = new Random(seed)
      val checks = for (_ <- 0 to 10) yield random.splitIntoNonEmptyPieces(items)
      checks.exists(1 < _.length)
    }
    })
  }

  it should "eventually yield all possible splits" in {
    check(Prop.forAll(seedGenerator, Gen.choose(1, 5)) { case (seed, numberOfItems) => {
      val items = 1 to numberOfItems toSet
      val random = new Random(seed)
      val sizeOfPowerSet = 1 << numberOfItems
      val setOfSets = (for (_ <- 0 to 10 * sizeOfPowerSet) yield random.splitIntoNonEmptyPieces(items) toSet).toSet
      sizeOfPowerSet === 1 + setOfSets.size // Add one - this accounts for the fact that we have to choose at least one piece from a non-empty group of items, the powerset includes the empty choice case.
    }
    })
  }
}
