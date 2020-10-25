package com.sageserpent.americium

import _root_.java.util.function.{Consumer, Predicate, Function => JavaFunction}

import com.sageserpent.americium.java.{Trials => JavaTrials}

object Trials {
  def constant[SomeCase](value: SomeCase): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  def choose[SomeCase](choices: SomeCase*): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  def choose[SomeCase](choices: Iterable[SomeCase]): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  def alternate[SomeCase](alternatives: Trials[SomeCase]*): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  def alternate[SomeCase](
      alternatives: Iterable[Trials[SomeCase]]): Trials[SomeCase] = {
    throw new NotImplementedError
  }
}

trait Trials[Case] extends JavaTrials[Case] {
  def map[TransformedCase](
      transform: Case => TransformedCase): Trials[TransformedCase] =
    map(transform.apply _: JavaFunction[Case, TransformedCase])

  def map[TransformedCase](
      transform: JavaFunction[Case, TransformedCase]): Trials[TransformedCase] =
    ???

  def flatMap[TransformedCase](
      step: Case => Trials[TransformedCase]): Trials[TransformedCase] =
    flatMap(step.apply _: JavaFunction[Case, JavaTrials[TransformedCase]])

  def flatMap[TransformedCase](
      step: JavaFunction[Case, JavaTrials[TransformedCase]])
    : Trials[TransformedCase] = ???

  def filter(predicate: Case => Boolean): Trials[Case] =
    filter(predicate.apply _: Predicate[Case])

  def filter(predicate: Predicate[Case]): Trials[Case] = ???

  def supplyTo(consumer: Case => Unit): Unit =
    supplyTo(consumer.apply _: Consumer[Case])

  def supplyTo(consumer: Consumer[_ >: Case]): Unit = ???

  def reproduce(recipe: String): Case

  def supplyTo(recipe: String, consumer: Case => Unit): Unit =
    supplyTo(recipe, consumer.apply _: Consumer[Case])

  def supplyTo(recipe: String, consumer: Consumer[_ >: Case]): Unit = ???
}
