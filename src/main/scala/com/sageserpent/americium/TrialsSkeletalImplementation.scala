package com.sageserpent.americium
import cats.implicits.*
import com.sageserpent.americium.TrialsApis.scalaApi
import com.sageserpent.americium.generation.FiltrationResult
import com.sageserpent.americium.generation.GenerationOperation.Generation
import com.sageserpent.americium.tupleTrials.Tuple2Trials as ScalaTuple2Trials
import com.sageserpent.americium.{
  Trials as ScalaTrials,
  TrialsScaffolding as ScalaTrialsScaffolding
}

import scala.collection.immutable.{SortedMap, SortedSet}

trait TrialsSkeletalImplementation[Case] extends ScalaTrials[Case] {
  override def map[TransformedCase](
      transform: Case => TransformedCase
  ): TrialsImplementation[TransformedCase] =
    TrialsImplementation(generation map transform)

  override def filter(
      predicate: Case => Boolean
  ): TrialsImplementation[Case] = {
    flatMap(caze =>
      new TrialsImplementation(
        FiltrationResult(Some(caze).filter(predicate))
      )
    )
  }

  override def mapFilter[TransformedCase](
      filteringTransform: Case => Option[TransformedCase]
  ): TrialsImplementation[TransformedCase] =
    flatMap(caze =>
      new TrialsImplementation(
        FiltrationResult(filteringTransform(caze))
      )
    )

  override def flatMap[TransformedCase](
      step: Case => ScalaTrials[TransformedCase]
  ): TrialsImplementation[TransformedCase] = {
    val adaptedStep = (step andThen (_.generation))
      .asInstanceOf[Case => Generation[TransformedCase]]
    TrialsImplementation(generation flatMap adaptedStep)
  }

  override def and[Case2](
      secondTrials: ScalaTrials[Case2]
  ): ScalaTrialsScaffolding.Tuple2Trials[Case, Case2] =
    new ScalaTuple2Trials(this, secondTrials)

  override def or[Case2](
      alternativeTrials: ScalaTrials[Case2]
  ): TrialsImplementation[Either[Case, Case2]] = scalaApi.alternate(
    this.map(Either.left[Case, Case2]),
    alternativeTrials.map(Either.right)
  )

  override def several[Container](implicit
      factory: collection.Factory[Case, Container]
  ): TrialsSkeletalImplementation[Container]

  override def lists: TrialsSkeletalImplementation[List[Case]] = several

  override def sets: TrialsSkeletalImplementation[Set[_ <: Case]] = several

  override def sortedSets(implicit
      ordering: Ordering[_ >: Case]
  ): TrialsSkeletalImplementation[SortedSet[_ <: Case]] =
    lists.map(SortedSet.from[Case](_)(ordering.asInstanceOf[Ordering[Case]]))

  override def maps[Value](
      values: ScalaTrials[Value]
  ): TrialsSkeletalImplementation[Map[Case, Value]] =
    flatMap(key => values.map(key -> _)).several[Map[Case, Value]]

  override def sortedMaps[Value](values: ScalaTrials[Value])(implicit
      ordering: Ordering[_ >: Case]
  ): TrialsSkeletalImplementation[SortedMap[Case, Value]] = flatMap(key =>
    values.map(key -> _)
  ).lists
    .map(SortedMap.from[Case, Value](_)(ordering.asInstanceOf[Ordering[Case]]))

  override def lotsOfSize[Collection](size: Int)(implicit
      factory: collection.Factory[Case, Collection]
  ): TrialsSkeletalImplementation[Collection]

  override def listsOfSize(
      size: Int
  ): TrialsSkeletalImplementation[List[Case]] = lotsOfSize(
    size
  )
}
