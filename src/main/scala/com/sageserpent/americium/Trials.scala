package com.sageserpent.americium

import cats._
import cats.implicits._
import com.sageserpent.americium.Trials.SupplyToSyntax
import com.sageserpent.americium.TrialsImplementation.GenerationSupport
import com.sageserpent.americium.java.TrialsFactoring

import scala.collection.Factory
import scala.collection.immutable.{SortedMap, SortedSet}
import scala.language.implicitConversions

object Trials extends TrialsByMagnolia {
  def api: TrialsApi = TrialsImplementation.scalaApi

  private[americium] class RejectionByInlineFilter extends RuntimeException

  def whenever[Result](satisfiedPrecondition: Boolean)(
      block: => Result
  ): Result =
    if (satisfiedPrecondition) block else throw new RejectionByInlineFilter()

  trait SupplyToSyntax[+Case] {
    def supplyTo(consumer: Case => Unit): Unit
  }

  trait SupplyToSyntaxTuple2[+Case1, +Case2]
      extends SupplyToSyntax[(Case1, Case2)] {
    def supplyTo(consumer: (Case1, Case2) => Unit): Unit = supplyTo(
      consumer.tupled
    )
  }

  trait SupplyToSyntaxTuple3[+Case1, +Case2, +Case3]
      extends SupplyToSyntax[(Case1, Case2, Case3)] {
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
    def withLimit(limit: Int): SupplyToSyntaxTuple2[Case1, Case2] =
      new SupplyToSyntaxTuple2[Case1, Case2] {
        val withLimit = pair.mapN(Tuple2.apply).withLimit(limit)

        override def supplyTo(consumer: ((Case1, Case2)) => Unit): Unit =
          withLimit.supplyTo(consumer)
      }
  }

  implicit class tuple3Trials[+Case1, +Case2, +Case3](
      val triple: (Trials[Case1], Trials[Case2], Trials[Case3])
  ) extends AnyVal {
    def withLimit(limit: Int): SupplyToSyntaxTuple3[Case1, Case2, Case3] =
      new SupplyToSyntaxTuple3[Case1, Case2, Case3] {
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

  def withLimit(limit: Int): SupplyToSyntax[Case]

  def withRecipe(recipe: String): SupplyToSyntax[Case]

  def several[Container](implicit
      factory: Factory[Case, Container]
  ): Trials[Container]

  def lists: Trials[List[Case]]

  def sets: Trials[Set[_ <: Case]]

  def sortedSets(implicit
      ordering: Ordering[_ >: Case]
  ): Trials[SortedSet[_ <: Case]]

  def maps[Value](values: Trials[Value]): Trials[Map[_ <: Case, Value]]

  def sortedMaps[Value](values: Trials[Value])(implicit
      ordering: Ordering[_ >: Case]
  ): Trials[SortedMap[_ <: Case, Value]]

  def lotsOfSize[Container](size: Int)(implicit
      factory: Factory[Case, Container]
  ): Trials[Container]

  def listsOfSize(size: Int): Trials[List[Case]]
}
