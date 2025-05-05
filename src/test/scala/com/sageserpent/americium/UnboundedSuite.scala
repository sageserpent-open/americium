package com.sageserpent.americium

import com.eed3si9n.expecty.Expecty.assert
import com.sageserpent.americium.Trials.api
import com.sageserpent.americium.junit5.*
import org.junit.jupiter.api.{Test, TestFactory}

class UnboundedSuite {
  // TODO: I would like to be able to write comparisons using the `Ops` syntax
  // enhancement, but this doesn't play well with the infinities...

  private val integerTrials = api.integers

  private val unboundedGenerator: Trials[Unbounded[Int]] =
    implicitly[Factory[Unbounded[Int]]].trials

  @TestFactory
  def liftedFiniteValuesShouldBeOrderedToTheUnderlyingFiniteValues()
      : DynamicTests =
    (integerTrials and integerTrials).withLimit(100).dynamicTests {
      (firstUnderlying, secondUnderlying) =>
        assert(
          Ordering[Unbounded[Int]].compare(
            Finite(firstUnderlying),
            Finite(secondUnderlying)
          ) == firstUnderlying.compare(secondUnderlying)
        )
    }

  @TestFactory
  def negativeInfinityShouldBeLessThanAllFiniteValues(): DynamicTests =
    integerTrials
      .withLimit(100)
      .dynamicTests { underlying =>
        assert(
          Ordering[Unbounded[Int]].lt(NegativeInfinity, Finite(underlying))
        )
        assert(
          Ordering[Unbounded[Int]].gt(Finite(underlying), NegativeInfinity)
        )
      }

  @TestFactory
  def positiveInfinityShouldBeGreaterThanAllFiniteValues(): DynamicTests =
    integerTrials
      .withLimit(100)
      .dynamicTests { underlying =>
        assert(
          Ordering[Unbounded[Int]].gt(PositiveInfinity, Finite(underlying))
        )
        assert(
          Ordering[Unbounded[Int]].lt(Finite(underlying), PositiveInfinity)
        )
      }

  @Test
  def negativeInfinityShouldBeLessThanPositiveInfinity(): Unit = {
    assert(Ordering[Unbounded[Int]].lt(NegativeInfinity, PositiveInfinity))
    assert(Ordering[Unbounded[Int]].gt(PositiveInfinity, NegativeInfinity))
  }

  @Test
  def negativeInfinityShouldBeEqualToItself(): Unit = {
    assert(
      Ordering[NegativeInfinity.type].equiv(NegativeInfinity, NegativeInfinity)
    )
    assert(
      Ordering[Unbounded[Int]].equiv(NegativeInfinity, NegativeInfinity)
    )
  }

  @Test
  def positiveInfinityShouldBeEqualToItself(): Unit = {
    assert(
      Ordering[PositiveInfinity.type].equiv(PositiveInfinity, PositiveInfinity)
    )
    assert(
      Ordering[Unbounded[Int]].equiv(PositiveInfinity, PositiveInfinity)
    )
  }

  // TODO - use a laws approach for this, there will be one out there somewhere,
  // probably in Cats...
  @TestFactory
  def theSameItemsShouldCompareEqual(): DynamicTests =
    unboundedGenerator
      .withLimit(100)
      .dynamicTests { unbounded =>
        assert(0 == Ordering[Unbounded[Int]].compare(unbounded, unbounded))
      }

  // TODO - use a laws approach for this, there will be one out there somewhere,
  // probably in Cats...
  @TestFactory
  def swappingTwoItemsShouldNegateTheComparison(): DynamicTests =
    (unboundedGenerator and unboundedGenerator)
      .withLimit(100)
      .dynamicTests { (one, another) =>
        assert(
          Ordering[Unbounded[Int]]
            .compare(another, one) == -Ordering[Unbounded[Int]]
            .compare(one, another)
        )
      }

  // TODO - use a laws approach for this, there will be one out there somewhere,
  // probably in Cats...
  @TestFactory
  def transitiveUnequalComparisonsShouldBePossibleWithStepwiseUnequalComparisonsThatAgreeInSense()
      : DynamicTests =
    (unboundedGenerator and unboundedGenerator and unboundedGenerator)
      .withLimit(100)
      .dynamicTests { (first, common, last) =>
        val firstWithCommon = Ordering[Unbounded[Int]].compare(first, common)
        val commonWithLast  = Ordering[Unbounded[Int]].compare(common, last)

        Trials.whenever(0 < firstWithCommon * commonWithLast) {
          assume(firstWithCommon.sign == commonWithLast.sign)
          assert(
            Ordering[Unbounded[Int]]
              .compare(first, last)
              .sign == firstWithCommon.sign
          )
        }
      }

  // TODO - use a laws approach for this, there will be one out there somewhere,
  // probably in Cats...
  @TestFactory
  def transitiveEqualComparisonsShouldBePossibleWithStepWiseEqualComparisons
      : DynamicTests =
    (unboundedGenerator and unboundedGenerator and unboundedGenerator)
      .withLimit(100)
      .dynamicTests { (first, common, last) =>
        val firstWithCommon = Ordering[Unbounded[Int]].compare(first, common)
        val commonWithLast  = Ordering[Unbounded[Int]].compare(common, last)

        Trials.whenever(0 == firstWithCommon && 0 == commonWithLast) {
          assert(0 == Ordering[Unbounded[Int]].compare(first, last))
        }
      }
}
