package com.sageserpent.infrastructure

import junit.framework.TestCase
import com.sageserpent.infrastructure._
import org.junit.Test

class UnboundedTests extends TestCase {
  val fortyFive = Finite(45)

  val negativeInfinity = NegativeInfinity

  val positiveInfinity = PositiveInfinity

  val twentyThree = Finite(23)

  def wrap(x: Int) = Finite(x)

  @Test
  def testFinitesAndInfinitesInCombination() {
    assert(negativeInfinity < fortyFive)

    //assert(negativeInfinity < Finite(45))  // COMPILE ERROR WHEN UNCOMMENTED: Huh?

    assert(NegativeInfinity < fortyFive)

    //assert(NegativeInfinity < Finite(45))  // COMPILE ERROR WHEN UNCOMMENTED: Huh?

    assert(Finite(45) > negativeInfinity)

    assert(Finite(45) > NegativeInfinity)

    assert(NegativeInfinity < wrap(45))

    assert(wrap(45) > NegativeInfinity)

    assert(NegativeInfinity < (Finite(45): Unbounded[Int]))

    assert(NegativeInfinity < (Finite(45): Finite[Int]))

    assert(NegativeInfinity < twentyThree)

    assert(positiveInfinity > fortyFive)

    //assert(positiveInfinity > Finite(45))  // COMPILE ERROR WHEN UNCOMMENTED: Huh?

    assert(PositiveInfinity > fortyFive)

    //assert(PositiveInfinity > Finite(45))  // COMPILE ERROR WHEN UNCOMMENTED: Huh?

    assert(Finite(45) < positiveInfinity)

    assert(Finite(45) < PositiveInfinity)

    assert(PositiveInfinity > wrap(45))

    assert(wrap(45) < PositiveInfinity)

    assert(PositiveInfinity > (Finite(45): Unbounded[Int]))

    assert(PositiveInfinity > (Finite(45): Finite[Int]))

    assert(PositiveInfinity > twentyThree)
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
    assert(!(NegativeInfinity[Nothing] < NegativeInfinity[Int]))

    assert(!(NegativeInfinity[Int]() > NegativeInfinity() || NegativeInfinity[Int]() < NegativeInfinity()))
    assert(!(NegativeInfinity > NegativeInfinity[Nothing]()) || NegativeInfinity < NegativeInfinity[Nothing]())
    assert(!(negativeInfinity > NegativeInfinity[Nothing] || negativeInfinity < NegativeInfinity[Nothing]))
    assert(!(NegativeInfinity > NegativeInfinity || NegativeInfinity < NegativeInfinity))

    assert(NegativeInfinity <= NegativeInfinity[Int] && NegativeInfinity >= NegativeInfinity[Int])
    assert(NegativeInfinity[Nothing] <= negativeInfinity && NegativeInfinity[Nothing] >= negativeInfinity)
    assert(negativeInfinity <= NegativeInfinity[Nothing] && negativeInfinity >= NegativeInfinity[Nothing])
    assert(NegativeInfinity <= NegativeInfinity && NegativeInfinity >= NegativeInfinity)

    assert(PositiveInfinity == PositiveInfinity)
    assert(PositiveInfinity[Nothing] == PositiveInfinity[Int])
    assert(!(PositiveInfinity[Nothing] < PositiveInfinity[Int]))

    assert(!(PositiveInfinity[Int]() > PositiveInfinity() || PositiveInfinity[Int]() < PositiveInfinity()))
    assert(!(PositiveInfinity > PositiveInfinity[Nothing]()) || PositiveInfinity < PositiveInfinity[Nothing]())
    assert(!(positiveInfinity > PositiveInfinity[Nothing] || positiveInfinity < PositiveInfinity[Nothing]))
    assert(!(PositiveInfinity > PositiveInfinity || PositiveInfinity < PositiveInfinity))

    assert(PositiveInfinity <= PositiveInfinity[Int] && PositiveInfinity >= PositiveInfinity[Int])
    assert(PositiveInfinity[Nothing] <= positiveInfinity && PositiveInfinity[Nothing] >= positiveInfinity)
    assert(positiveInfinity <= PositiveInfinity[Nothing] && positiveInfinity >= PositiveInfinity[Nothing])
    assert(PositiveInfinity <= PositiveInfinity && PositiveInfinity >= PositiveInfinity)

    assert(NegativeInfinity != PositiveInfinity)
    assert(NegativeInfinity < PositiveInfinity)
    assert(PositiveInfinity > NegativeInfinity)
  }
}