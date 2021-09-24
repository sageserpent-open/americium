package com.sageserpent.americium

import com.sageserpent.americium.Trials.api
import com.sageserpent.americium.Unbounded.convertToOrdered
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UnboundedSpec extends AnyFlatSpec with Matchers {
  private val integerTrials = api.integers

  private val unboundedGenerator: Trials[Unbounded[Int]] =
    implicitly[Trials.Factory[Unbounded[Int]]].trials

  "lifted finite values" should "be ordered correspondingly to the underlying finite values" in
    (integerTrials and integerTrials)
      .withLimit(100)
      .supplyTo { (firstUnderlying, secondUnderlying) =>
        withClue(
          s"Comparing $firstUnderlying and $secondUnderlying as lifted finite values and then directly: "
        ) {
          Finite(firstUnderlying).compare(
            Finite(secondUnderlying)
          ) shouldBe firstUnderlying
            .compare(secondUnderlying)
        }
      }

  "negative infinity" should "be less than all finite values" in
    integerTrials
      .withLimit(100)
      .supplyTo { underlying =>
        (NegativeInfinity[Int](): Unbounded[Int]) shouldBe <(
          Finite(underlying): Unbounded[Int]
        )
      }

  "positive infinity" should "be greater than all finite values" in
    integerTrials
      .withLimit(100)
      .supplyTo { underlying =>
        (PositiveInfinity[Int](): Unbounded[Int]) shouldBe >(
          Finite(underlying): Unbounded[Int]
        )
      }

  "negative infinity" should "be less than positive infinity" in {
    (NegativeInfinity[Int](): Unbounded[Int]) shouldBe <(
      PositiveInfinity[Int](): Unbounded[Int]
    )
  }

  // TODO - use a laws approach for this, there will be one out there somewhere,
  // probably in Cats...
  "the same items" should "compare equal" in
    unboundedGenerator
      .withLimit(100)
      .supplyTo { unbounded =>
        withClue(s"The result of comparison of $unbounded with itself: ") {
          unbounded.compare(unbounded) shouldBe 0
        }
      }

  // TODO - use a laws approach for this, there will be one out there somewhere,
  // probably in Cats...
  "swapping two items" should "negate the comparison" in
    (unboundedGenerator and unboundedGenerator)
      .withLimit(100)
      .supplyTo { (one, another) =>
        withClue(
          s"Comparing $another with $one and then negating the reverse comparison: "
        ) {
          another.compare(one) shouldBe (-one.compare(another))
        }
      }

  // TODO - use a laws approach for this, there will be one out there somewhere,
  // probably in Cats...
  "transitive unequal comparisons" should "be possible with step wise unequal comparisons that agree in sense" in
    (unboundedGenerator and unboundedGenerator and unboundedGenerator)
      .withLimit(100)
      .supplyTo { (first, common, last) =>
        val firstWithCommon = first.compare(common)
        val commonWithLast  = common.compare(last)

        if (0 < firstWithCommon * commonWithLast) {
          assert(firstWithCommon.signum == commonWithLast.signum)
          withClue(s"Comparing $first with $last and then with $common") {
            first.compare(last).signum shouldBe firstWithCommon.signum
          }
        }
      }

  // TODO - use a laws approach for this, there will be one out there somewhere,
  // probably in Cats...
  "transitive equal comparisons" should "be possible with step wise equal comparisons" in
    (unboundedGenerator and unboundedGenerator and unboundedGenerator)
      .withLimit(100)
      .supplyTo { (first, common, last) =>
        val firstWithCommon = first.compare(common)
        val commonWithLast  = common.compare(last)

        Trials.whenever(0 == firstWithCommon && 0 == commonWithLast) {
          first.compare(last) shouldBe 0
        }
      }
}
