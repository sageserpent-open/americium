package com.sageserpent

import scala.math.Ordered
import scala.util.Random
import scala.collection.immutable.List

package object infrastructure {
  implicit def enrich(random: Random) = new RichRandom(random)
  
  implicit def enrich[X](list: List[X]) = new RichList(list)

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