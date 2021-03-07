package com.sageserpent.americium

import cats._
import com.sageserpent.americium.Trials.WithLimit
import com.sageserpent.americium.TrialsImplementation.GenerationSupport
import com.sageserpent.americium.java.TrialsFactoring

import _root_.java.lang.{Double => JavaDouble}
import scala.language.implicitConversions

object Trials {
  def api: TrialsApi = TrialsImplementation

  implicit val monadInstance: Monad[Trials] = new Monad[Trials]
  with StackSafeMonad[Trials] {
    override def pure[A](x: A): Trials[A] = api.only(x)

    override def flatMap[A, B](fa: Trials[A])(f: A => Trials[B]): Trials[B] =
      fa.flatMap(f)
  }

  implicit val functorFilterInstance: FunctorFilter[Trials] =
    new FunctorFilter[Trials] {
      override def functor: Functor[Trials] = monadInstance

      override def mapFilter[A, B](fa: Trials[A])(
          f: A => Option[B]): Trials[B] = fa.mapFilter(f)
    }

  trait WithLimit[+Case] {
    def supplyTo(consumer: Case => Unit): Unit
  }

  def integers: Trials[Integer] =
    TrialsImplementation.stream((input: Long) => Int.box(input.hashCode()))

  def longs: Trials[Long] = TrialsImplementation.stream(identity[Long] _)

  def doubles: Trials[Double] =
    TrialsImplementation.stream(JavaDouble.longBitsToDouble _)

  def trueOrFalse: Trials[Boolean] =
    TrialsImplementation.choose(true, false)

  def coinFlip: Trials[Boolean] =
    TrialsImplementation.stream(0 == (_: Long) % 2)
}

trait Trials[+Case] extends TrialsFactoring[Case] with GenerationSupport[Case] {
  def map[TransformedCase](
      transform: Case => TransformedCase): Trials[TransformedCase]

  def flatMap[TransformedCase](
      step: Case => Trials[TransformedCase]): Trials[TransformedCase]

  def filter(predicate: Case => Boolean): Trials[Case]

  def mapFilter[TransformedCase](
      filteringTransform: Case => Option[TransformedCase])
    : Trials[TransformedCase]

  def withLimit(limit: Int): WithLimit[Case]

  def supplyTo(recipe: String, consumer: Case => Unit): Unit
}
