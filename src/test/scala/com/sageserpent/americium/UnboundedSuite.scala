package com.sageserpent.americium

import com.eed3si9n.expecty.Expecty.assert
import com.sageserpent.americium.Trials.api
import com.sageserpent.americium.junit5.*
import org.junit.jupiter.api.{Test, TestFactory}

class UnboundedSuite {
  // TODO: I would like to be able to write comparisons using the `Ops` syntax
  // enhancement, but this doesn't play well with the infinities...

  private val integerTrials = api.integers
  
  // This test proves that `Unbounded[X]` respects the same order as the
  // underlying `X`. So there is no need to prove the axioms about reflexivity,
  // anticommutativity and transitivity, as these come for free via
  // `Ordering[X]`. The only loophole is how the infinite values fit into the
  // ordering; that is covered by the other tests.
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
}
