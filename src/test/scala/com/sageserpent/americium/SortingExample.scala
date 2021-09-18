package com.sageserpent.americium

import com.sageserpent.americium.Trials.api
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SortingExample extends AnyFlatSpec with Matchers {
  // Insertion sort, but with a bug...
  def notSoStableSort[Element](
      elements: List[Element]
  )(implicit ordering: Ordering[Element]): List[Element] =
    elements match {
      case Nil          => Nil
      case head :: tail =>
        // Spot the deliberate mistake......vvvv
        notSoStableSort(tail).span(ordering.lteq(_, head)) match {
          case (first, second) => first ++ (head :: second)
        }
    }

  // We're going to sort a list of associations (key-value pairs) by the key...
  val ordering = Ordering.by[(Int, Int), Int](_._1)

  // Build up a trials instance for key value pairs by flat-mapping from simpler
  // trials instances for the keys and values...
  val keyValuePairs: Trials[(Int, Int)] = for {
    key <- api.choose(
      0 to 100
    ) // We want to encourage duplicated keys - so a key is always some integer from 0 up to but not including 100.
    value <-
      api.integers // A value on the other hand is any integer from right across the permissible range.
  } yield key -> value

  // Here's the trials instance we use to drive the tests for sorting...
  val associationLists: Trials[List[(Int, Int)]] =
    keyValuePairs.lists // This makes a trials of lists out of the simpler trials of key-value pairs.

  "stableSorting" should "sort according to the ordering" in
    associationLists
      .filter(
        _.nonEmpty
      ) // Filter out the empty case as we can't assert sensibly on it.
      .withLimit(200) // Only check up to 200 cases inclusive.
      .supplyTo { nonEmptyAssocationList: List[(Int, Int)] =>
        // This is a parameterised test, using `nonEmptyAssociationList` as the
        // test case parameter...
        val sortedResult = notSoStableSort(nonEmptyAssocationList)(ordering)

        // Using Scalatest assertions here...
        assert(
          sortedResult.zip(sortedResult.tail).forall((ordering.lteq _).tupled)
        )
      }

  it should "conserve the original elements" in
    associationLists.withLimit(200).supplyTo {
      associationList: List[(Int, Int)] =>
        val sortedResult = notSoStableSort(associationList)(ordering)

        sortedResult should contain theSameElementsAs associationList
    }

  // Until the bug is fixed, we expect this test to fail...
  it should "also preserve the original order of the subsequences of elements that are equivalent according to the order" ignore
    associationLists.withLimit(200).supplyTo {
      associationList: List[(Int, Int)] =>
        Trials.whenever(
          associationList.nonEmpty
        ) // Filter out the empty case as while we can assert on it, the assertion would be trivial.
        {
          val sortedResult = notSoStableSort(associationList)(ordering)

          assert(sortedResult.groupBy(_._1) == associationList.groupBy(_._1))
        }
    }

  // Until the bug is fixed, we expect this test to fail...
  it should "also preserve the original order of the subsequences of elements that are equivalent according to the order - this time with the failure reproduced directly" ignore
    associationLists
      .withRecipe("""[
                    |    {
                    |        "ChoiceOf" : {
                    |            "index" : 1
                    |        }
                    |    },
                    |    {
                    |        "ChoiceOf" : {
                    |            "index" : 46
                    |        }
                    |    },
                    |    {
                    |        "FactoryInputOf" : {
                    |            "input" : 0
                    |        }
                    |    },
                    |    {
                    |        "ChoiceOf" : {
                    |            "index" : 1
                    |        }
                    |    },
                    |    {
                    |        "ChoiceOf" : {
                    |            "index" : 46
                    |        }
                    |    },
                    |    {
                    |        "FactoryInputOf" : {
                    |            "input" : 2
                    |        }
                    |    },
                    |    {
                    |        "ChoiceOf" : {
                    |            "index" : 0
                    |        }
                    |    }
                    |]""".stripMargin)
      .supplyTo { associationList: List[(Int, Int)] =>
        {
          val sortedResult = notSoStableSort(associationList)(ordering)

          assert(sortedResult.groupBy(_._1) == associationList.groupBy(_._1))
        }
      }
}
