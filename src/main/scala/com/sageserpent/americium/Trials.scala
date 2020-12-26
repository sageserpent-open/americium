package com.sageserpent.americium

object Trials {
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
      step: Case => Trials[TransformedCase]): Trials[TransformedCase] =
    ???

  def filter(predicate: Case => Boolean): Trials[Case] = ???

  def supplyTo(consumer: Case => Unit): Unit = ???

  def reproduce(recipe: String): Case

  def supplyTo(recipe: String, consumer: Case => Unit): Unit = ???
}
