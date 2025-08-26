package com.sageserpent.americium

import com.eed3si9n.expecty.Expecty.assert
import com.sageserpent.americium.Trials.api
import com.sageserpent.americium.junit5.*
import org.junit.jupiter.api.{Test, TestFactory}

class UnboundedSuite {
  private val integerTrials = api.integers

  private implicit val ordering: Ordering[Unbounded[Int]] =
    Unbounded.ordering[Int]

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
        // First, via `Ordering`...
        assert(
          Ordering[Unbounded[Int]].compare(
            Finite(firstUnderlying),
            Finite(secondUnderlying)
          ) == firstUnderlying.compare(secondUnderlying)
        )
        assert(
          Ordering[Finite[Int]].compare(
            Finite(firstUnderlying),
            Finite(secondUnderlying)
          ) == firstUnderlying.compare(secondUnderlying)
        )

        // Second, via `Ordered` as a syntax enhancement...
        assert(
          Finite(firstUnderlying).compare(
            Finite(
              secondUnderlying
            )
          ) == firstUnderlying.compare(secondUnderlying)
        )
        assert(
          Finite(firstUnderlying).compare(
            Finite(
              secondUnderlying
            ): Unbounded[Int]
          ) == firstUnderlying.compare(secondUnderlying)
        )
        assert(
          (Finite(firstUnderlying): Unbounded[Int]).compare(
            Finite(
              secondUnderlying
            )
          ) == firstUnderlying.compare(secondUnderlying)
        )
    }

  @TestFactory
  def negativeInfinityShouldBeLessThanAllFiniteValues(): DynamicTests =
    integerTrials
      .withLimit(100)
      .dynamicTests { underlying =>
        // First, via `Ordering`...
        assert(
          Ordering[Unbounded[Int]].lt(NegativeInfinity, Finite(underlying))
        )
        assert(
          Ordering[Unbounded[Int]].gt(Finite(underlying), NegativeInfinity)
        )

        // Second, via `Ordered` as a syntax enhancement...
        assert(
          NegativeInfinity < Finite(underlying)
        )
        assert(
          NegativeInfinity < (Finite(underlying): Unbounded[Int])
        )
        assert(
          (NegativeInfinity: Unbounded[Int]) < Finite(underlying)
        )
        assert(
          (NegativeInfinity: Unbounded[Int]) < (Finite(underlying): Unbounded[
            Int
          ])
        )

        assert(
          Finite(underlying) > NegativeInfinity
        )
        assert(
          Finite(underlying) > (NegativeInfinity: Unbounded[Int])
        )
        assert(
          (Finite(underlying): Unbounded[Int]) > NegativeInfinity
        )
        assert(
          (Finite(underlying): Unbounded[Int]) > (NegativeInfinity: Unbounded[
            Int
          ])
        )

      }

  @TestFactory
  def positiveInfinityShouldBeGreaterThanAllFiniteValues(): DynamicTests =
    integerTrials
      .withLimit(100)
      .dynamicTests { underlying =>
        // First, via `Ordering`...
        assert(
          Ordering[Unbounded[Int]].gt(PositiveInfinity, Finite(underlying))
        )
        assert(
          Ordering[Unbounded[Int]].lt(Finite(underlying), PositiveInfinity)
        )

        // Second, via `Ordered` as a syntax enhancement...
        assert(
          PositiveInfinity > Finite(underlying)
        )
        assert(
          PositiveInfinity > (Finite(underlying): Unbounded[Int])
        )
        assert(
          (PositiveInfinity: Unbounded[Int]) > Finite(underlying)
        )
        assert(
          (PositiveInfinity: Unbounded[Int]) > (Finite(underlying): Unbounded[
            Int
          ])
        )

        assert(
          Finite(underlying) < PositiveInfinity
        )
        assert(
          Finite(underlying) < (PositiveInfinity: Unbounded[Int])
        )
        assert(
          (Finite(underlying): Unbounded[Int]) < PositiveInfinity
        )
        assert(
          (Finite(underlying): Unbounded[Int]) < (PositiveInfinity: Unbounded[
            Int
          ])
        )
      }

  @Test
  def negativeInfinityShouldBeLessThanPositiveInfinity(): Unit = {
    // First, via `Ordering`...
    assert(Ordering[Unbounded[Int]].lt(NegativeInfinity, PositiveInfinity))
    assert(Ordering[Unbounded[Int]].gt(PositiveInfinity, NegativeInfinity))

    // Second, via `Ordered` as a syntax enhancement...
    assert(NegativeInfinity < PositiveInfinity)
    assert(NegativeInfinity < (PositiveInfinity: Unbounded[Int]))
    assert((NegativeInfinity: Unbounded[Int]) < PositiveInfinity)
    assert(
      (NegativeInfinity: Unbounded[Int]) < (PositiveInfinity: Unbounded[Int])
    )

    assert(PositiveInfinity > NegativeInfinity)
    assert(PositiveInfinity > (NegativeInfinity: Unbounded[Int]))
    assert((PositiveInfinity: Unbounded[Int]) > NegativeInfinity)
    assert(
      (PositiveInfinity: Unbounded[Int]) > (NegativeInfinity: Unbounded[Int])
    )
  }

  @Test
  def negativeInfinityShouldBeEqualToItself(): Unit = {
    // First, via `Ordering`...
    assert(
      Ordering[NegativeInfinity.type].equiv(NegativeInfinity, NegativeInfinity)
    )
    assert(
      Ordering[Unbounded[Int]].equiv(NegativeInfinity, NegativeInfinity)
    )

    // Second, via `Ordered` as a syntax enhancement...
    assert(
      NegativeInfinity == NegativeInfinity
    )
    assert(
      (NegativeInfinity: Unbounded[Int]) == NegativeInfinity
    )
    assert(
      (NegativeInfinity: Unbounded[Int]) == (NegativeInfinity: Unbounded[Int])
    )
  }

  @Test
  def positiveInfinityShouldBeEqualToItself(): Unit = {
    // First, via `Ordering`...
    assert(
      Ordering[PositiveInfinity.type].equiv(PositiveInfinity, PositiveInfinity)
    )
    assert(
      Ordering[Unbounded[Int]].equiv(PositiveInfinity, PositiveInfinity)
    )

    // Second, via `Ordered` as a syntax enhancement...
    assert(
      PositiveInfinity == PositiveInfinity
    )
    assert(
      (PositiveInfinity: Unbounded[Int]) == PositiveInfinity
    )
    assert(
      (PositiveInfinity: Unbounded[Int]) == (PositiveInfinity: Unbounded[Int])
    )
  }
}
