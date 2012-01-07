package com.sageserpent.infrastructure

import scala.math.Ordered

import org.scalatest.Suite

abstract class Unbounded[X <% Ordered[X]] extends Ordered[Unbounded[X]] {
  def compare(another: Unbounded[X]) = (this, another) match {
    case (Finite(thisUnlifted), Finite(anotherUnlifted)) => thisUnlifted compare anotherUnlifted
    case (NegativeInfinity(), NegativeInfinity())        => 0
    case (NegativeInfinity(), _)                         => -1
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

class TestSuite extends Suite {

  def testOperations() = {
    val negativeInfinity = NegativeInfinity

    val fortyFive = Finite(45)

    assert(negativeInfinity < fortyFive)

    //assert(negativeInfinity < Finite(45))  // COMPILE ERROR WHEN UNCOMMENTED: Huh?

    assert(NegativeInfinity < fortyFive)

    //assert(NegativeInfinity < Finite(45))  // COMPILE ERROR WHEN UNCOMMENTED: Huh?

    assert(Finite(45) > negativeInfinity)

    assert(Finite(45) > NegativeInfinity)

    def wrap(x: Int) = Finite(x)

    assert(NegativeInfinity < wrap(45))

    assert(wrap(45) > NegativeInfinity)

    assert(NegativeInfinity < (Finite(45): Unbounded[Int]))

    assert(NegativeInfinity < (Finite(45): Finite[Int]))

    //********************************************

    val twentyThree = Finite(23)

    assert(negativeInfinity < twentyThree)

    assert(twentyThree < fortyFive)

    assert(!(twentyThree > fortyFive))

    assert(Finite(23) == twentyThree)

    //********************************************

    assert(NegativeInfinity == NegativeInfinity)

    assert(!(NegativeInfinity[Int]() > NegativeInfinity()))
    assert(!(NegativeInfinity > NegativeInfinity[Nothing]()))
    assert(NegativeInfinity <= NegativeInfinity[Nothing])
    assert(NegativeInfinity[Nothing] <= negativeInfinity)
    assert(negativeInfinity <= NegativeInfinity[Nothing])
    assert(NegativeInfinity <= NegativeInfinity)
    assert(!(NegativeInfinity > NegativeInfinity))

    assert(NegativeInfinity < twentyThree)
  }
}

object Main extends App {
  (new TestSuite).execute()
}

