package com.sageserpent.americium

object Unbounded {
  implicit def ordering[X: Ordering]: Ordering[Unbounded[X]] =
    (first: Unbounded[X], second: Unbounded[X]) =>
      (first, second) match {
        case (Finite(thisUnlifted), Finite(anotherUnlifted)) =>
          Ordering[X].compare(thisUnlifted, anotherUnlifted)
        case (PositiveInfinity, PositiveInfinity) => 0
        case (NegativeInfinity, NegativeInfinity) => 0
        case (_, PositiveInfinity)                => -1
        case (NegativeInfinity, _)                => -1
        case (PositiveInfinity, _)                => 1
        case (_, NegativeInfinity)                => 1
      }

  implicit val negativeInfinityOrdering: Ordering[NegativeInfinity.type] =
    (one: NegativeInfinity.type, andTheSame: NegativeInfinity.type) => 0
  implicit val positiveInfinityOrdering: Ordering[PositiveInfinity.type] =
    (one: PositiveInfinity.type, andTheSame: PositiveInfinity.type) => 0

  implicit def finiteOrdering[X: Ordering]: Ordering[Finite[X]] =
    (first: Finite[X], second: Finite[X]) =>
      Ordering[X].compare(first.unlifted, second.unlifted)
}

sealed trait Unbounded[+X]

case class Finite[X: Ordering](unlifted: X) extends Unbounded[X]

case object NegativeInfinity extends Unbounded[Nothing]

case object PositiveInfinity extends Unbounded[Nothing]
