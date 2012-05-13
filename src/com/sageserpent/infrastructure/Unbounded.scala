package com.sageserpent.infrastructure

import scala.math.Ordered


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
