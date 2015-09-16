package com.sageserpent.infrastructure

import scala.math.Ordered

object Unbounded {
  implicit def convertToOrdered[X](unbounded: Unbounded[X])(implicit evidence: X => Ordered[X]): Ordered[Unbounded[X]] = new Ordered[Unbounded[X]]{
    override def compare(another: Unbounded[X]): Int = (unbounded, another) match {
      case (Finite(thisUnlifted), Finite(anotherUnlifted)) => thisUnlifted compare anotherUnlifted
      case (PositiveInfinity(), PositiveInfinity())        => 0
      case (NegativeInfinity(), NegativeInfinity())        => 0
      case (_, PositiveInfinity())                         => -1
      case (NegativeInfinity(), _)                         => -1
      case (PositiveInfinity(), _)                         => 1
      case (_, NegativeInfinity())                         => 1
    }
  }
}

abstract class Unbounded[X] {
}

case class Finite[X](unlifted: X) extends Unbounded[X] {
}

case class NegativeInfinity[X]() extends Unbounded[X] {

}

case class PositiveInfinity[X]() extends Unbounded[X] {
}

