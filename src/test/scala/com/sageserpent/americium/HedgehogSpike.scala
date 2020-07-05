package com.sageserpent.americium

import hedgehog._
import hedgehog.core.{Result, _}
import hedgehog.runner.Test
import org.scalatest.{Failed => _, _}

trait HedgehogIntegration {
  import Assertions.fail

  def check(property: PropertyT[Assertion])(
      implicit configuration: PropertyConfig): Unit = {
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

class HedgehogSpike extends FlatSpec with Matchers with HedgehogIntegration {
  val generator: Gen[List[Int]] = Gen
    .list(Gen.int(Range.linear(1, 100 / 7)).map(_ * 7), Range.linear(3, 10))
    .filter(list =>
      list.zip(list.tail).forall { case (first, second) => first <= second })

  implicit val configuration: PropertyConfig =
    PropertyConfig(SuccessCount(5000), DiscardCount(100), ShrinkLimit(100))

  "Hedgehog" should "eat all your bugs" in
    check(
      generator.forAll
        .map(testCase =>
          assert(testCase.length > 5 ||
            testCase.sum < 500)))
}
