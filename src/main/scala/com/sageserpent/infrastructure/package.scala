package com.sageserpent

import scala.math.Ordered

package object infrastructure  {
  object listEnrichment extends ListEnrichment

  object randomEnrichment extends RandomEnrichment

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