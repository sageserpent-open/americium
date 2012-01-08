package com.sageserpent.infrastructure

import scala.math.Ordered

import org.scalatest.Suite

abstract class Unbounded[X <% Ordered[X]] extends Ordered[Unbounded[X]] {
  def compare(another: Unbounded[X]) = (this, another) match {
    case (Finite(thisUnlifted), Finite(anotherUnlifted)) => thisUnlifted compare anotherUnlifted
    case (PositiveInfinity(), PositiveInfinity())        => 0
    case (NegativeInfinity(), NegativeInfinity())        => 0
    case (_, PositiveInfinity())                         => -1
    case (NegativeInfinity(), _)                         => -1
    case (PositiveInfinity(), _)                         => 1
    case (_, NegativeInfinity())                         => 1
  }
}

case class Finite[X <% Ordered[X]](unlifted: X) extends Unbounded[X] {
}

case class NegativeInfinity[X <% Ordered[X]]() extends Unbounded[X] {
  implicit def fakeCovarianceHack[Y <% Ordered[Y]](ni: NegativeInfinity[Nothing]) = NegativeInfinity[Y]()
}

object NegativeInfinity extends NegativeInfinity[Nothing] {
}

case class PositiveInfinity[X <% Ordered[X]]() extends Unbounded[X] {
  implicit def fakeCovarianceHack[Y <% Ordered[Y]](pi: PositiveInfinity[Nothing]) = PositiveInfinity[Y]()
}

object PositiveInfinity extends PositiveInfinity[Nothing] {
}

class TestSuite extends Suite {
  val fortyFive = Finite(45)

  val negativeInfinity = NegativeInfinity

  val positiveInfinity = PositiveInfinity

  val twentyThree = Finite(23)

  def wrap(x: Int) = Finite(x)

  def testFinitesAndInfinitesInCombination() = {

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

    val positiveInfinity = PositiveInfinity

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

  def testFinites() = {

    assert(negativeInfinity < twentyThree)

    assert(twentyThree < fortyFive)

    assert(!(twentyThree > fortyFive))

    assert(Finite(23) == twentyThree)
  }

  def testInfinites() = {

    assert(NegativeInfinity == NegativeInfinity)

    assert(!(NegativeInfinity[Int]() > NegativeInfinity() || NegativeInfinity[Int]() < NegativeInfinity()))
    assert(!(NegativeInfinity > NegativeInfinity[Nothing]()) || NegativeInfinity < NegativeInfinity[Nothing]())
    assert(NegativeInfinity <= NegativeInfinity[Nothing] && NegativeInfinity >= NegativeInfinity[Nothing])
    assert(NegativeInfinity[Nothing] <= negativeInfinity && NegativeInfinity[Nothing] >= negativeInfinity)
    assert(negativeInfinity <= NegativeInfinity[Nothing] && negativeInfinity >= NegativeInfinity[Nothing])
    assert(NegativeInfinity <= NegativeInfinity && NegativeInfinity >= NegativeInfinity)
    assert(!(NegativeInfinity > NegativeInfinity || NegativeInfinity < NegativeInfinity))

    assert(PositiveInfinity == PositiveInfinity)

    assert(!(PositiveInfinity[Int]() > PositiveInfinity() || PositiveInfinity[Int]() < PositiveInfinity()))
    assert(!(PositiveInfinity > PositiveInfinity[Nothing]()) || PositiveInfinity < PositiveInfinity[Nothing]())
    assert(PositiveInfinity <= PositiveInfinity[Nothing] && PositiveInfinity >= PositiveInfinity[Nothing])
    assert(PositiveInfinity[Nothing] <= positiveInfinity && PositiveInfinity[Nothing] >= positiveInfinity)
    assert(positiveInfinity <= PositiveInfinity[Nothing] && positiveInfinity >= PositiveInfinity[Nothing])
    assert(PositiveInfinity <= PositiveInfinity && PositiveInfinity >= PositiveInfinity)
    assert(!(PositiveInfinity > PositiveInfinity || PositiveInfinity < PositiveInfinity))

    assert(NegativeInfinity != PositiveInfinity)
    assert(NegativeInfinity < PositiveInfinity)
    assert(PositiveInfinity > NegativeInfinity)
  }
}

object Main extends App {
  (new TestSuite).execute()
}

