package com.sageserpent.americium

import cats._
import com.sageserpent.americium.Trials.SupplyToSyntax
import com.sageserpent.americium.java.TrialsImplementation.GenerationSupport
import com.sageserpent.americium.java.{TrialsFactoring, TrialsImplementation}

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

  trait SupplyToSyntax[+Case] {
    def supplyTo(consumer: Case => Unit): Unit
  }

  trait SupplyToSyntaxTuple2[+Case1, +Case2]
      extends SupplyToSyntax[(Case1, Case2)] {
    def supplyTo(consumer: (Case1, Case2) => Unit): Unit
  }

  trait SupplyToSyntaxTuple3[+Case1, +Case2, +Case3]
      extends SupplyToSyntax[(Case1, Case2, Case3)] {
    def supplyTo(consumer: (Case1, Case2, Case3) => Unit): Unit
  }

  trait SupplyToSyntaxTuple4[+Case1, +Case2, +Case3, +Case4]
      extends SupplyToSyntax[(Case1, Case2, Case3, Case4)] {
    def supplyTo(consumer: (Case1, Case2, Case3, Case4) => Unit): Unit
  }

  trait Tuple2Trials[+Case1, +Case2] {
    def and[Case3](
        thirdTrials: Trials[Case3]
    ): Tuple3Trials[Case1, Case2, Case3]

    def withLimit(limit: Int): SupplyToSyntaxTuple2[Case1, Case2]

    def withRecipe(recipe: String): SupplyToSyntaxTuple2[Case1, Case2]
  }

  trait Tuple3Trials[+Case1, +Case2, +Case3] {
    def and[Case4](
        fourthTrials: Trials[Case4]
    ): Tuple4Trials[Case1, Case2, Case3, Case4]

    def withLimit(limit: Int): SupplyToSyntaxTuple3[Case1, Case2, Case3]

    def withRecipe(recipe: String): SupplyToSyntaxTuple3[Case1, Case2, Case3]
  }

  trait Tuple4Trials[+Case1, +Case2, +Case3, +Case4] {
    def withLimit(
        limit: Int
    ): SupplyToSyntaxTuple4[Case1, Case2, Case3, Case4]

    def withRecipe(
        recipe: String
    ): SupplyToSyntaxTuple4[Case1, Case2, Case3, Case4]
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

  def and[Case2](secondTrials: Trials[Case2]): Trials.Tuple2Trials[Case, Case2]

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
