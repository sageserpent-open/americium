package com.sageserpent.americium

import hedgehog.core.{DiscardCount, PropertyConfig, ShrinkLimit, SuccessCount}
import hedgehog.{Gen, Range}
import org.scalatest.{FlatSpec, Matchers}

class HedgehogSpike
    extends FlatSpec
    with Matchers
    with HedgehogScalatestIntegration {
  private val sortedMultiplesOfSevenGenerator: Gen[List[Int]] = Gen
    .list(Gen.int(Range.linear(1, 100 / 7)).map(_ * 7), Range.linear(3, 10))
    .filter(list =>
      list.zip(list.tail).forall { case (first, second) => first <= second })

  private val shoutGenerator: Gen[String] =
    Gen.string(Gen.alpha, Range.linear(1, 10)).map(_.toUpperCase)

  private val choiceGenerator: Gen[Either[String, Boolean]] = {
    // Implicit (but locally scoped) context so that we know what Magnolia will pick up.
    implicit val stringGenerator  = shoutGenerator
    implicit val booleanGenerator = Gen.boolean
    hedgehogGenByMagnolia.gen // Use an explicit call for clarity; we're capturing into a non-implicit val anyway.
  }

  implicit val configuration: PropertyConfig =
    PropertyConfig(SuccessCount(5000), DiscardCount(1000), ShrinkLimit(100))

  "Hedgehog" should "eat all your bugs" in
    check(sortedMultiplesOfSevenGenerator, shoutGenerator, choiceGenerator) {
      (sortedMultiplesOfSeven, shout, choice) =>
        // Gibber to stdout.
        println(choice)
        // This is just for the sake of having some assertions - they don't mean anything special.
        assert(
          sortedMultiplesOfSeven.length > 5 ||
            sortedMultiplesOfSeven.sum < 500)
        shout should be(shout.toUpperCase)
    }
}
