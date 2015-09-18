package com.sageserpent.infrastructure

import com.sageserpent.americium.{Unbounded, NegativeInfinity, PositiveInfinity, Finite}
import junit.framework.TestCase
import org.junit.Test


class UnboundedTests extends TestCase {
  val fortyFive = Finite(45)

  val negativeInfinity = NegativeInfinity[Int]

  val positiveInfinity = PositiveInfinity[Int]

  val twentyThree = Finite(23)

  def wrap(x: Int) = Finite(x)

  @Test
  def testFinitesAndInfinitesInCombination() {
    assert(negativeInfinity < fortyFive)

    assert(negativeInfinity < Finite(45))

    assert(NegativeInfinity[Int] < fortyFive)

    assert(NegativeInfinity[Int] < Finite(45))

    assert(Finite(45) > negativeInfinity)

    assert(Finite(45) > NegativeInfinity[Int])

    assert(NegativeInfinity[Int] < wrap(45))

    assert(wrap(45) > NegativeInfinity[Int])

    assert(NegativeInfinity[Int] < (Finite(45): Unbounded[Int]))

    assert(NegativeInfinity[Int] < (Finite(45): Finite[Int]))

    assert(NegativeInfinity[Int] < twentyThree)

    assert(positiveInfinity > fortyFive)

    assert(positiveInfinity > Finite(45))

    assert(PositiveInfinity[Int] > fortyFive)

    assert(PositiveInfinity[Int] > Finite(45))

    assert(Finite(45) < positiveInfinity)

    assert(Finite(45) < PositiveInfinity[Int])

    assert(PositiveInfinity[Int] > wrap(45))

    assert(wrap(45) < PositiveInfinity[Int])

    assert(PositiveInfinity[Int] > (Finite(45): Unbounded[Int]))

    assert(PositiveInfinity[Int] > (Finite(45): Finite[Int]))

    assert(PositiveInfinity[Int] > twentyThree)
  }

  @Test
  def testFinites() {
    assert(negativeInfinity < twentyThree)

    assert(twentyThree < fortyFive)

    assert(!(twentyThree > fortyFive))

    assert(Finite(23) == twentyThree)
  }

  @Test
  def testInfinites() {
    assert(NegativeInfinity == NegativeInfinity)
    assert(NegativeInfinity[Nothing] == NegativeInfinity[Int])
    assert(!(NegativeInfinity[Int] < NegativeInfinity[Int]))


    assert(!(NegativeInfinity[Int] > NegativeInfinity[Int] || NegativeInfinity[Int] < NegativeInfinity[Int]))

    assert(NegativeInfinity[Int] <= NegativeInfinity[Int] && NegativeInfinity[Int] >= NegativeInfinity[Int])

    assert(PositiveInfinity == PositiveInfinity)
    assert(PositiveInfinity[Nothing] == PositiveInfinity[Int])
    assert(!(PositiveInfinity[Int] < PositiveInfinity[Int]))

    assert(!(PositiveInfinity[Int] > PositiveInfinity[Int] || PositiveInfinity[Int] < PositiveInfinity[Int]))

    assert(PositiveInfinity[Int] <= PositiveInfinity[Int] && PositiveInfinity[Int] >= PositiveInfinity[Int])

    assert(NegativeInfinity != PositiveInfinity)
    assert(NegativeInfinity[Int] < PositiveInfinity[Int])
    assert(PositiveInfinity[Int] > NegativeInfinity[Int])
  }
}