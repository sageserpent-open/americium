package com.sageserpent.americium

import com.sageserpent.americium.java.TrialsApi

import _root_.java.lang.{Iterable => JavaIterable}
import _root_.java.util.function.{Consumer, Predicate, Function => JavaFunction}
import scala.collection.JavaConverters._

object Trials extends TrialsApi {
  // Scala-only API ...
  def choose[SomeCase](choices: Iterable[SomeCase]): Trials[SomeCase] =
    throw new NotImplementedError

  def alternate[SomeCase](
      alternatives: Iterable[Trials[SomeCase]]): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  // Java/Scala API ...

  def only[SomeCase](onlyCase: SomeCase): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  def choose[SomeCase](firstChoice: SomeCase,
                       secondChoice: SomeCase,
                       otherChoices: SomeCase*): Trials[SomeCase] =
    choose(firstChoice +: secondChoice +: otherChoices)

  def choose[SomeCase](choices: JavaIterable[SomeCase]): Trials[SomeCase] =
    choose(choices.asScala)

  def choose[SomeCase](choices: Array[SomeCase with AnyRef]): Trials[SomeCase] =
    choose(choices.toSeq)

  def alternate[SomeCase](
      firstAlternative: com.sageserpent.americium.Trials[_ <: SomeCase],
      secondAlternative: com.sageserpent.americium.Trials[_ <: SomeCase],
      otherAlternatives: com.sageserpent.americium.Trials[_ <: SomeCase]*)
    : com.sageserpent.americium.Trials[SomeCase] =
    alternate(
      firstAlternative +: secondAlternative +: Seq(otherAlternatives: _*))

  def alternate[SomeCase](
      alternatives: JavaIterable[Trials[SomeCase]]): Trials[SomeCase] =
    alternate(alternatives.asScala)

  def alternate[SomeCase](
      alternatives: Array[Trials[SomeCase]]): Trials[SomeCase] =
    alternate(alternatives.toSeq)
}

trait Trials[+Case] {
  // Scala-only API ...

  def map[TransformedCase](
      transform: Case => TransformedCase): Trials[TransformedCase] = ???

  def flatMap[TransformedCase](
      step: Case => Trials[TransformedCase]): Trials[TransformedCase] = ???

  def filter(predicate: Case => Boolean): Trials[Case] = ???

  def supplyTo(consumer: Case => Unit): Unit

  def supplyTo(recipe: String, consumer: Case => Unit): Unit =
    consumer(reproduce(recipe))

  // Java/Scala API ...

  def map[TransformedCase](transform: JavaFunction[_ >: Case, TransformedCase])
    : Trials[TransformedCase] = map(transform.apply _)

  def flatMap[TransformedCase](
      step: JavaFunction[_ >: Case, Trials[TransformedCase]])
    : Trials[TransformedCase] = flatMap(step.apply _)

  def filter(predicate: Predicate[_ >: Case]): Trials[Case] =
    filter(predicate.test _)

  abstract class TrialException extends RuntimeException {

    /**
      * @return The case that provoked the exception.
      */
    def provokingCase: Case

    /**
      * @return A recipe that can be used to reproduce the provoking case
      *         when supplied to the corresponding trials instance.
      */
    def recipe: String
  }

  /**
    * Consume trial cases until either there are no more or an exception is thrown by {@code consumer}.
    * If an exception is thrown, attempts will be made to shrink the trial case that caused the
    * exception to a simpler case that throws an exception - the specific kind of exception isn't
    * necessarily the same between the first exceptional case and the final simplified one. The exception
    * from the simplified case (or the original exceptional case if it could not be simplified) is wrapped
    * in an instance of {@link TrialException} which also contains the case that provoked the exception.
    *
    * @param consumer An operation that consumes a 'Case', and may throw an exception.
    */
  def supplyTo(consumer: Consumer[_ >: Case]): Unit =
    supplyTo(consumer.accept _)

  /**
    * Reproduce a specific case in a repeatable fashion, based on a recipe.
    *
    * @param recipe This encodes a specific case and will only be understood by the
    *               same *value* of trials instance that was used to obtain it.
    * @return The specific case denoted by the recipe.
    * @throws RuntimeException if the recipe does not correspond to the receiver,
    *                          either due to it being created by a different
    *                          flavour of trials instance or subsequent code changes.
    */
  def reproduce(recipe: String): Case

  /**
    * Consume the single trial case reproduced by a recipe. This is intended
    * to repeatedly run a test against a known failing case when debugging, so
    * the expectation is for this to *eventually* not throw an exception after
    * code changes are made in the system under test.
    *
    * @param recipe   This encodes a specific case and will only be understood by the
    *                 same *value* of trials instance that was used to obtain it.
    * @param consumer An operation that consumes a 'Case', and may throw an exception.
    * @throws RuntimeException if the recipe is not one corresponding to the receiver,
    *                          either due to it being created by a different flavour of trials instance.
    */
  def supplyTo(recipe: String, consumer: Consumer[_ >: Case]): Unit =
    supplyTo(recipe, consumer.accept _)
}
