package com.sageserpent.americium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ShufflesTest extends AnyFlatSpec with Matchers {
  val api = Trials.api

  private val itemsTrials: Trials[Vector[Int]] =
    api.integers.several[Vector[Int]]

  "shuffles" should "yield a permutation of the original items" in {
    itemsTrials.withLimit(50).supplyTo { items =>
      api.shuffles(items).withLimit(10).supplyTo { shuffled =>
        shuffled should contain theSameElementsAs items
        shuffled.size should be(items.size)
      }
    }
  }

  it should "preserve the container type of the shuffles" in {
    val vectorItems = Vector(1, 2, 3)
    api.shuffles(vectorItems).withLimit(1).supplyTo { shuffled =>
      shuffled shouldBe a[Vector[_]]
    }

    val listItems = List(1, 2, 3)
    api.shuffles(listItems).withLimit(1).supplyTo { shuffled =>
      shuffled shouldBe a[List[_]]
    }
  }

  it should "eventually yield all possible shuffles" in {
    val items                    = Vector(1, 2, 3)
    val expectedNumberOfShuffles = 6 // 3!
    val shuffles                 =
      api.shuffles(items).withLimit(100).asIterator().to(LazyList).toSet
    shuffles should have size expectedNumberOfShuffles
  }

  it should "handle empty collections" in {
    val emptyItems = Vector.empty[Int]
    api.shuffles(emptyItems).withLimit(1).supplyTo { shuffled =>
      shuffled shouldBe empty
    }
  }
}
