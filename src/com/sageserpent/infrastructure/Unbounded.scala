package com.sageserpent.infrastructure

import scala.math.Ordered

import org.scalatest.Suite

abstract class Unbounded[X <% Ordered[X]] extends Ordered[Unbounded[X]] {
  def compare(another: Unbounded[X]) = (this, another) match {
    case (Finite(thisUnlifted), Finite(anotherUnlifted)) => thisUnlifted compare anotherUnlifted
    case (Foo(), Foo())        => 0
    case (Foo(), _)                 => -1
    case (_, Foo())                 => 1
  }
}

case class Finite[X <% Ordered[X]](unlifted: X) extends Unbounded[X] {
}

case class Foo[X <% Ordered[X]]() extends Unbounded[X] {
  implicit def fakeCovarianceHack[Y <% Ordered[Y]](ni: this.type) = Foo[Y]()
}

object NegativeInfinity extends Foo[Nothing] {
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

    assert(negativeInfinity < fortyFive)
    
    //assert(negativeInfinity < Finite(45))  // COMPILE ERROR WHEN UNCOMMENTED: Huh?
    
    assert(NegativeInfinity < fortyFive)

    //assert(NegativeInfinity < Finite(45))  // COMPILE ERROR WHEN UNCOMMENTED: Huh?
    
    assert(Finite(45) > NegativeInfinity)

    assert(NegativeInfinity == NegativeInfinity)

    assert(!(Foo[Int]() > Foo()))
    assert(!(NegativeInfinity > Foo[Nothing]()))
    assert(NegativeInfinity <= Foo[Nothing])
    assert(Foo[Nothing] <= negativeInfinity)
    assert(negativeInfinity <= Foo[Nothing])
    assert(NegativeInfinity <= NegativeInfinity)
    assert(!(NegativeInfinity > NegativeInfinity))
    
    assert(NegativeInfinity < twentyThree)
  }
}

object Main extends App {
  (new TestSuite).execute()
}

