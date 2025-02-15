package com.sageserpent.americium
import magnolia1.{AutoDerivation, CaseClass, Monadic, SealedTrait}

trait Factory[Case] {
  def trials: Trials[Case]
}

object Factory extends AutoDerivation[Factory] {
  def join[Case](caseClass: CaseClass[Typeclass, Case]): Typeclass[Case] =
    lift(
      caseClass.constructMonadic(parameter =>
        Trials.api.delay(parameter.typeclass.trials)
      )
    )

  given Factory[Int]     = lift(Trials.api.integers)
  given Factory[Double]  = lift(Trials.api.doubles)
  given Factory[Long]    = lift(Trials.api.longs)
  given Factory[Boolean] = lift(Trials.api.booleans)

  def split[Case](
      sealedTrait: SealedTrait[Typeclass, Case]
  ): Typeclass[Case] = {
    val subtypeGenerators: Seq[Trials[Case]] =
      sealedTrait.subtypes.map(_.typeclass.trials)
    lift(Trials.api.alternate(subtypeGenerators))
  }

  // HACK: had to write an explicit implicit implementation to supply an
  // `apply` method to implement `point`, which `Trials` does not provide.
  given evidence: Monadic[Trials] = new Monadic[Trials] {
    override def flatMap[A, B](from: Trials[A])(
        fn: A => Trials[B]
    ): Trials[B] =
      from.flatMap(fn)

    override def point[A](value: A): Trials[A] = Trials.api.only(value)

    override def map[A, B](from: Trials[A])(fn: A => B): Trials[B] =
      from.map(fn)
  }

  def lift[Case](unlifted: Trials[Case]): Factory[Case] = new Factory[Case] {
    override def trials: Trials[Case] = unlifted
  }
}
