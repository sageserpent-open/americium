package com.sageserpent.americium

import hedgehog.core._
import hedgehog.{Gen, Property, Range, Result}
import magnolia._
import mercator.Monadic

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

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    // TODO - use a wildcard capture or something to work in hand with
    // the Scala type system.
    caseClass.constructMonadic[Gen, Any](parameter =>
      parameter.typeclass.asInstanceOf[Gen[Any]])

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = {
    // TODO - use a wildcard capture or something to work in hand with
    // the Scala type system.
    val subtypeGenerators: Seq[Typeclass[T]] =
      sealedTrait.subtypes.map(_.typeclass).asInstanceOf[Seq[Typeclass[T]]]
    Gen.choice(subtypeGenerators.head, subtypeGenerators.tail.toList)
  }

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]
}

object testBed extends App {
  implicit val ints: Gen[Int]       = Gen.int(Range.linear(1, 45))
  implicit val strings: Gen[String] = Gen.string(Gen.alpha, Range.linear(1, 35))

  val eithers: Gen[Either[Int, String]] = magnoliaGen.gen

  val configuration: PropertyConfig =
    PropertyConfig(SuccessCount(20000), DiscardCount(200), ShrinkLimit(20000))

  val report = Property.check(
    configuration,
    eithers.forAll
      .map(println)
      .map(_ => Result.success),
    Seed.fromLong(1L)
  )
}
