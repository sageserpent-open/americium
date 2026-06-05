package com.sageserpent.americium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SplitsIntoPiecesTest extends AnyFlatSpec with Matchers {
  val api = Trials.api

  private val itemsTrials: Trials[Vector[Int]] =
    api.integers.several[Vector[Int]]

  "splitsIntoPieces" should "yield pieces that concatenate to the original items" in {
    val testCaseTrials = for {
      items <- itemsTrials
      overshootToForceSomeMandatoryEmptyPieces = 1 max items.size / 5
      numberOfPieces <- api.integers(
        1,
        overshootToForceSomeMandatoryEmptyPieces + items.size
      )
      pieces <- api.splitsIntoPieces(items, numberOfPieces)
    } yield (items, numberOfPieces, pieces)

    testCaseTrials.withLimit(100).supplyTo {
      case (items, numberOfPieces, pieces) =>
        pieces.size should be(numberOfPieces)
        pieces.flatten should be(items)
    }
  }

  it should "preserve the container type of the pieces" in {
    val vectorItems = Vector(1, 2, 3)
    api.splitsIntoPieces(vectorItems, 2).withLimit(1).supplyTo { pieces =>
      pieces.head shouldBe a[Vector[_]]
    }

    val listItems = List(1, 2, 3)
    api.splitsIntoPieces(listItems, 2).withLimit(1).supplyTo { pieces =>
      pieces.head shouldBe a[List[_]]
    }
  }

  "splitsIntoNonEmptyPieces specifying the number of pieces" should "yield non-empty pieces that concatenate to the original items" in {
    val testCaseTrials = for {
      items          <- itemsTrials.filter(_.nonEmpty)
      numberOfPieces <- api.integers(1, items.size)
      pieces         <- api.splitsIntoNonEmptyPieces(items, numberOfPieces)
    } yield (items, numberOfPieces, pieces)

    testCaseTrials.withLimit(100).supplyTo {
      case (items, numberOfPieces, pieces) =>
        pieces.size should be(numberOfPieces)
        pieces.flatten should be(items)
        pieces.forall(_.nonEmpty) should be(true)
    }
  }

  "splitsIntoNonEmptyPieces" should "yield non-empty pieces that concatenate to the original items" in {
    val testCaseTrials = for {
      items          <- itemsTrials
      pieces         <- api.splitsIntoNonEmptyPieces(items)
    } yield (items, pieces)

    testCaseTrials.withLimit(100).supplyTo {
      case (items, pieces) =>
        pieces.size should be <= items.size
        pieces.flatten should be(items)
        pieces.forall(_.nonEmpty) should be(true)
    }
  }
}
