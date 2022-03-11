package com.sageserpent.americium

import cats.*
import com.sageserpent.americium.TrialsImplementation.GenerationSupport
import com.sageserpent.americium.TrialsScaffolding.Tuple2Trials
import com.sageserpent.americium.java.Trials as JavaTrials

import scala.collection.Factory
import scala.collection.immutable.{SortedMap, SortedSet}
import scala.language.implicitConversions
import scala.util.DynamicVariable

object Trials {
  def api: TrialsApi = TrialsApis.scalaApi

  private[americium] val throwInlineFilterRejection
      : DynamicVariable[() => Unit] =
    new DynamicVariable(() => {})

  def whenever(satisfiedPrecondition: Boolean)(
      block: => Unit
  ): Unit =
    if (satisfiedPrecondition) block
    else throwInlineFilterRejection.value.apply()

  implicit class CharacterTrialsSyntax(val characterTrials: Trials[Char]) {
    def strings: Trials[String] = characterTrials.several

    def stringsOfSize(size: Int): Trials[String] =
      characterTrials.lotsOfSize(size)
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

trait Trials[+Case]
    extends TrialsScaffolding[Case]
    with GenerationSupport[Case] {
  override type SupplySyntaxType <: TrialsScaffolding.SupplyToSyntax[Case]

  def javaTrials: JavaTrials[_ <: Case]

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

  def and[Case2](secondTrials: Trials[Case2]): Tuple2Trials[Case, Case2]

  def or[Case2](alternativeTrials: Trials[Case2]): Trials[Either[Case, Case2]]

  def options: Trials[Option[Case]]

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
