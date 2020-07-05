package com.sageserpent.americium

import hedgehog._
import hedgehog.core.{Result, _}
import hedgehog.runner.Test
import org.scalatest.{Failed => _, _}

class HedgehogSpike extends FlatSpec with Matchers {
  val configuration: PropertyConfig =
    PropertyConfig(SuccessCount(5000), DiscardCount(100), ShrinkLimit(100))

  val generator: Gen[List[Int]] = Gen
    .list(Gen.int(Range.linear(1, 100 / 7)).map(_ * 7), Range.linear(3, 10))
    .filter(list =>
      list.zip(list.tail).forall { case (first, second) => first <= second })

  "Hedgehog" should "eat all your bugs" in {
    {
      val property: PropertyT[Assertion] = generator.forAll
        .map(
          testCase =>
            assert(testCase.length <= 5 ||
              testCase.sum < 500))

      val report = Property.check(
        configuration,
        property.map(_ => Result.success),
        Seed.fromLong(1L)
      )

      report.status match {
        case OK =>
        case GaveUp =>
          fail(
            s"Gave up after only ${report.tests.value} passed test. ${report.discards.value} were discarded")
        case Failed(shrinks, log) =>
          fail(
            (s"Falsified after ${report.tests.value} passed tests, did ${shrinks.value} shrinks" :: log
              .map(Test.renderLog)).mkString("\n"))
      }
    }
  }
}
