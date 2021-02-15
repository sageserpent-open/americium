package com.sageserpent.americium

import cats._
import com.sageserpent.americium.java.{Trials => JavaTrials}

import _root_.java.util.Optional
import _root_.java.util.function.{Consumer, Predicate, Function => JavaFunction}
import scala.language.implicitConversions

object Trials extends TrialsJavaScalaFusionApi {
  override def api: TrialsJavaScalaFusionApi = this

  implicit val monadInstance: Monad[Trials] = new Monad[Trials]
  with StackSafeMonad[Trials] {
    override def pure[A](x: A): Trials[A] = Trials.only(x)

    override def flatMap[A, B](fa: Trials[A])(f: A => Trials[B]): Trials[B] =
      fa.flatMap(f)
  }

  implicit val functorFilterInstance: FunctorFilter[Trials] =
    new FunctorFilter[Trials] {
      override def functor: Functor[Trials] = monadInstance

      override def mapFilter[A, B](fa: Trials[A])(
          f: A => Option[B]): Trials[B] = fa.mapFilter(f)
    }
}

trait Trials[+Case] extends JavaTrials[Case] {
  // Scala-only API ...

  def map[TransformedCase](
      transform: Case => TransformedCase): Trials[TransformedCase]

  def flatMap[TransformedCase](
      step: Case => Trials[TransformedCase]): Trials[TransformedCase]

  def filter(predicate: Case => Boolean): Trials[Case]

  def mapFilter[TransformedCase](
      filteringTransform: Case => Option[TransformedCase])
    : Trials[TransformedCase]

  def supplyTo(consumer: Case => Unit): Unit

  def supplyTo(recipe: String, consumer: Case => Unit): Unit =
    consumer(reproduce(recipe))

  // Java API ...

  override def map[TransformedCase](
      transform: JavaFunction[_ >: Case, TransformedCase])
    : Trials[TransformedCase] = map(transform.apply _)

  override def flatMap[TransformedCase](
      step: JavaFunction[_ >: Case, JavaTrials[TransformedCase]])
    : Trials[TransformedCase] = flatMap(step.apply _ andThen (_.scalaTrials))

  override def filter(predicate: Predicate[_ >: Case]): Trials[Case] =
    filter(predicate.test _)

  def mapFilter[TransformedCase](
      filteringTransform: JavaFunction[_ >: Case, Optional[TransformedCase]])
    : Trials[TransformedCase] =
    mapFilter(filteringTransform.apply _ andThen {
      case withPayload if withPayload.isPresent => Some(withPayload.get())
      case _                                    => None
    })

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
  override def supplyTo(consumer: Consumer[_ >: Case]): Unit =
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
    *                          either due to it being created by a different flavour
    *                          of trials instance.
    */
  override def supplyTo(recipe: String, consumer: Consumer[_ >: Case]): Unit =
    supplyTo(recipe, consumer.accept _)

  override private[americium] val scalaTrials = this
}
