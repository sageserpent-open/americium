package com.sageserpent.americium

import cats._
import com.sageserpent.americium.Trials.WithLimit
import com.sageserpent.americium.TrialsImplementation.GenerationSupport
import com.sageserpent.americium.java.TrialsFactoring

import scala.collection.Factory
import scala.language.implicitConversions

object Trials extends TrialsByMagnolia {
  def api: TrialsApi = TrialsImplementation.scalaApi

  trait WithLimit[+Case] {
    def supplyTo(consumer: Case => Unit): Unit
  }

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
          f: A => Option[B]
      ): Trials[B] = fa.mapFilter(f)
    }
}

trait Trials[+Case] extends TrialsFactoring[Case] with GenerationSupport[Case] {
  def map[TransformedCase](
      transform: Case => TransformedCase
  ): Trials[TransformedCase]

  def flatMap[TransformedCase](
      step: Case => Trials[TransformedCase]
  ): Trials[TransformedCase]

  def filter(predicate: Case => Boolean): Trials[Case]

  def mapFilter[TransformedCase](
      filteringTransform: Case => Option[TransformedCase]
  ): Trials[TransformedCase]

  def withLimit(limit: Int): WithLimit[Case]

  def supplyTo(recipe: String, consumer: Case => Unit): Unit

  def several[Container](implicit
      factory: Factory[Case, Container]
  ): Trials[Container]
}
