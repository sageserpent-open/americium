package com.sageserpent.americium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Examples extends AnyFlatSpec with Matchers {
  // Insertion sort, but with a bug...
  def notSoStableSort[Element](
      elements: List[Element]
  )(implicit ordering: Ordering[Element]): List[Element] =
    elements match {
      case Nil => Nil
      case head :: tail =>
        notSoStableSort(tail).span(ordering.lteq(_, head)) match {
          case (first, second) => first ++ (head :: second)
        }
    }

  val ordering = Ordering.by[(Int, Int), Int](_._1)

  val api = Trials.api

  val lists = (for {
    key   <- api.choose(0 to 100)
    value <- api.integers
  } yield key -> value).lists

  "stableSorting" should "sort according to the ordering" in
    lists
      .filter(
        _.nonEmpty
      ) // Filter out the empty case as we can't assert sensibly on it.
      .withLimit(200)
      .supplyTo { nonEmptyList: List[(Int, Int)] =>
        val sortedResult = notSoStableSort(nonEmptyList)(ordering)

        assert(
          sortedResult.zip(sortedResult.tail).forall((ordering.lteq _).tupled)
        )
      }

  it should "conserve the original elements" in
    lists.withLimit(200).supplyTo { list: List[(Int, Int)] =>
      val sortedResult = notSoStableSort(list)(ordering)

      sortedResult should contain theSameElementsAs list
    }

  // This is going to fail...
  ignore should "also preserve the original order of the subsequences of elements that are equivalent according to the order" in
    lists.withLimit(200).supplyTo { list: List[(Int, Int)] =>
      Trials.whenever(
        list.nonEmpty
      ) // Filter out the empty case as while we can assert on it, the assertion would be trivial.
      {
        val sortedResult = notSoStableSort(list)(ordering)

        assert(sortedResult.groupBy(_._1) == list.groupBy(_._1))
      }
    }
}
