package com.sageserpent.americium
import com.sageserpent.americium.TrialsImplementation.{
  FiltrationResult,
  Generation
}
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
      ): ScalaTrials[Case]
    )
  }

  override def mapFilter[TransformedCase](
      filteringTransform: Case => Option[TransformedCase]
  ): TrialsImplementation[TransformedCase] =
    flatMap(caze =>
      new TrialsImplementation(
        FiltrationResult(filteringTransform(caze))
      ): ScalaTrials[TransformedCase]
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

  override def lists: ScalaTrials[List[Case]] = several

  override def sets: ScalaTrials[Set[_ <: Case]] = several

  override def sortedSets(implicit
      ordering: Ordering[_ >: Case]
  ): ScalaTrials[SortedSet[_ <: Case]] =
    lists.map(SortedSet.from[Case](_)(ordering.asInstanceOf[Ordering[Case]]))

  override def maps[Value](
      values: ScalaTrials[Value]
  ): ScalaTrials[Map[Case, Value]] =
    flatMap(key => values.map(key -> _)).several[Map[Case, Value]]

  override def sortedMaps[Value](values: ScalaTrials[Value])(implicit
      ordering: Ordering[_ >: Case]
  ): ScalaTrials[SortedMap[Case, Value]] = flatMap(key =>
    values.map(key -> _)
  ).lists
    .map(SortedMap.from[Case, Value](_)(ordering.asInstanceOf[Ordering[Case]]))

  override def listsOfSize(size: Int): ScalaTrials[List[Case]] = lotsOfSize(
    size
  )
}
