package com.sageserpent.americium

import hedgehog.core.{DiscardCount, PropertyConfig, ShrinkLimit, SuccessCount}
import hedgehog.{Gen, Range}
import org.scalatest.{FlatSpec, Matchers}

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
