package com.sageserpent.americium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ChooseSeveralOfTest extends AnyFlatSpec with Matchers {
  val api = Trials.api

  private val itemsAndSizeToChooseTrials: Trials[(Vector[Int], Int)] =
    for {
      items        <- api.integers.several[Vector[Int]]
      sizeToChoose <- api.integers(0, items.size)
    } yield items -> sizeToChoose

  "chooseSeveralOf" should "yield a permutation of some of the original items" in {
    itemsAndSizeToChooseTrials.withLimit(50).supplyTo {
      case (items, sizeToChoose) =>
        api.chooseSeveralOf(items, sizeToChoose).withLimit(10).supplyTo {
          chosen =>
            chosen.size should be(sizeToChoose)
            chosen.toSet.subsetOf(items.toSet) should be(true)
            // Since we are picking a permutation, it should have distinct
            // elements if the input has distinct elements.
            // Wait, if the input has duplicates, the output can have
            // duplicates.
            // But it's a sample without replacement from the indices.
            if (items.distinct.size == items.size) {
              chosen.distinct.size should be(sizeToChoose)
            }
        }
    }
  }

  it should "eventually yield all possible permutations of the given size" in {
    val items                        = Vector(1, 2, 3)
    val sizeToChoose                 = 2
    val expectedNumberOfPermutations = 6 // 3 * 2
    val permutations =
      api
        .chooseSeveralOf(items, sizeToChoose)
        .withLimit(100)
        .asIterator()
        .to(List)
    permutations should have size expectedNumberOfPermutations
    permutations.distinct should have size expectedNumberOfPermutations
  }

  it should "handle choosing zero items" in {
    val items = Vector(1, 2, 3)
    api.chooseSeveralOf(items, 0).withLimit(1).supplyTo { chosen =>
      chosen shouldBe empty
    }
  }

  it should "handle choosing all items (behaving like a shuffle)" in {
    val items = Vector(1, 2, 3)
    val permutations = api
      .chooseSeveralOf(items, items.size)
      .withLimit(100)
      .asIterator()
      .to(List)

    permutations should have size 6
    permutations.distinct should have size 6
  }

  it should "throw an requirement error if numberToChoose is greater than the number of candidates" in {
    val items = Vector(1, 2, 3)
    an[IllegalArgumentException] should be thrownBy {
      api.chooseSeveralOf(items, 4)
    }
  }
}
