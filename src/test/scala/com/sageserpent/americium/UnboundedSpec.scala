package com.sageserpent.americium

import hedgehog.core.{DiscardCount, PropertyConfig, ShrinkLimit, SuccessCount}
import hedgehog.{Gen, Range}
import org.scalatest.{FlatSpec, Matchers}

class UnboundedSpec
    extends FlatSpec
    with Matchers
    with HedgehogScalatestIntegration {
  implicit val configuration: PropertyConfig =
    PropertyConfig(SuccessCount(1000), DiscardCount(15000), ShrinkLimit(500))

  private val integerGenerator: Gen[Int] = Gen.int(Range.linear(-200, 200))

  private val unboundedGenerator: Gen[Unbounded[Int]] = {
    implicit val _ = integerGenerator

    hedgehogGenByMagnolia.gen
  }

  "lifted finite values" should "be ordered correspondingly to the underlying finite values" in
    check(integerGenerator, integerGenerator) {
      (firstUnderlying, secondUnderlying) =>
        withClue(s"Comparing $firstUnderlying and $secondUnderlying as lifted finite values and then directly: "){
          Finite(firstUnderlying).compare(Finite(secondUnderlying)) shouldBe firstUnderlying
            .compare(secondUnderlying)
        }
    }

  "negative infinity" should "be less than all finite values" in
    check(integerGenerator) { underlying =>
      (NegativeInfinity[Int](): Unbounded[Int]) shouldBe < (Finite(underlying): Unbounded[Int])
    }

  "positive infinity" should "be greater than all finite values" in check(
    integerGenerator) { underlying =>
    (PositiveInfinity[Int](): Unbounded[Int]) shouldBe > (Finite(underlying): Unbounded[Int])
  }

  "negative infinity" should "be less than positive infinity" in {
    (NegativeInfinity[Int](): Unbounded[Int]) shouldBe < (PositiveInfinity[Int](): Unbounded[Int])
  }

  // TODO - use a laws approach for this, there will be one out there somewhere, probably in Cats...
  "the same items" should "compare equal" in
    check(unboundedGenerator) { unbounded =>
      withClue(s"The result of comparison of $unbounded with itself: ") {
        unbounded.compare(unbounded) shouldBe 0
      }
    }

  // TODO - use a laws approach for this, there will be one out there somewhere, probably in Cats...
  "swapping two items" should "negate the comparison" in
    check(unboundedGenerator, unboundedGenerator) { (one, another) =>
      withClue(s"Comparing $another with $one and then negating the reverse comparison: ") {
        another.compare(one) shouldBe (-one.compare(another))
      }
    }

  // TODO - use a laws approach for this, there will be one out there somewhere, probably in Cats...
  "transitive unequal comparisons" should "be possible with step wise unequal comparisons that agree in sense" in
    check(unboundedGenerator, unboundedGenerator, unboundedGenerator) {
      (first, common, last) =>
        val firstWithCommon = first.compare(common)
        val commonWithLast  = common.compare(last)

        whenever(0 < firstWithCommon * commonWithLast) {
          assert(firstWithCommon.signum == commonWithLast.signum)
          withClue(s"Comparing $first with $last and then with $common"){
            first.compare(last).signum shouldBe firstWithCommon.signum
          }
        }
    }

  // TODO - use a laws approach for this, there will be one out there somewhere, probably in Cats...
  "transitive equal comparisons" should "be possible with step wise equal comparisons" in
    check(unboundedGenerator, unboundedGenerator, unboundedGenerator) {
      (first, common, last) =>
        val firstWithCommon = first.compare(common)
        val commonWithLast  = common.compare(last)

        whenever(0 == firstWithCommon && 0 == commonWithLast) {
          first.compare(last) shouldBe 0
        }
    }
}
