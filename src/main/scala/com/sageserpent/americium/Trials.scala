package com.sageserpent.americium

import cats._
import cats.implicits._
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

  trait WithLimitTuple2[+Case1, +Case2] extends WithLimit[(Case1, Case2)] {
    def supplyTo(consumer: (Case1, Case2) => Unit): Unit = supplyTo(
      consumer.tupled
    )
  }

  trait WithLimitTuple3[+Case1, +Case2, +Case3]
      extends WithLimit[(Case1, Case2, Case3)] {
    def supplyTo(consumer: (Case1, Case2, Case3) => Unit): Unit = supplyTo(
      consumer.tupled
    )
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

  implicit class tuple2Trials[+Case1, +Case2](
      val pair: (Trials[Case1], Trials[Case2])
  ) extends AnyVal {
    def withLimit(limit: Int): WithLimitTuple2[Case1, Case2] =
      new WithLimitTuple2[Case1, Case2] {
        val withLimit = pair.mapN(Tuple2.apply).withLimit(limit)

        override def supplyTo(consumer: ((Case1, Case2)) => Unit): Unit =
          withLimit.supplyTo(consumer)
      }
  }

  implicit class tuple3Trials[+Case1, +Case2, +Case3](
      val triple: (Trials[Case1], Trials[Case2], Trials[Case3])
  ) extends AnyVal {
    def withLimit(limit: Int): WithLimitTuple3[Case1, Case2, Case3] =
      new WithLimitTuple3[Case1, Case2, Case3] {
        val withLimit = triple.mapN(Tuple3.apply).withLimit(limit)

        override def supplyTo(consumer: ((Case1, Case2, Case3)) => Unit): Unit =
          withLimit.supplyTo(consumer)
      }
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

  def lists: Trials[List[Case]] = several
}
