package com.sageserpent.americium

import com.sageserpent.americium.java.{Trials => JavaTrials}

import _root_.java.util.function.{Consumer, Predicate, Function => JavaFunction}

object Trials {
  implicit class JavaTrialsSyntax[Case](val trials: Trials[Case])
      extends JavaTrials[Case] {
    override def map[TransformedCase](
        transform: JavaFunction[_ >: Case, TransformedCase])
      : JavaTrials[TransformedCase] =
      new JavaTrialsSyntax(trials.map(transform.apply))

    override def flatMap[TransformedCase](
        step: JavaFunction[_ >: Case, JavaTrials[TransformedCase]])
      : JavaTrials[TransformedCase] =
      new JavaTrialsSyntax(trials.flatMap(step.apply _ andThen (javaResult =>
        new Trials[TransformedCase] {
          override def supplyTo(consumer: TransformedCase => Unit): Unit =
            javaResult.supplyTo(consumer.apply)
          override def reproduce(recipe: String): TransformedCase =
            javaResult.reproduce(recipe)
        })))

    override def filter(predicate: Predicate[_ >: Case]): JavaTrials[Case] =
      new JavaTrialsSyntax(trials.filter(predicate.test))

    override def supplyTo(consumer: Consumer[_ >: Case]): Unit =
      trials.supplyTo(consumer)

    override def reproduce(recipe: String): Case = trials.reproduce(recipe)

    override def supplyTo(recipe: String, consumer: Consumer[_ >: Case]): Unit =
      trials.supplyTo(recipe, consumer)
  }

  def only[SomeCase](onlyCase: SomeCase): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  def choose[SomeCase](firstChoice: SomeCase,
                       secondChoice: SomeCase,
                       otherChoices: SomeCase*): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  def choose[SomeCase](choices: Iterable[SomeCase]) =
    throw new NotImplementedError

  def alternate[SomeCase](
      firstAlternative: Trials[SomeCase],
      secondAlternative: Trials[SomeCase],
      otherAlternatives: Trials[SomeCase]*): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  def alternate[SomeCase](
      alternatives: Iterable[Trials[SomeCase]]): Trials[SomeCase] = {
    throw new NotImplementedError
  }
}

trait Trials[+Case] {
  def map[TransformedCase](
      transform: Case => TransformedCase): Trials[TransformedCase] = ???

  def flatMap[TransformedCase](
      step: Case => Trials[TransformedCase]): Trials[TransformedCase] = ???

  def filter(predicate: Case => Boolean): Trials[Case] = ???

  def supplyTo(consumer: Case => Unit): Unit

  def reproduce(recipe: String): Case

  def supplyTo(recipe: String, consumer: Case => Unit): Unit =
    consumer(reproduce(recipe))
}
