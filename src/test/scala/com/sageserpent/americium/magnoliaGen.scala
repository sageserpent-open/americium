package com.sageserpent.americium

import hedgehog.core.{DiscardCount, PropertyConfig, Seed, ShrinkLimit, SuccessCount}
import hedgehog.{Gen, Property, Range, Result}
import magnolia._
import mercator._

import scala.language.experimental.macros

object magnoliaGen {
  type Typeclass[T] = Gen[T]

  // HACK: had to write an explicit implicit implementation
  // as Mercator would need an 'apply' method to implement 'point',
  // which 'Gen' does not provide.
  implicit def evidence: Monadic[Gen] = new Monadic[Typeclass] {
    override def flatMap[A, B](from: Typeclass[A])(
        fn: A => Typeclass[B]): Typeclass[B] = from.flatMap(fn)

    override def point[A](value: A): Typeclass[A] = Gen.constant(value)

    override def map[A, B](from: Typeclass[A])(fn: A => B): Typeclass[B] =
      from.map(fn)
  }
  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] = {
    // This is where I decided that the nominally square peg would indeed
    // fit snugly into the round hole ... and lo, it did!
    caseClass.constructMonadic[Gen, Any](parameter =>
      parameter.typeclass.asInstanceOf[Gen[Any]])
  }
  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???
  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]
}

object testBed extends App {
  implicit val ints: Gen[Int]       = Gen.int(Range.linear(1, 45))
  implicit val strings: Gen[String] = Gen.string(Gen.alpha, Range.linear(1, 35))

  val tuples: Gen[Tuple2[Int, String]] = magnoliaGen.gen

  val configuration: PropertyConfig =
    PropertyConfig(SuccessCount(20000), DiscardCount(200), ShrinkLimit(20000))

  val report = Property.check(
    configuration,
    tuples.forAll
      .map { case (int, string) => println(int, string) }
      .map(_ => Result.success),
    Seed.fromLong(1L)
  )
}
