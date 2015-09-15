package com.sageserpent.infrastructure


abstract class Unbounded[X] {
}

case class Finite[X](unlifted: X) extends Unbounded[X] {
}

case class NegativeInfinity[X]() extends Unbounded[X] {

}

case class PositiveInfinity[X]() extends Unbounded[X] {
}

