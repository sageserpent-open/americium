package com.sageserpent.americium
import com.sageserpent.americium.Trials.api
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BuildingComplexTrials extends AnyFlatSpec with Matchers {

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
