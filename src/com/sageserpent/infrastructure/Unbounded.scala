package com.sageserpent.infrastructure

import scala.math.Ordered

import org.scalatest.Suite

class Unbounded[X <% Ordered[X]] extends Ordered[Unbounded[X]] {
  def compare(another: Unbounded[X]) = (this, another) match {
    case (Finite(thisUnlifted), Finite(anotherUnlifted)) => thisUnlifted compare anotherUnlifted
    case (NegativeInfinity(), Finite(_))                 => -1
    case (Finite(_), NegativeInfinity())                 => 1
    case (NegativeInfinity(), NegativeInfinity())        => 0
  }
}

case class Finite[X <% Ordered[X]](unlifted: X) extends Unbounded[X] {

}

case class NegativeInfinity[X <% Ordered[X]]() extends Unbounded[X]

object NegativeInfinity {
  implicit def bridgeToAllCandidateTypes[X <% Ordered[X]](ni: this.type) = new NegativeInfinity()
  implicit def fallbackBridge(ni: this.type) = new NegativeInfinity[Nothing]()
}

class TestSuite extends Suite {

  def testOperations() = {
    val negativeInfinity = NegativeInfinity

    val twentyThree = Finite(23)

    assert(negativeInfinity < twentyThree)

    val fortyFive = Finite(45)

    assert(twentyThree < fortyFive)

    assert(!(twentyThree > fortyFive))

    assert(Finite(23) == twentyThree)

    assert(NegativeInfinity < Finite(45))

    assert(Finite(45) > NegativeInfinity)

    assert(NegativeInfinity == NegativeInfinity)

    assert(!(NegativeInfinity[Int]() > NegativeInfinity()))
    assert(!(NegativeInfinity > NegativeInfinity[Nothing]()))
    assert(NegativeInfinity <= NegativeInfinity[Nothing])
    
    assert(NegativeInfinity < twentyThree)
  }
}

object Main extends App {
  (new TestSuite).execute()
}

