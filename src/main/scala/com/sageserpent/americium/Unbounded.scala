package com.sageserpent.americium
import scala.language.implicitConversions

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

  implicit def orderedSyntaxFor[X: Ordering](
      unbounded: Unbounded[X]
  ): Ordered[Unbounded[X]] =
    scala.math.Ordered.orderingToOrdered[Unbounded[X]](unbounded)
}

sealed trait Unbounded[+X]

case class Finite[X: Ordering](unlifted: X)
    extends Unbounded[X]
    with Ordered[Unbounded[X]] {
  override def compare(that: Unbounded[X]): Int =
    Unbounded.ordering.compare(this, that)
}

case object NegativeInfinity
    extends Unbounded[Nothing]
    with Ordered[Unbounded[_ <: Any]] {
  override def compare(that: Unbounded[_]): Int = that match {
    case NegativeInfinity => 0
    case _                => -1
  }
}

case object PositiveInfinity
    extends Unbounded[Nothing]
    with Ordered[Unbounded[_ <: Any]] {
  override def compare(that: Unbounded[_]): Int = that match {
    case PositiveInfinity => 0
    case _                => 1
  }
}
