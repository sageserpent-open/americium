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
        Finite(firstUnderlying).compare(Finite(secondUnderlying)) shouldBe firstUnderlying
          .compare(secondUnderlying)
    }

  "negative infinity" should "be less than all finite values" in
    check(integerGenerator) { underlying =>
      NegativeInfinity[Int]().compare(Finite(underlying)) should be < 0
    }

  "positive infinity" should "be greater than all finite values" in check(
    integerGenerator) { underlying =>
    PositiveInfinity[Int]().compare(Finite(underlying)) should be > 0
  }

  "negative infinity" should "be less than positive infinity" in {
    NegativeInfinity[Int]().compare(PositiveInfinity[Int]()) shouldBe -1
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
      withClue(s"The result of comparison of $another with $one: ") {
        another.compare(one) shouldBe (-one.compare(another))
      }
    }

  // TODO - use a laws approach for this, there will be one out there somewhere, probably in Cats...
  "transitive comparisons" should "be possible when step wise comparisons do not change sign" in {
    check(unboundedGenerator, unboundedGenerator, unboundedGenerator) {
      (first, common, last) =>
        whenever(0 < first.compare(common) * common.compare(last)) {
          first.compare(last) shouldBe first.compare(common)
        }
    }

    check(unboundedGenerator, unboundedGenerator, unboundedGenerator) {
      (first, common, last) =>
        whenever(0 == first.compare(common) && 0 == common.compare(last)) {
          first.compare(last) shouldBe first.compare(common)
        }
    }
  }
}
