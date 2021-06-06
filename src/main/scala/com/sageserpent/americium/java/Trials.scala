package com.sageserpent.americium.java

import _root_.com.sageserpent.americium.{
  TrialsImplementation,
  Trials => ScalaTrials
}
import com.sageserpent.americium.java.Trials.WithLimit

import _root_.java.util.function.{Consumer, Predicate, Function => JavaFunction}
import java.util.Optional

object Trials {
  def api(): TrialsApi = TrialsImplementation.javaApi

  trait WithLimit[+Case] {

    /** Consume trial cases until either there are no more or an exception is thrown by {@code consumer}.
      * If an exception is thrown, attempts will be made to shrink the trial case that caused the
      * exception to a simpler case that throws an exception - the specific kind of exception isn't
      * necessarily the same between the first exceptional case and the final simplified one. The exception
      * from the simplified case (or the original exceptional case if it could not be simplified) is wrapped
      * in an instance of {@link TrialException} which also contains the case that provoked the exception.
      *
      * @param consumer An operation that consumes a {@code Case}, and may throw an exception.
      * @note The limit applies to the count of the number of supplied
      *       cases, regardless of whether some of these cases are
      *       duplicated or not. There is no guarantee that all of
      *       the non-duplicated cases have to be supplied, even if
      *       they could potentially all fit within the limit.
      */
    def supplyTo(consumer: Consumer[_ >: Case]): Unit
  }
}

trait Trials[+Case] extends TrialsFactoring[Case] {
  private[americium] val scalaTrials: ScalaTrials[Case]

  def map[TransformedCase](
      transform: JavaFunction[_ >: Case, TransformedCase]
  ): Trials[TransformedCase]

  def flatMap[TransformedCase](
      step: JavaFunction[_ >: Case, Trials[TransformedCase]]
  ): Trials[TransformedCase]

  def filter(predicate: Predicate[_ >: Case]): Trials[Case]

  def mapFilter[TransformedCase](
      filteringTransform: JavaFunction[_ >: Case, Optional[
        TransformedCase
      ]]
  ): Trials[TransformedCase]

  /** Fluent syntax for configuring a limit to the number of cases
    * supplied to a consumer.
    *
    * @param limit
    * @return An instance of {@link WithLimit} with the limit configured.
    */
  def withLimit(limit: Int): WithLimit[Case]

  /** Consume the single trial case reproduced by a recipe. This is intended
    * to repeatedly run a test against a known failing case when debugging, so
    * the expectation is for this to *eventually* not throw an exception after
    * code changes are made in the system under test.
    *
    * @param recipe   This encodes a specific {@code Case} and will only be understood by the
    *                 same *value* of {@link Trials} that was used to obtain it.
    * @param consumer An operation that consumes a {@code Case}, and may throw an exception.
    * @throws RuntimeException if the recipe is not one corresponding to the receiver,
    *                          either due to it being created by a different flavour
    *                          of {@link Trials} instance.
    */
  def supplyTo(recipe: String, consumer: Consumer[_ >: Case]): Unit
}
