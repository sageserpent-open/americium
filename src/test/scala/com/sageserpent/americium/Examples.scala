package com.sageserpent.americium

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Examples extends AnyFlatSpec with Matchers {
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

  val api = Trials.api

  // Here's the trials instance...
  val associationLists: Trials[List[(Int, Int)]] = (for {
    key   <- api.choose(0 to 100)
    value <- api.integers
  } yield key -> value).lists

  "stableSorting" should "sort according to the ordering" in
    associationLists
      .filter(
        _.nonEmpty
      )               // Filter out the empty case as we can't assert sensibly on it.
      .withLimit(200) // Only check up to 200 cases inclusive.
      .supplyTo { nonEmptyAssocationList: List[(Int, Int)] =>
        // This is a parameterised test, using `nonEmptyAssociationList` as the test case parameter...
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

  val listsFavouringDuplicatedEntries: Trials[List[Int]] =
    for {
      size    <- api.choose(0 to 10)
      choices <- api.integers.listsOfSize(size)
      values = api.choose(choices)
      result <- values.listsOfSize(size)
    } yield result

  def vainAttemptToDisproveHavingMoreThanTwoAdjacentDuplicates(
      list: List[Int]
  ) = {
    import com.sageserpent.americium.seqEnrichment._

    val groupsOfAdjacentDuplicates = list.groupWhile(_ == _)

    println(groupsOfAdjacentDuplicates)

    assert(groupsOfAdjacentDuplicates.find(_.size > 2).isEmpty)
  }

  // We expect this test to fail because our assumptions are wrong...
  "a lazy developer" should "get an unwelcome surprise" ignore
    listsFavouringDuplicatedEntries
      .withLimit(50)
      .supplyTo(vainAttemptToDisproveHavingMoreThanTwoAdjacentDuplicates)

  // We expect this test to fail because our assumptions are wrong...
  they should "get an unwelcome surprise - this time with the failure reproduced directly" ignore {
    listsFavouringDuplicatedEntries
      .withRecipe("""[
                    |    {
                    |        "ChoiceOf" : {
                    |            "index" : 3
                    |        }
                    |    },
                    |    {
                    |        "FactoryInputOf" : {
                    |            "input" : 0
                    |        }
                    |    },
                    |    {
                    |        "FactoryInputOf" : {
                    |            "input" : 0
                    |        }
                    |    },
                    |    {
                    |        "FactoryInputOf" : {
                    |            "input" : -1
                    |        }
                    |    },
                    |    {
                    |        "ChoiceOf" : {
                    |            "index" : 1
                    |        }
                    |    },
                    |    {
                    |        "ChoiceOf" : {
                    |            "index" : 0
                    |        }
                    |    },
                    |    {
                    |        "ChoiceOf" : {
                    |            "index" : 1
                    |        }
                    |    }
                    |]""".stripMargin)
      .supplyTo(vainAttemptToDisproveHavingMoreThanTwoAdjacentDuplicates)
  }
}
