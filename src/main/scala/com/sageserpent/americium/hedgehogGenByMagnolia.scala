package com.sageserpent.americium

import hedgehog.Gen
import magnolia.{CaseClass, Magnolia, SealedTrait}
import mercator.Monadic

import scala.language.experimental.macros

object hedgehogGenByMagnolia {
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
    caseClass.constructMonadic[Typeclass, T](
      parameter =>
        // HACK: 'Gen' should be covariant in its type parameter,
        // but at time of writing this, it is invariant - so use
        // a type cast as a workaround.
        parameter.typeclass.asInstanceOf[Typeclass[T]])

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = {
    val subtypeGenerators: Seq[Typeclass[T]] =
      // HACK: 'Gen' should be covariant in its type parameter,
      // but at time of writing this, it is invariant - so use
      // a type cast as a workaround.
      sealedTrait.subtypes.map(_.typeclass.asInstanceOf[Typeclass[T]])
    Gen.choice(subtypeGenerators.head, subtypeGenerators.tail.toList)
  }

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]
}
